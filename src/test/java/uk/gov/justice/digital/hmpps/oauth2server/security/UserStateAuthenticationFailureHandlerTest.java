package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.web.RedirectStrategy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserStateAuthenticationFailureHandlerTest {
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private RedirectStrategy redirectStrategy;

    private UserStateAuthenticationFailureHandler handler;

    @Before
    public void setUp() {
        setupHandler(false);
    }

    @Test
    public void onAuthenticationFailure_locked() throws IOException {
        handler.onAuthenticationFailure(request, response, new LockedException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/login?error&reason=locked");
    }

    @Test
    public void onAuthenticationFailure_expired() throws IOException {
        handler.onAuthenticationFailure(request, response, new AccountExpiredException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/login?error&reason=expired");
    }

    @Test
    public void onAuthenticationFailure_expiredResetEnabled() throws IOException {
        when(request.getParameter("username")).thenReturn("bob");
        setupHandler(true);
        handler.onAuthenticationFailure(request, response, new AccountExpiredException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/changePassword?username=BOB");
    }

    @Test
    public void onAuthenticationFailure_missing() throws IOException {
        handler.onAuthenticationFailure(request, response, new MissingCredentialsException());

        verify(redirectStrategy).sendRedirect(request, response, "/login?error&reason=missing");
    }

    @Test
    public void onAuthenticationFailure_other() throws IOException {
        handler.onAuthenticationFailure(request, response, new BadCredentialsException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/login?error");
    }

    private void setupHandler(final boolean expiredReset) {
        handler = new UserStateAuthenticationFailureHandler(expiredReset);
        handler.setRedirectStrategy(redirectStrategy);
    }
}
