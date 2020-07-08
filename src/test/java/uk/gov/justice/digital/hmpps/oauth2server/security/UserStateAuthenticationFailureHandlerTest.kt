package uk.gov.justice.digital.hmpps.oauth2server.security

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.LockedException
import org.springframework.security.web.RedirectStrategy
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.MfaPreferenceType.TEXT
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.CHANGE
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider.MfaRequiredException
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider.MfaUnavailableException
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaData
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class UserStateAuthenticationFailureHandlerTest {
  private val request: HttpServletRequest = mock()
  private val response: HttpServletResponse = mock()
  private val tokenService: TokenService = mock()
  private val mfaService: MfaService = mock()
  private val redirectStrategy: RedirectStrategy = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val handler = setupHandler(false)

  @Test
  fun onAuthenticationFailure_locked() {
    handler.onAuthenticationFailure(request, response, LockedException("msg"))
    verify(redirectStrategy).sendRedirect(request, response, "/login?error=locked")
    verify(telemetryClient).trackEvent("AuthenticateFailure", mapOf("username" to "missinguser", "type" to "locked"), null)
  }

  @Test
  fun onAuthenticationFailure_expired() {
    whenever(request.getParameter("username")).thenReturn("bob")
    whenever(tokenService.createToken(any(), anyString())).thenReturn("sometoken")
    handler.onAuthenticationFailure(request, response, CredentialsExpiredException("msg"))
    verify(redirectStrategy).sendRedirect(request, response, "/change-password?token=sometoken")
    verify(tokenService).createToken(CHANGE, "BOB")
    verify(telemetryClient).trackEvent("AuthenticateFailure", mapOf("username" to "BOB", "type" to "expired"), null)
  }

  @Test
  fun onAuthenticationFailure_expiredTrimUppercase() {
    whenever(request.getParameter("username")).thenReturn("   Joe  ")
    whenever(tokenService.createToken(any(), anyString())).thenReturn("sometoken")
    handler.onAuthenticationFailure(request, response, CredentialsExpiredException("msg"))
    verify(tokenService).createToken(CHANGE, "JOE")
    verify(telemetryClient).trackEvent("AuthenticateFailure", mapOf("username" to "JOE", "type" to "expired"), null)
  }

  @Test
  fun onAuthenticationFailure_missingpass() {
    whenever(request.getParameter("username")).thenReturn("bob")
    handler.onAuthenticationFailure(request, response, MissingCredentialsException())
    verify(redirectStrategy).sendRedirect(request, response, "/login?error=missingpass")
    verify(telemetryClient).trackEvent("AuthenticateFailure", mapOf("username" to "BOB", "type" to "missingpass"), null)
  }

  @Test
  fun onAuthenticationFailure_missinguser() {
    whenever(request.getParameter("password")).thenReturn("bob")
    handler.onAuthenticationFailure(request, response, MissingCredentialsException())
    verify(redirectStrategy).sendRedirect(request, response, "/login?error=missinguser")
    verify(telemetryClient).trackEvent("AuthenticateFailure", mapOf("username" to "missinguser", "type" to "missinguser"), null)
  }

  @Test
  fun onAuthenticationFailure_missingboth() {
    handler.onAuthenticationFailure(request, response, MissingCredentialsException())
    verify(redirectStrategy).sendRedirect(request, response, "/login?error=missinguser&error=missingpass")
    verify(telemetryClient).trackEvent("AuthenticateFailure", mapOf("username" to "missinguser", "type" to "missinguser"), null)
  }

  @Test
  fun onAuthenticationFailure_deliusDown() {
    handler.onAuthenticationFailure(request, response, DeliusAuthenticationServiceException())
    verify(redirectStrategy).sendRedirect(request, response, "/login?error=invalid&error=deliusdown")
    verify(telemetryClient).trackEvent("AuthenticateFailure", mapOf("username" to "missinguser", "type" to "invalid"), null)
  }

  @Test
  fun onAuthenticationFailure_other() {
    handler.onAuthenticationFailure(request, response, BadCredentialsException("msg"))
    verify(redirectStrategy).sendRedirect(request, response, "/login?error=invalid")
    verify(telemetryClient).trackEvent("AuthenticateFailure", mapOf("username" to "missinguser", "type" to "invalid"), null)
  }

  @Test
  fun onAuthenticationFailure_mfa() {
    whenever(request.getParameter("username")).thenReturn("bob")
    whenever(mfaService.createTokenAndSendMfaCode(anyString())).thenReturn(MfaData("sometoken", "somecode", TEXT))
    handler.onAuthenticationFailure(request, response, MfaRequiredException("msg"))
    verify(redirectStrategy).sendRedirect(request, response, "/mfa-challenge?token=sometoken&mfaPreference=TEXT")
    verify(mfaService).createTokenAndSendMfaCode("BOB")
  }

  @Test
  fun onAuthenticationFailure_mfaRequiredButUnavailable() {
    whenever(request.getParameter("username")).thenReturn("bob")
    whenever(mfaService.createTokenAndSendMfaCode(anyString())).thenThrow(MfaUnavailableException("msg"))
    handler.onAuthenticationFailure(request, response, MfaRequiredException("msg"))
    verify(redirectStrategy).sendRedirect(request, response, "/login?error=mfaunavailable")
    verify(telemetryClient).trackEvent("AuthenticateFailure", mapOf("username" to "BOB", "type" to "mfaunavailable"), null)
  }

  @Test
  fun onAuthenticationFailure_mfa_smokeTestEnabled() {
    whenever(request.getParameter("username")).thenReturn("bob")
    whenever(mfaService.createTokenAndSendMfaCode(anyString())).thenReturn(MfaData("sometoken", "somecode", TEXT))
    setupHandler(true).onAuthenticationFailure(request, response, MfaRequiredException("msg"))
    verify(redirectStrategy).sendRedirect(request, response, "/mfa-challenge?token=sometoken&mfaPreference=TEXT&smokeCode=somecode")
    verify(mfaService).createTokenAndSendMfaCode("BOB")
  }

  @Test
  fun onAuthenticationFailure_mfaUnavailable() {
    handler.onAuthenticationFailure(request, response, MfaUnavailableException("msg"))
    verify(redirectStrategy).sendRedirect(request, response, "/login?error=mfaunavailable")
    verify(telemetryClient).trackEvent("AuthenticateFailure", mapOf("username" to "missinguser", "type" to "mfaunavailable"), null)
  }

  private fun setupHandler(smokeTestEnabled: Boolean): UserStateAuthenticationFailureHandler {
    val setupHandler = UserStateAuthenticationFailureHandler(tokenService, mfaService, smokeTestEnabled, telemetryClient)
    setupHandler.setRedirectStrategy(redirectStrategy)
    return setupHandler
  }
}
