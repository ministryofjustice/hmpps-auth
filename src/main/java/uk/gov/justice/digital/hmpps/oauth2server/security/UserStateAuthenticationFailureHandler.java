package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import uk.gov.justice.digital.hmpps.oauth2server.security.ApiAuthenticationProvider.MissingCredentialsException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

public class UserStateAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private static final String FAILURE_URL = "/login?error";

    public UserStateAuthenticationFailureHandler() {
        super(FAILURE_URL);
        setAllowSessionCreation(false);
    }

    @Override
    public void onAuthenticationFailure(final HttpServletRequest request, final HttpServletResponse response,
                                        final AuthenticationException exception) throws IOException {
        final Optional<String> reason;
        if (exception instanceof LockedException) {
            reason = Optional.of("locked");
        } else if (exception instanceof AccountExpiredException) {
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
