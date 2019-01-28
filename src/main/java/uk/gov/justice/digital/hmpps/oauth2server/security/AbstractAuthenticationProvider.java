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

import java.util.Map;

@Slf4j
public abstract class AbstractAuthenticationProvider extends DaoAuthenticationProvider {
    private final UserRetriesService userRetriesService;
    private final TelemetryClient telemetryClient;
    private final int accountLockoutCount;

    public AbstractAuthenticationProvider(final UserDetailsService userDetailsService,
                                          final UserRetriesService userRetriesService,
                                          final TelemetryClient telemetryClient,
                                          final int accountLockoutCount) {
        this.userRetriesService = userRetriesService;
        this.telemetryClient = telemetryClient;
        this.accountLockoutCount = accountLockoutCount;
        setUserDetailsService(userDetailsService);
    }

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        if (StringUtils.isBlank(authentication.getName()) || authentication.getCredentials() == null ||
                StringUtils.isBlank(authentication.getCredentials().toString())) {
            log.info("Credentials missing for user {}", authentication.getName());
            trackFailure(authentication.getName(), "credentials", "missing");
            throw new MissingCredentialsException();
        }

        try {
            return super.authenticate(authentication);
        } catch (final AuthenticationException e) {
            final var reason = e.getClass().getSimpleName();
            final var username = authentication.getName();
            log.info("Authenticate failed for user {} with reason {}", username, reason, e);
            trackFailure(username, reason);
            throw e;
        }
    }

    @Override
    protected void additionalAuthenticationChecks(final UserDetails userDetails, final UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        final var username = userDetails.getUsername();
        final var password = authentication.getCredentials().toString();

        final var userData = getUserData(username);

        if (userData == null) {
            log.info("User data missing for user {}", username);
            trackFailure(username, "credentials", "usermissing");
            throw new BadCredentialsException("Authentication failed: unable to check password value");
        }

        checkPasswordWithAccountLock(username, password, userData);

        log.info("Successful login for user {}", username);
        telemetryClient.trackEvent("AuthenticateSuccess", Map.of("username", username), null);
    }

    private void checkPasswordWithAccountLock(final String username, final String password, final UserData userData) {
        if (getPasswordEncoder().matches(password, userData.getPassword())) {
            log.info("Resetting retries for user {}", username);
            userRetriesService.resetRetries(username);

        } else {
            final var newRetryCount = userRetriesService.incrementRetries(username, userData.getRetryCount());

            // check the number of retries
            if (newRetryCount >= accountLockoutCount) {
                // Throw locked exception
                lockAccount(username);

                // need to reset the retry count otherwise when the user is then unlocked they will have to get the password right first time
                userRetriesService.lockAccount(username);

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

    protected abstract void lockAccount(final String username);

    @Data
    static class UserData {
        private String spare4;
        private int retryCount;

        String getPassword() {
            return "{oracle}" + spare4;
        }
    }
}
