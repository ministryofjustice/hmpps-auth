package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.web.RedirectStrategy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserStateAuthenticationFailureHandlerTest {
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ChangePasswordService changePasswordService;
    @Mock
    private RedirectStrategy redirectStrategy;

    private UserStateAuthenticationFailureHandler handler;

    @Before
    public void setUp() {
        setupHandler();
    }

    @Test
    public void onAuthenticationFailure_locked() throws IOException {
        handler.onAuthenticationFailure(request, response, new LockedException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/login?error=locked");
    }

    @Test
    public void onAuthenticationFailure_expired() throws IOException {
        when(request.getParameter("username")).thenReturn("bob");
        when(changePasswordService.createToken(anyString())).thenReturn("sometoken");
        handler.onAuthenticationFailure(request, response, new CredentialsExpiredException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/change-password?token=sometoken");
        verify(changePasswordService).createToken("BOB");
    }

    @Test
    public void onAuthenticationFailure_expiredTrimUppercase() throws IOException {
        when(request.getParameter("username")).thenReturn("   Joe  ");
        when(changePasswordService.createToken(anyString())).thenReturn("sometoken");
        handler.onAuthenticationFailure(request, response, new CredentialsExpiredException("msg"));

        verify(changePasswordService).createToken("JOE");
    }

    @Test
    public void onAuthenticationFailure_missingpass() throws IOException {
        when(request.getParameter("username")).thenReturn("bob");

        handler.onAuthenticationFailure(request, response, new MissingCredentialsException());

        verify(redirectStrategy).sendRedirect(request, response, "/login?error=missingpass");
    }

    @Test
    public void onAuthenticationFailure_missinguser() throws IOException {
        when(request.getParameter("password")).thenReturn("bob");

        handler.onAuthenticationFailure(request, response, new MissingCredentialsException());

        verify(redirectStrategy).sendRedirect(request, response, "/login?error=missinguser");
    }

    @Test
    public void onAuthenticationFailure_missingboth() throws IOException {
        handler.onAuthenticationFailure(request, response, new MissingCredentialsException());

        verify(redirectStrategy).sendRedirect(request, response, "/login?error=missinguser&error=missingpass");
    }

    @Test
    public void onAuthenticationFailure_other() throws IOException {
        handler.onAuthenticationFailure(request, response, new BadCredentialsException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/login?error=invalid");
    }

    private void setupHandler() {
        handler = new UserStateAuthenticationFailureHandler(changePasswordService);
        handler.setRedirectStrategy(redirectStrategy);
    }
}
