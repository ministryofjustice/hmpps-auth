package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

@Slf4j
public abstract class AbstractAuthenticationProvider extends DaoAuthenticationProvider {
    private final UserRetriesService userRetriesService;

    public AbstractAuthenticationProvider(final UserDetailsService userDetailsService, final UserRetriesService userRetriesService) {
        this.userRetriesService = userRetriesService;
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
            userRetriesService.resetRetries(username);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } else {
            final var newRetryCount = userRetriesService.incrementRetries(username, userData.getRetryCount());

            // check the number of retries
            if (newRetryCount > 2) {
                // Throw locked exception
                final var lockStatus = userData.getStatus().isGracePeriod() ? AccountStatus.EXPIRED_GRACE_LOCKED_TIMED : AccountStatus.LOCKED_TIMED;
                lockAccount(lockStatus, username);

                // need to reset the retry count otherwise when the user is then unlocked they will have to get the password right first time
                userRetriesService.resetRetries(username);

                throw new LockedException("Account Locked, number of retries exceeded");
            }
            throw new BadCredentialsException("Authentication failed: password does not match stored value");
        }
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
