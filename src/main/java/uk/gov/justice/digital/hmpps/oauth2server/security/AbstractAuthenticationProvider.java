package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.Assert;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus;

import java.util.Map;

@Slf4j
public abstract class AbstractAuthenticationProvider extends DaoAuthenticationProvider {
    private final UserRetriesService userRetriesService;
    private final TelemetryClient telemetryClient;

    public AbstractAuthenticationProvider(final UserDetailsService userDetailsService,
                                          final UserRetriesService userRetriesService,
                                          final TelemetryClient telemetryClient) {
        this.userRetriesService = userRetriesService;
        this.telemetryClient = telemetryClient;
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
            log.info("Credentials missing for user {}", username);
            trackFailure(username, "credentials", "missing");
            throw new MissingCredentialsException();
        }

        final var userData = getUserData(username);

        if (userData == null) {
            log.info("User data missing for user {}", username);
            trackFailure(username, "credentials", "usermissing");
            throw new BadCredentialsException("Authentication failed: unable to check password value");
        }
        if (userData.getStatus().isLocked()) {
            log.info("Locked account for user {}", username);
            trackFailure(username, "locked", "already");
            throw new LockedException("User account is locked");
        }

        checkPasswordWithAccountLock(username, password, userData);

        // need to create a new authentication token with username in uppercase
        final var token = new UsernamePasswordAuthenticationToken(username, password);
        // copy across details from old token too
        token.setDetails(authentication.getDetails());

        try {
            return super.authenticate(token);
        } catch (final AuthenticationException e) {
            final var reason = e.getClass().getSimpleName();
            log.info("Authenticate failed for user {} with reason {}", username, reason, e);
            trackFailure(username, reason);
            throw e;
        }
    }

    @Override
    protected void additionalAuthenticationChecks(final UserDetails userDetails, final UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        final var username = authentication.getName();
        if (authentication.getCredentials() == null) {
            log.info("No credentials for user {}", username);
            trackFailure(username, "credentials", "null");

            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials",
                    "Bad credentials"));
        }
        log.info("Successful login for user {}", username);
        telemetryClient.trackEvent("AuthenticateSuccess", Map.of("username", username), null);
    }

    private void checkPasswordWithAccountLock(final String username, final String password, final UserData userData) {
        final var encodedPassword = encode(password, userData.getSalt());

        if (encodedPassword.equals(userData.getHash())) {
            log.info("Resetting retries for user {}", username);
            userRetriesService.resetRetries(username);

        } else {
            final var newRetryCount = userRetriesService.incrementRetries(username, userData.getRetryCount());

            // check the number of retries
            if (newRetryCount > 2) {
                // Throw locked exception
                final var lockStatus = userData.getStatus().isGracePeriod() ? AccountStatus.EXPIRED_GRACE_LOCKED_TIMED : AccountStatus.LOCKED_TIMED;
                lockAccount(lockStatus, username);

                // need to reset the retry count otherwise when the user is then unlocked they will have to get the password right first time
                userRetriesService.resetRetries(username);

                log.info("Locking account for user {}", username);
                trackFailure(username, "locked", "exceeded");
                throw new LockedException("Account is locked, number of retries exceeded");
            }
            log.info("Credentials incorrect for user {}", username);
            trackFailure(username, "credentials", "incorrect");
            throw new BadCredentialsException("Authentication failed: password does not match stored value");
        }
    }

    private void trackFailure(final String username, final String type) {
        telemetryClient.trackEvent("AuthenticateFailure", Map.of("username", username, "type", type), null);
    }

    private void trackFailure(final String username, final String type, final String subType) {
        telemetryClient.trackEvent("AuthenticateFailure", Map.of("username", username, "type", type, "subType", subType), null);
    }

    protected abstract UserData getUserData(final String username);

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
