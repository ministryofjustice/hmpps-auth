package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus;

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
    private static final String GET_LCOUNT = "SELECT LCOUNT FROM SYS.user$ WHERE name = ?";
    private static final String HASH = "{ ? = call SYS.DBMS_CRYPTO.hash(UTL_RAW.cast_to_raw (?) || HEXTORAW (?), DBMS_CRYPTO.hash_sh1) }";
    private static final String UPDATE_LCOUNT = "UPDATE SYS.user$ SET LCOUNT = LCOUNT + 1 WHERE name = ?";
    private static final String RESET_LCOUNT = "UPDATE SYS.user$ SET LCOUNT = 0 WHERE name = ?";
    private static final String UPDATE_STATUS = "UPDATE SYS.user$ SET ASTATUS = ? WHERE name = ?";

    @Autowired
    public OracleAuthenticationProvider(DataSource dataSource, UserDetailsService userDetailsService) {
        jdbcTemplate = new JdbcTemplate(dataSource);
        setUserDetailsService(userDetailsService);
    }

    @Override
    protected void additionalAuthenticationChecks(final UserDetails userDetails, final UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        final var username = authentication.getName().toUpperCase();
        final var password = authentication.getCredentials().toString();

        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new MissingCredentialsException();
        }

        String userData = jdbcTemplate.queryForObject(GET_HASH_DETAIL, new Object[] { username }, String.class);

        if (userData != null) {
            String salt = StringUtils.substring(userData, 42, 62);
            String hash = StringUtils.substring(userData, 2, 42);

            String encodedPassword = encode(password, salt);

            if (encodedPassword.equals(hash)) {
                // reset the retry count
                jdbcTemplate.update(RESET_LCOUNT, username);  // reset retry count
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } else {
                // check the number of retries
                Long retryCount = jdbcTemplate.queryForObject(GET_LCOUNT, new Object[] { username }, Long.class);
                if (retryCount != null && retryCount >= 3) {
                    // Lock the account
                    jdbcTemplate.update(UPDATE_STATUS, AccountStatus.LOCKED.getCode(), username);
                    throw new LockedException("Account Locked, number of retries exceeded");
                }

                // update the retries
                jdbcTemplate.update(UPDATE_LCOUNT, username);

                throw new BadCredentialsException("Authentication failed: password does not match stored value");
            }
        } else {
            throw new BadCredentialsException("Authentication failed: unable to check password value");
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
