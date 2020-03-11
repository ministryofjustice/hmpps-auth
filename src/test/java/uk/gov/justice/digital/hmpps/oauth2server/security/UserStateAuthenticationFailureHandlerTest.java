package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.microsoft.applicationinsights.TelemetryClient;
import kotlin.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider.MfaRequiredException;
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider.MfaUnavailableException;
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class UserStateAuthenticationFailureHandlerTest {
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
    @Mock
    private TelemetryClient telemetryClient;

    private UserStateAuthenticationFailureHandler handler;

    @BeforeEach
    void setUp() {
        handler = setupHandler(false);
    }

    @Test
    void onAuthenticationFailure_locked() throws IOException {
        handler.onAuthenticationFailure(request, response, new LockedException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/login?error=locked");
        verify(telemetryClient).trackEvent("AuthenticateFailure", Map.of("username", "missinguser", "type", "locked"), null);
    }

    @Test
    void onAuthenticationFailure_expired() throws IOException {
        when(request.getParameter("username")).thenReturn("bob");
        when(tokenService.createToken(any(), anyString())).thenReturn("sometoken");
        handler.onAuthenticationFailure(request, response, new CredentialsExpiredException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/change-password?token=sometoken");
        verify(tokenService).createToken(TokenType.CHANGE, "BOB");
        verify(telemetryClient).trackEvent("AuthenticateFailure", Map.of("username", "BOB", "type", "expired"), null);
    }

    @Test
    void onAuthenticationFailure_expiredTrimUppercase() throws IOException {
        when(request.getParameter("username")).thenReturn("   Joe  ");
        when(tokenService.createToken(any(), anyString())).thenReturn("sometoken");
        handler.onAuthenticationFailure(request, response, new CredentialsExpiredException("msg"));

        verify(tokenService).createToken(TokenType.CHANGE, "JOE");
        verify(telemetryClient).trackEvent("AuthenticateFailure", Map.of("username", "JOE", "type", "expired"), null);
    }

    @Test
    void onAuthenticationFailure_missingpass() throws IOException {
        when(request.getParameter("username")).thenReturn("bob");

        handler.onAuthenticationFailure(request, response, new MissingCredentialsException());

        verify(redirectStrategy).sendRedirect(request, response, "/login?error=missingpass");
        verify(telemetryClient).trackEvent("AuthenticateFailure", Map.of("username", "BOB", "type", "missingpass"), null);
    }

    @Test
    void onAuthenticationFailure_missinguser() throws IOException {
        when(request.getParameter("password")).thenReturn("bob");

        handler.onAuthenticationFailure(request, response, new MissingCredentialsException());

        verify(redirectStrategy).sendRedirect(request, response, "/login?error=missinguser");
        verify(telemetryClient).trackEvent("AuthenticateFailure", Map.of("username", "missinguser", "type", "missinguser"), null);
    }

    @Test
    void onAuthenticationFailure_missingboth() throws IOException {
        handler.onAuthenticationFailure(request, response, new MissingCredentialsException());

        verify(redirectStrategy).sendRedirect(request, response, "/login?error=missinguser&error=missingpass");
        verify(telemetryClient).trackEvent("AuthenticateFailure", Map.of("username", "missinguser", "type", "missinguser"), null);
    }

    @Test
    void onAuthenticationFailure_deliusDown() throws IOException {
        handler.onAuthenticationFailure(request, response, new DeliusAuthenticationServiceException());

        verify(redirectStrategy).sendRedirect(request, response, "/login?error=invalid&error=deliusdown");
        verify(telemetryClient).trackEvent("AuthenticateFailure", Map.of("username", "missinguser", "type", "invalid"), null);
    }

    @Test
    void onAuthenticationFailure_other() throws IOException {
        handler.onAuthenticationFailure(request, response, new BadCredentialsException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/login?error=invalid");
        verify(telemetryClient).trackEvent("AuthenticateFailure", Map.of("username", "missinguser", "type", "invalid"), null);
    }

    @Test
    void onAuthenticationFailure_mfa() throws IOException {
        when(request.getParameter("username")).thenReturn("bob");
        when(mfaService.createTokenAndSendMfaCode(anyString())).thenReturn(new Pair<>("sometoken", "somecode"));
        handler.onAuthenticationFailure(request, response, new MfaRequiredException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/mfa-challenge?token=sometoken");
        verify(mfaService).createTokenAndSendMfaCode("BOB");
    }

    @Test
    void onAuthenticationFailure_mfa_smokeTestEnabled() throws IOException {
        when(request.getParameter("username")).thenReturn("bob");
        when(mfaService.createTokenAndSendMfaCode(anyString())).thenReturn(new Pair<>("sometoken", "somecode"));
        setupHandler(true).onAuthenticationFailure(request, response, new MfaRequiredException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/mfa-challenge?token=sometoken&smokeCode=somecode");
        verify(mfaService).createTokenAndSendMfaCode("BOB");
    }

    @Test
    void onAuthenticationFailure_mfaUnavailable() throws IOException {
        handler.onAuthenticationFailure(request, response, new MfaUnavailableException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/login?error=mfaunavailable");
        verify(telemetryClient).trackEvent("AuthenticateFailure", Map.of("username", "missinguser", "type", "mfaunavailable"), null);
    }

    private UserStateAuthenticationFailureHandler setupHandler(final boolean smokeTestEnabled) {
        final var setupHandler = new UserStateAuthenticationFailureHandler(tokenService, mfaService, smokeTestEnabled, telemetryClient);
        setupHandler.setRedirectStrategy(redirectStrategy);
        return setupHandler;
    }
}
