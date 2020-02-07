package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.justice.digital.hmpps.oauth2server.config.CookieRequestCache;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class JwtAuthenticationSuccessHandlerTest {
    @Mock
    private JwtCookieHelper jwtCookieHelper;
    @Mock
    private JwtAuthenticationHelper jwtAuthenticationHelper;
    @Mock
    private CookieRequestCache cookieRequestCache;
    @Mock
    private VerifyEmailService verifyEmailService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private RedirectStrategy redirectStrategy;

    private JwtAuthenticationSuccessHandler handler;

    @Test
    void onAuthenticationSuccess_verifyEnabledAlreadyVerified() throws IOException, ServletException {
        setupHandler();

        when(verifyEmailService.isNotVerified(anyString())).thenReturn(Boolean.FALSE);
        handler.onAuthenticationSuccess(request, response, new UsernamePasswordAuthenticationToken("user", "pass"));

        verify(redirectStrategy).sendRedirect(request, response, "/");
    }

    @Test
    void onAuthenticationSuccess_verifyEnabledNotVerified() throws IOException, ServletException {
        setupHandler();

        when(verifyEmailService.isNotVerified(anyString())).thenReturn(Boolean.TRUE);
        handler.onAuthenticationSuccess(request, response, new UsernamePasswordAuthenticationToken("user", "pass"));

        verify(redirectStrategy).sendRedirect(request, response, "/verify-email");
    }

    private void setupHandler() {
        handler = new JwtAuthenticationSuccessHandler(jwtCookieHelper, jwtAuthenticationHelper, cookieRequestCache,
                verifyEmailService);
        handler.setRedirectStrategy(redirectStrategy);
    }
}
