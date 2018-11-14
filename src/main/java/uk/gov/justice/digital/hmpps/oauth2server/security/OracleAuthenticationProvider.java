package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@Profile("oracle-auth")
public class OracleAuthenticationProvider extends DaoAuthenticationProvider {

    private final JdbcTemplate jdbcTemplate;
    private static final String GET_HASH_DETAIL = "SELECT spare4 FROM SYS.user$ WHERE name = ?";
    private static final String HASH = "{ ? = call SYS.DBMS_CRYPTO.hash(UTL_RAW.cast_to_raw (?) || HEXTORAW (?), DBMS_CRYPTO.hash_sh1) }";

    @Autowired
    public OracleAuthenticationProvider(DataSource dataSource, UserDetailsService userDetailsService) {
        jdbcTemplate = new JdbcTemplate(dataSource);
        setUserDetailsService(userDetailsService);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void additionalAuthenticationChecks(UserDetails userDetails,
                                                  UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {
        if (authentication.getCredentials() == null) {
            logger.debug("Authentication failed: no credentials provided");

            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials",
                    "Bad credentials"));
        }

        String userData = jdbcTemplate.queryForObject(
                GET_HASH_DETAIL, new Object[] { userDetails.getUsername() }, String.class);
        String salt =  StringUtils.substring(userData, 42, 62);
        String hash = StringUtils.substring(userData, 2, 42);

        String encodedPassword = encode(authentication.getCredentials().toString(), salt);

        if (!encodedPassword.equals(hash)) {
            logger.debug("Authentication failed: password does not match stored value");

            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials",
                    "Bad credentials"));
        }
    }


    private String encode(String rawPassword, String salt) {
        List<SqlParameter> params = new ArrayList<>();
        params.add(new SqlOutParameter("encodedPassword", Types.VARCHAR));
        params.add(new SqlParameter(Types.VARCHAR, salt));
        params.add(new SqlParameter(Types.VARCHAR, rawPassword));

        Map<String, Object> results = jdbcTemplate.call(
                con -> {
                    CallableStatement cs = con.prepareCall(HASH);
                    cs.registerOutParameter(1, Types.VARCHAR);
                    cs.setString(2, rawPassword);
                    cs.setString(3, salt);
                    return cs;
                },
                params);

        return (String)results.get("encodedPassword");
    }

}
