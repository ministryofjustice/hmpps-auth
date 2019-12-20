package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.StringJoiner;

@Component
public class UserStateAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private static final String FAILURE_URL = "/login";
    private final ChangePasswordService changePasswordService;


    public UserStateAuthenticationFailureHandler(final ChangePasswordService changePasswordService) {
        super(FAILURE_URL);
        this.changePasswordService = changePasswordService;
        setAllowSessionCreation(false);
    }

    @Override
    public void onAuthenticationFailure(final HttpServletRequest request, final HttpServletResponse response,
                                        final AuthenticationException exception) throws IOException {
        final var builder = new StringJoiner("&error=", "?error=", "");
        if (exception instanceof LockedException) {
            builder.add("locked");
        } else if (exception instanceof CredentialsExpiredException) {
            // special handling for expired users and feature switch turned on
            final var username = StringUtils.trim(request.getParameter("username").toUpperCase());
            final var token = changePasswordService.createToken(username);
            getRedirectStrategy().sendRedirect(request, response, "/change-password?token=" + token);
            return;
        } else if (exception instanceof MissingCredentialsException) {
            if (StringUtils.isBlank(request.getParameter("username"))) {
                builder.add("missinguser");
            }
            if (StringUtils.isBlank(request.getParameter("password"))) {
                builder.add("missingpass");
            }
        } else if (exception instanceof DeliusAuthenticationServiceException) {
            builder.add("invalid").add("deliusdown");
        } else {
            builder.add("invalid");
        }

        final var redirectUrl = FAILURE_URL + builder.toString();
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
