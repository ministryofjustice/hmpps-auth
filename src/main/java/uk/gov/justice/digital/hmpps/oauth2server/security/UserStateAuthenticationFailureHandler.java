package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component
public class UserStateAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private static final String FAILURE_URL = "/login?error";

    private final boolean expiredReset;

    public UserStateAuthenticationFailureHandler(
            @Value("${application.expired-reset.enabled}") final boolean expiredReset) {
        super(FAILURE_URL);
        this.expiredReset = expiredReset;
        setAllowSessionCreation(false);
    }

    @Override
    public void onAuthenticationFailure(final HttpServletRequest request, final HttpServletResponse response,
                                        final AuthenticationException exception) throws IOException {
        final Optional<String> reason;
        if (exception instanceof LockedException) {
            reason = Optional.of("locked");
        } else if (exception instanceof AccountExpiredException) {
            // special handling for expired users and feature switch turned on
            if (expiredReset) {
                final var username = request.getParameter("username").toUpperCase();
                getRedirectStrategy().sendRedirect(request, response, "/changePassword?username=" + username);
                return;
            }
            reason = Optional.of("expired");
        } else if (exception instanceof MissingCredentialsException) {
            reason = Optional.of("missing");
        } else {
            reason = Optional.empty();
        }

        final String redirectUrl = FAILURE_URL + reason.map(r -> "&reason=" + r).orElse("");

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
