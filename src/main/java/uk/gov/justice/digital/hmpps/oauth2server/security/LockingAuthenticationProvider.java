package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService;

import java.util.Map;

@Slf4j
public abstract class LockingAuthenticationProvider extends DaoAuthenticationProvider {
    private final UserRetriesService userRetriesService;
    private final MfaService mfaService;
    private final UserService userService;
    private final TelemetryClient telemetryClient;

    public LockingAuthenticationProvider(final UserDetailsService userDetailsService,
                                         final UserRetriesService userRetriesService,
                                         final MfaService mfaService,
                                         final UserService userService,
                                         final TelemetryClient telemetryClient) {
        this.userRetriesService = userRetriesService;
        this.mfaService = mfaService;
        this.userService = userService;
        this.telemetryClient = telemetryClient;
        setUserDetailsService(userDetailsService);

        final var oracleSha1PasswordEncoder = new OracleSha1PasswordEncoder();
        final var encoders = Map.of("bcrypt", new BCryptPasswordEncoder(), "oracle", oracleSha1PasswordEncoder);
        final var delegatingPasswordEncoder = new DelegatingPasswordEncoder("bcrypt", encoders);
        delegatingPasswordEncoder.setDefaultPasswordEncoderForMatches(oracleSha1PasswordEncoder);
        setPasswordEncoder(delegatingPasswordEncoder);
    }

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        if (StringUtils.isBlank(authentication.getName()) || authentication.getCredentials() == null ||
                StringUtils.isBlank(authentication.getCredentials().toString())) {
            log.info("Credentials missing for user {}", authentication.getName());
            throw new MissingCredentialsException();
        }

        try {
            return super.authenticate(authentication);
        } catch (final AuthenticationException e) {
            final var reason = e.getClass().getSimpleName();
            final var username = authentication.getName();
            log.info("Authenticate failed for user {} with reason {} and message {}", username, reason, e.getMessage());
            throw e;
        }
    }

    @Override
    protected void additionalAuthenticationChecks(final UserDetails userDetails, final UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        final var username = userDetails.getUsername();
        final var password = authentication.getCredentials().toString();

        checkPasswordWithAccountLock((UserPersonDetails) userDetails, password);

        log.info("Successful login for user {}", username);
        telemetryClient.trackEvent("AuthenticateSuccess", Map.of("username", username), null);
    }

    private void checkPasswordWithAccountLock(final UserPersonDetails userDetails, final String password) {
        final var username = userDetails.getUsername();
        if (checkPassword(userDetails, password)) {
            log.info("Resetting retries for user {}", username);
            userRetriesService.resetRetriesAndRecordLogin(userDetails);

            // now check if mfa is enabled for the user
            if (mfaService.needsMfa(userDetails.getAuthorities())) {
                if (userService.hasVerifiedEmail(userDetails)) {
                    throw new MfaRequiredException("MFA required");
                }
                throw new MfaUnavailableException("MFA required, but no email set");
            }
        } else {
            final var locked = userRetriesService.incrementRetriesAndLockAccountIfNecessary(userDetails);

            // check the number of retries
            if (locked) {
                log.info("Locked account for user {}", username);
                throw new LockedException("Account is locked, number of retries exceeded");
            }
            log.info("Credentials incorrect for user {}", username);
            throw new BadCredentialsException("Authentication failed: password does not match stored value");
        }
    }

    protected boolean checkPassword(final UserDetails userDetails, final String password) {
        return getPasswordEncoder().matches(password, userDetails.getPassword());
    }

    public static class MfaRequiredException extends AccountStatusException {
        MfaRequiredException(final String msg) {
            super(msg);
        }
    }

    public static class MfaUnavailableException extends AccountStatusException {
        MfaUnavailableException(final String msg) {
            super(msg);
        }
    }
}
