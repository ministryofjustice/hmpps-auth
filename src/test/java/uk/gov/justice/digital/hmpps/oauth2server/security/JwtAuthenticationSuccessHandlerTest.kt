package uk.gov.justice.digital.hmpps.oauth2server.security

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.web.RedirectStrategy
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.config.CookieRequestCache
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.auth
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import java.util.Optional
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
  private val restTemplate: RestTemplate = mock()
  private val handler = JwtAuthenticationSuccessHandler(
    jwtCookieHelper,
    jwtAuthenticationHelper,
    cookieRequestCache,
    verifyEmailService,
    restTemplate,
    true
  )
  private val handlerTokenVerificationDisabled = JwtAuthenticationSuccessHandler(
    jwtCookieHelper,
    jwtAuthenticationHelper,
    cookieRequestCache,
    verifyEmailService,
    restTemplate,
    false
  )
  private val user = UserDetailsImpl("user", "name", setOf(), auth.name, "userid", "jwtId")

  @Test
  fun onAuthenticationSuccess_verifyEnabledAlreadyVerified() {
    whenever(verifyEmailService.isNotVerified(anyString(), eq(AuthSource.fromNullableString("nomis")))).thenReturn(false)
    handler.onAuthenticationSuccess(request, response, UsernamePasswordAuthenticationToken(user, "pass"))
    verify(redirectStrategy).sendRedirect(request, response, "/")
  }

  @Test
  fun onAuthenticationSuccess_verifyEnabledNotVerified() {
    whenever(verifyEmailService.isNotVerified(anyString(), any())).thenReturn(true)
    handler.onAuthenticationSuccess(request, response, UsernamePasswordAuthenticationToken(user, "pass"))
    verify(redirectStrategy).sendRedirect(request, response, "/verify-email")
  }

  @Test
  fun `onAuthenticationSuccess existing cookie`() {
    whenever(jwtCookieHelper.readValueFromCookie(any())).thenReturn(Optional.of("cookie_value"))
    whenever(jwtAuthenticationHelper.readUserDetailsFromJwt(anyString())).thenReturn(Optional.of(user))
    handler.onAuthenticationSuccess(request, response, UsernamePasswordAuthenticationToken(user, "pass"))
    verify(restTemplate).delete("/token?authJwtId={authJwtId}", "jwtId")
  }

  @Test
  fun `onAuthenticationSuccess new jwt id value`() {
    whenever(jwtCookieHelper.readValueFromCookie(any())).thenReturn(Optional.of("cookie_value"))
    whenever(jwtAuthenticationHelper.readUserDetailsFromJwt(anyString())).thenReturn(Optional.of(user))
    whenever(jwtAuthenticationHelper.createJwt(any())).thenReturn("newJwt")
    val token = UsernamePasswordAuthenticationToken(user, "pass")
    handler.onAuthenticationSuccess(request, response, token)

    verify(jwtCookieHelper).addCookieToResponse(request, response, "newJwt")
    verify(jwtAuthenticationHelper).createJwt(token)
  }

  @Test
  fun `onAuthenticationSuccess existing cookie verification disabled`() {
    whenever(jwtCookieHelper.readValueFromCookie(any())).thenReturn(Optional.of("cookie_value"))
    whenever(jwtAuthenticationHelper.readUserDetailsFromJwt(anyString())).thenReturn(Optional.of(user))
    handlerTokenVerificationDisabled.onAuthenticationSuccess(
      request,
      response,
      UsernamePasswordAuthenticationToken(user, "pass")
    )
    verifyZeroInteractions(restTemplate)
  }

  @Test
  fun `updateAuthenticationInRequest no existing cookie`() {
    whenever(jwtAuthenticationHelper.createJwt(any())).thenReturn("newJwt")
    val token = UsernamePasswordAuthenticationToken(user, "pass")
    handler.updateAuthenticationInRequest(request, response, token)

    verify(jwtCookieHelper).addCookieToResponse(request, response, "newJwt")
    verify(jwtAuthenticationHelper).createJwt(token)
  }

  @Test
  fun `updateAuthenticationInRequest existing cookie`() {
    whenever(jwtCookieHelper.readValueFromCookie(any())).thenReturn(Optional.of("cookie_value"))
    whenever(jwtAuthenticationHelper.readUserDetailsFromJwt(anyString())).thenReturn(Optional.of(user))
    whenever(jwtAuthenticationHelper.createJwt(any())).thenReturn("newJwt")
    val token = UsernamePasswordAuthenticationToken(user, "pass")
    handler.updateAuthenticationInRequest(request, response, token)

    verify(jwtCookieHelper).addCookieToResponse(request, response, "newJwt")
    verify(jwtAuthenticationHelper).createJwtWithId(token, "jwtId")
  }

  @BeforeEach
  internal fun setupHandlers() {
    handler.setRedirectStrategy(redirectStrategy)
    handlerTokenVerificationDisabled.setRedirectStrategy(redirectStrategy)
  }
}
