package uk.gov.justice.digital.hmpps.oauth2server.security

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.web.RedirectStrategy
import uk.gov.justice.digital.hmpps.oauth2server.config.CookieRequestCache
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JwtAuthenticationSuccessHandlerTest {
  private val jwtCookieHelper: JwtCookieHelper = mock()
  private val jwtAuthenticationHelper: JwtAuthenticationHelper = mock()
  private val cookieRequestCache: CookieRequestCache = mock()
  private val verifyEmailService: VerifyEmailService = mock()
  private val request: HttpServletRequest = mock()
  private val response: HttpServletResponse = mock()
  private val redirectStrategy: RedirectStrategy = mock()
  private val handler = JwtAuthenticationSuccessHandler(jwtCookieHelper, jwtAuthenticationHelper, cookieRequestCache,
      verifyEmailService)

  @Test
  fun onAuthenticationSuccess_verifyEnabledAlreadyVerified() {
    setupHandler()
    whenever(verifyEmailService.isNotVerified(anyString())).thenReturn(false)
    handler.onAuthenticationSuccess(request, response, UsernamePasswordAuthenticationToken("user", "pass"))
    verify(redirectStrategy).sendRedirect(request, response, "/")
  }

  @Test
  fun onAuthenticationSuccess_verifyEnabledNotVerified() {
    setupHandler()
    whenever(verifyEmailService.isNotVerified(anyString())).thenReturn(true)
    handler.onAuthenticationSuccess(request, response, UsernamePasswordAuthenticationToken("user", "pass"))
    verify(redirectStrategy).sendRedirect(request, response, "/verify-email")
  }

  private fun setupHandler() {
    handler.setRedirectStrategy(redirectStrategy)
  }
}
