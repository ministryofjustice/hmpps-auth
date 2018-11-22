package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AccountExpiredException;
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

import java.sql.DriverManager;
import java.sql.SQLException;

@Component
@Slf4j
@Profile("!oracle-auth")
public class ApiAuthenticationProvider extends DaoAuthenticationProvider {

    private final String jdbcUrl;

    @Autowired
    public ApiAuthenticationProvider(final UserDetailsService userDetailsService, @Value("${spring.datasource.url}") final String jdbcUrl) {
        setUserDetailsService(userDetailsService);
        this.jdbcUrl = jdbcUrl;
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

        try (final var ignored = DriverManager.getConnection(jdbcUrl, username, password)) {
            logger.debug(String.format("Verified database connection for user: %s", username));
            // Username and credentials are now validated. Must set authentication in security context now
            // so that subsequent user details queries will work.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (final SQLException ex) {
            log.info("Caught {} with message {}", ex.getClass().getName(), ex.getMessage());
            switch (ex.getErrorCode()) {
                case 28001:
                    throw new AccountExpiredException(ex.getMessage(), ex);
                case 28000:
                    throw new LockedException(ex.getMessage(), ex);
                default:
                    throw new BadCredentialsException(ex.getMessage(), ex);
            }
        }

        // need to create a new authentication token with username in uppercase
        final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
        // copy across details from old token too
        token.setDetails(authentication.getDetails());

        return super.authenticate(token);
    }

    @Override
    protected void additionalAuthenticationChecks(final UserDetails userDetails, final UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        if (authentication.getCredentials() == null) {
            logger.debug("Authentication failed: no credentials provided");

            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials",
                    "Bad credentials"));
        }
    }

}
