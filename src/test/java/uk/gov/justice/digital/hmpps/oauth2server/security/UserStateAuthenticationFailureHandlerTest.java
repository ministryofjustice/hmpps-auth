package uk.gov.justice.digital.hmpps.oauth2server.security;

import kotlin.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.web.RedirectStrategy;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider.MfaRequiredException;
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider.MfaUnavailableException;
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
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
    private TokenService tokenService;
    @Mock
    private MfaService mfaService;
    @Mock
    private RedirectStrategy redirectStrategy;

    private UserStateAuthenticationFailureHandler handler;

    @Before
    public void setUp() {
        handler = setupHandler(false);
    }

    @Test
    public void onAuthenticationFailure_locked() throws IOException {
        handler.onAuthenticationFailure(request, response, new LockedException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/login?error=locked");
    }

    @Test
    public void onAuthenticationFailure_expired() throws IOException {
        when(request.getParameter("username")).thenReturn("bob");
        when(tokenService.createToken(any(), anyString())).thenReturn("sometoken");
        handler.onAuthenticationFailure(request, response, new CredentialsExpiredException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/change-password?token=sometoken");
        verify(tokenService).createToken(TokenType.CHANGE, "BOB");
    }

    @Test
    public void onAuthenticationFailure_expiredTrimUppercase() throws IOException {
        when(request.getParameter("username")).thenReturn("   Joe  ");
        when(tokenService.createToken(any(), anyString())).thenReturn("sometoken");
        handler.onAuthenticationFailure(request, response, new CredentialsExpiredException("msg"));

        verify(tokenService).createToken(TokenType.CHANGE, "JOE");
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
    public void onAuthenticationFailure_deliusDown() throws IOException {
        handler.onAuthenticationFailure(request, response, new DeliusAuthenticationServiceException());

        verify(redirectStrategy).sendRedirect(request, response, "/login?error=invalid&error=deliusdown");
    }

    @Test
    public void onAuthenticationFailure_other() throws IOException {
        handler.onAuthenticationFailure(request, response, new BadCredentialsException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/login?error=invalid");
    }

    @Test
    public void onAuthenticationFailure_mfa() throws IOException {
        when(request.getParameter("username")).thenReturn("bob");
        when(mfaService.createTokenAndSendEmail(anyString())).thenReturn(new Pair<>("sometoken", "somecode"));
        handler.onAuthenticationFailure(request, response, new MfaRequiredException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/mfa-challenge?token=sometoken");
        verify(mfaService).createTokenAndSendEmail("BOB");
    }

    @Test
    public void onAuthenticationFailure_mfa_smokeTestEnabled() throws IOException {
        when(request.getParameter("username")).thenReturn("bob");
        when(mfaService.createTokenAndSendEmail(anyString())).thenReturn(new Pair<>("sometoken", "somecode"));
        setupHandler(true).onAuthenticationFailure(request, response, new MfaRequiredException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/mfa-challenge?token=sometoken&smokeCode=somecode");
        verify(mfaService).createTokenAndSendEmail("BOB");
    }

    @Test
    public void onAuthenticationFailure_mfaUnavailable() throws IOException {
        handler.onAuthenticationFailure(request, response, new MfaUnavailableException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/login?error=mfaunavailable");
    }

    private UserStateAuthenticationFailureHandler setupHandler(final boolean smokeTestEnabled) {
        final var setupHandler = new UserStateAuthenticationFailureHandler(tokenService, mfaService, smokeTestEnabled);
        setupHandler.setRedirectStrategy(redirectStrategy);
        return setupHandler;
    }
}
