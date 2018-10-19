package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    private final JwtCookieHelper jwtCookieHelper;
    private final JwtAuthenticationHelper jwtAuthenticationHelper;

    @Autowired
    public JwtAuthenticationSuccessHandler(final JwtCookieHelper jwtCookieHelper,
                                           final JwtAuthenticationHelper jwtAuthenticationHelper,
                                           final CookieRequestCache cookieRequestCache) {
        this.jwtCookieHelper = jwtCookieHelper;
        this.jwtAuthenticationHelper = jwtAuthenticationHelper;
        setRequestCache(cookieRequestCache);
    }

    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response,
                                        final Authentication authentication) throws IOException, ServletException {

        final String jwt = jwtAuthenticationHelper.createJwt(authentication);
        jwtCookieHelper.addCookieToResponse(request, response, jwt);

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
