package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@Profile("oracle-auth")
public class OracleAuthenticationProvider extends DaoAuthenticationProvider {

    private final JdbcTemplate jdbcTemplate;
    private static final String GET_USER_DETAIL = "SELECT spare4, LCOUNT retry_count, ASTATUS status_code FROM SYS.user$ WHERE name = ?";
    private static final String HASH = "{ ? = call SYS.DBMS_CRYPTO.hash(UTL_RAW.cast_to_raw (?) || HEXTORAW (?), DBMS_CRYPTO.hash_sh1) }";
    private static final String UPDATE_LCOUNT = "UPDATE SYS.user$ SET LCOUNT = ? WHERE name = ?";
    private static final String UPDATE_STATUS = "UPDATE SYS.user$ SET ASTATUS = ?, LTIME = ? WHERE name = ?";

    @Autowired
    public OracleAuthenticationProvider(final DataSource dataSource, final UserDetailsService userDetailsService) {
        jdbcTemplate = new JdbcTemplate(dataSource);
        setUserDetailsService(userDetailsService);
    }

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication,
                () -> messages.getMessage(
                        "AbstractUserDetailsAuthenticationProvider.onlySupports",
                        "Only UsernamePasswordAuthenticationToken is supported"));

        final var username = authentication.getName().toUpperCase();
        final var password = authentication.getCredentials().toString();

        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new MissingCredentialsException();
        }

        // need to create a new authentication token with username in uppercase
        final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
        // copy across details from old token too
        token.setDetails(authentication.getDetails());

        return super.authenticate(token);
    }

    @Override
    protected void additionalAuthenticationChecks(final UserDetails userDetails, final UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        final var username = authentication.getName().toUpperCase();
        final var password = authentication.getCredentials().toString();

        final UserData userData = jdbcTemplate.queryForObject(GET_USER_DETAIL, new Object[] { username }, new BeanPropertyRowMapper<>(UserData.class));

        if (userData != null) {
            final String encodedPassword = encode(password, userData.getSalt());

            if (encodedPassword.equals(userData.getHash())) {
                // reset the retry count
                jdbcTemplate.update(UPDATE_LCOUNT, 0, username);  // reset retry count
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } else {
                jdbcTemplate.update(UPDATE_LCOUNT, userData.getRetryCount() + 1, username);

                // check the number of retries
                if (userData.getRetryCount()+1 > 2) {
                    // Throw locked exception
                    final AccountStatus lockStatus = userData.getStatus().isGracePeriod() ? AccountStatus.EXPIRED_GRACE_LOCKED_TIMED : AccountStatus.LOCKED_TIMED;
                    jdbcTemplate.update(UPDATE_STATUS, lockStatus.getCode(), Timestamp.valueOf(LocalDateTime.now()), username);
                    throw new LockedException("Account Locked, number of retries exceeded");
                }
                throw new BadCredentialsException("Authentication failed: password does not match stored value");
            }
        } else {
            throw new BadCredentialsException("Authentication failed: unable to check password value");
        }
    }

    private String encode(final String rawPassword, final String salt) {
        final List<SqlParameter> params = new ArrayList<>();
        params.add(new SqlOutParameter("encodedPassword", Types.VARCHAR));
        params.add(new SqlParameter(Types.VARCHAR, salt));
        params.add(new SqlParameter(Types.VARCHAR, rawPassword));

        final Map<String, Object> results = jdbcTemplate.call(
                con -> {
                    final CallableStatement cs = con.prepareCall(HASH);
                    cs.registerOutParameter(1, Types.VARCHAR);
                    cs.setString(2, rawPassword);
                    cs.setString(3, salt);
                    return cs;
                },
                params);

        return (String)results.get("encodedPassword");
    }

    @Data
    private static class UserData {
        private String spare4;
        private int retryCount;
        private int statusCode;

        AccountStatus getStatus() {
            return AccountStatus.get(statusCode);
        }

        String getSalt() {
            return StringUtils.substring(spare4, 42, 62);
        }

        String getHash() {
            return StringUtils.substring(spare4, 2, 42);
        }

    }
}
