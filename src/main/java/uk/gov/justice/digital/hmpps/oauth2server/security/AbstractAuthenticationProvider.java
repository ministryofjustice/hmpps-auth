package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.Assert;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus;

import javax.sql.DataSource;

@Slf4j
public abstract class AbstractAuthenticationProvider extends DaoAuthenticationProvider {
    private static final String SELECT_RETRY_COUNT = "SELECT retry_count FROM user_retries WHERE username = ?";
    private static final String UPDATE_RETRY_COUNT = "UPDATE user_retries SET retry_count = ? WHERE username = ?";
    private static final String INSERT_RETRY_COUNT = "INSERT INTO user_retries (retry_count, username) VALUES(?,?)";

    private final JdbcTemplate authJdbcTemplate;

    public AbstractAuthenticationProvider(final UserDetailsService userDetailsService, final DataSource authDataSource) {
        setUserDetailsService(userDetailsService);
        authJdbcTemplate = new JdbcTemplate(authDataSource);
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
        final var token = new UsernamePasswordAuthenticationToken(username, password);
        // copy across details from old token too
        token.setDetails(authentication.getDetails());

        return super.authenticate(token);
    }

    @Override
    protected void additionalAuthenticationChecks(final UserDetails userDetails, final UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        final var username = authentication.getName().toUpperCase();
        final var password = authentication.getCredentials().toString();

        final var userData = getUserData(username);

        if (userData == null) {
            throw new BadCredentialsException("Authentication failed: unable to check password value");
        }

        final var encodedPassword = encode(password, userData.getSalt());

        if (encodedPassword.equals(userData.getHash())) {
            resetRetryCount(username);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } else {
            final var newRetryCount = incrementRetryCount(username, userData.getRetryCount());

            // check the number of retries
            if (newRetryCount > 2) {
                // Throw locked exception
                final var lockStatus = userData.getStatus().isGracePeriod() ? AccountStatus.EXPIRED_GRACE_LOCKED_TIMED : AccountStatus.LOCKED_TIMED;
                lockAccount(lockStatus, username);

                // need to reset the retry count otherwise when the user is then unlocked they will have to get the password right first time
                resetRetryCount(username);

                throw new LockedException("Account Locked, number of retries exceeded");
            }
            throw new BadCredentialsException("Authentication failed: password does not match stored value");
        }
    }

    protected abstract UserData getUserData(final String username);

    private void resetRetryCount(final String username) {
        final var results = authJdbcTemplate.queryForList(SELECT_RETRY_COUNT, new Object[]{username}, Integer.class);
        if (results.isEmpty()) {
            // no row exists, so insert from other table
            authJdbcTemplate.update(INSERT_RETRY_COUNT, 0, username);
        } else {
            // we have a row, so reset the value
            authJdbcTemplate.update(UPDATE_RETRY_COUNT, 0, username);
        }
    }

    private int incrementRetryCount(final String username, final int retryCount) {
        final var results = authJdbcTemplate.queryForList(SELECT_RETRY_COUNT, new Object[]{username}, Integer.class);
        final int newCount;
        if (results.isEmpty()) {
            // no row exists, so insert from other table
            newCount = retryCount + 1;
            authJdbcTemplate.update(INSERT_RETRY_COUNT, newCount, username);
        } else {
            newCount = DataAccessUtils.intResult(results) + 1;
            // we have a row, so increment that value instead and ignore value in oracle
            authJdbcTemplate.update(UPDATE_RETRY_COUNT, newCount, username);
        }
        return newCount;
    }

    protected abstract void lockAccount(final AccountStatus status, final String username);

    protected abstract String encode(final String rawPassword, final String salt);

    @Data
    static class UserData {
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
