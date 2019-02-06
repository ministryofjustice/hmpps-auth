package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import uk.gov.justice.digital.hmpps.oauth2server.config.CookieRequestCache;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    private final JwtCookieHelper jwtCookieHelper;
    private final JwtAuthenticationHelper jwtAuthenticationHelper;
    private final VerifyEmailService verifyEmailService;

    @Autowired
    public JwtAuthenticationSuccessHandler(final JwtCookieHelper jwtCookieHelper,
                                           final JwtAuthenticationHelper jwtAuthenticationHelper,
                                           final CookieRequestCache cookieRequestCache,
                                           final VerifyEmailService verifyEmailService) {
        this.jwtCookieHelper = jwtCookieHelper;
        this.jwtAuthenticationHelper = jwtAuthenticationHelper;
        this.verifyEmailService = verifyEmailService;
        setRequestCache(cookieRequestCache);
        setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response,
                                        final Authentication authentication) throws IOException, ServletException {

        final var jwt = jwtAuthenticationHelper.createJwt(authentication);
        jwtCookieHelper.addCookieToResponse(request, response, jwt);

        // we have successfully authenticated and added the cookie.  Now need to check that they have a validated email address
        if (verifyEmailService.isNotVerified(authentication.getName())) {
            getRedirectStrategy().sendRedirect(request, response, "/verify-email");
            return;
        }

        proceed(request, response, authentication);
    }

    public void proceed(final HttpServletRequest request, final HttpServletResponse response,
                        final Authentication authentication) throws ServletException, IOException {
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
