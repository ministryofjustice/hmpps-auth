@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.oauth2server.security

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.RedirectStrategy
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.config.CookieRequestCache
import uk.gov.justice.digital.hmpps.oauth2server.config.SimpleSavedRequest
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.auth
import uk.gov.justice.digital.hmpps.oauth2server.service.ClientService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import java.util.Optional
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JwtAuthenticationSuccessHandlerTest {
  private val jwtCookieHelper: JwtCookieHelper = mock()
  private val jwtAuthenticationHelper: JwtAuthenticationHelper = mock()
  private val requestCache: CookieRequestCache = mock()
  private val verifyEmailService: VerifyEmailService = mock()
  private val clientService: ClientService = mock()
  private val request: HttpServletRequest = mock()
  private val response: HttpServletResponse = mock()
  private val redirectStrategy: RedirectStrategy = mock()
  private val restTemplate: RestTemplate = mock()
  private val handler = JwtAuthenticationSuccessHandler(
    jwtCookieHelper,
    jwtAuthenticationHelper,
    requestCache,
    verifyEmailService,
    clientService,
    restTemplate,
    true
  )
  private val handlerTokenVerificationDisabled = JwtAuthenticationSuccessHandler(
    jwtCookieHelper,
    jwtAuthenticationHelper,
    requestCache,
    verifyEmailService,
    clientService,
    restTemplate,
    false
  )
  private val user = UserDetailsImpl("user", "name", setOf(), auth.name, "userid", "jwtId")

  @Nested
  inner class onAuthenticationSuccess {
    @Test
    fun verifyEnabledAlreadyVerified() {
      whenever(verifyEmailService.isNotVerified(anyString())).thenReturn(false)
      handler.onAuthenticationSuccess(request, response, UsernamePasswordAuthenticationToken("user", "pass"))
      verify(redirectStrategy).sendRedirect(request, response, "/")
    }

    @Test
    fun verifyEnabledNotVerified() {
      whenever(verifyEmailService.isNotVerified(anyString())).thenReturn(true)
      handler.onAuthenticationSuccess(request, response, UsernamePasswordAuthenticationToken("user", "pass"))
      verify(redirectStrategy).sendRedirect(request, response, "/verify-email")
    }

    @Test
    fun `existing cookie`() {
      whenever(jwtCookieHelper.readValueFromCookie(any())).thenReturn(Optional.of("cookie_value"))
      whenever(jwtAuthenticationHelper.readUserDetailsFromJwt(anyString())).thenReturn(Optional.of(user))
      handler.onAuthenticationSuccess(request, response, UsernamePasswordAuthenticationToken("user", "pass"))
      verify(restTemplate).delete("/token?authJwtId={authJwtId}", "jwtId")
    }

    @Test
    fun `new jwt id value`() {
      whenever(jwtCookieHelper.readValueFromCookie(any())).thenReturn(Optional.of("cookie_value"))
      whenever(jwtAuthenticationHelper.readUserDetailsFromJwt(anyString())).thenReturn(Optional.of(user))
      whenever(jwtAuthenticationHelper.createJwt(any())).thenReturn("newJwt")
      val token = UsernamePasswordAuthenticationToken("user", "pass")
      handler.onAuthenticationSuccess(request, response, token)

      verify(jwtCookieHelper).addCookieToResponse(request, response, "newJwt")
      verify(jwtAuthenticationHelper).createJwt(token)
    }

    @Test
    fun `existing cookie verification disabled`() {
      whenever(jwtCookieHelper.readValueFromCookie(any())).thenReturn(Optional.of("cookie_value"))
      whenever(jwtAuthenticationHelper.readUserDetailsFromJwt(anyString())).thenReturn(Optional.of(user))
      handlerTokenVerificationDisabled.onAuthenticationSuccess(
        request,
        response,
        UsernamePasswordAuthenticationToken("user", "pass")
      )
      verifyZeroInteractions(restTemplate)
    }
  }

  @Nested
  inner class updateAuthenticationInRequest {
    @Test
    fun `no existing cookie`() {
      whenever(jwtAuthenticationHelper.createJwt(any())).thenReturn("newJwt")
      val token = UsernamePasswordAuthenticationToken("user", "pass")
      handler.updateAuthenticationInRequest(request, response, token)

      verify(jwtCookieHelper).addCookieToResponse(request, response, "newJwt")
      verify(jwtAuthenticationHelper).createJwt(token)
    }

    @Test
    fun `existing cookie`() {
      whenever(jwtCookieHelper.readValueFromCookie(any())).thenReturn(Optional.of("cookie_value"))
      whenever(jwtAuthenticationHelper.readUserDetailsFromJwt(anyString())).thenReturn(Optional.of(user))
      whenever(jwtAuthenticationHelper.createJwt(any())).thenReturn("newJwt")
      val token = UsernamePasswordAuthenticationToken("user", "pass")
      handler.updateAuthenticationInRequest(request, response, token)

      verify(jwtCookieHelper).addCookieToResponse(request, response, "newJwt")
      verify(jwtAuthenticationHelper).createJwtWithId(token, "jwtId", false)
    }

    @Test
    fun `existing cookie copies mfa value over`() {
      val mfaUser = UserDetailsImpl("user", "name", setOf(), auth.name, "userid", "jwtId", true)

      whenever(jwtCookieHelper.readValueFromCookie(any())).thenReturn(Optional.of("cookie_value"))
      whenever(jwtAuthenticationHelper.readUserDetailsFromJwt(anyString())).thenReturn(Optional.of(mfaUser))
      whenever(jwtAuthenticationHelper.createJwt(any())).thenReturn("newJwt")
      val token = UsernamePasswordAuthenticationToken("user", "pass")
      handler.updateAuthenticationInRequest(request, response, token)

      verify(jwtAuthenticationHelper).createJwtWithId(token, "jwtId", true)
    }
  }

  @Nested
  inner class proceed {
    @Test
    fun `use request cache`() {
      whenever(requestCache.getRequest(any(), any())).thenReturn(SimpleSavedRequest("https://some_url/joe"))
      val token = UsernamePasswordAuthenticationToken(
        UserDetailsImpl("user", jwtId = "bob", userId = "userId", authorities = setOf(), name = "joe"), "pass"
      )
      handler.proceed(request, response, token)
      verify(redirectStrategy).sendRedirect(request, response, "https://some_url/joe")
    }

    @Test
    fun `request cache overrides target url`() {
      whenever(request.getParameter(any())).thenReturn("https://other/value")
      whenever(requestCache.getRequest(any(), any())).thenReturn(SimpleSavedRequest("https://some_url/joe"))
      val token = UsernamePasswordAuthenticationToken(
        UserDetailsImpl("user", jwtId = "bob", userId = "userId", authorities = setOf(), name = "joe"), "pass"
      )
      handler.proceed(request, response, token)
      verify(redirectStrategy).sendRedirect(request, response, "https://some_url/joe")
    }

    @Test
    fun `redirects if no request cache and valid target url`() {
      whenever(clientService.isValid(any())).thenReturn(true)
      whenever(request.getParameter(any())).thenReturn("https://other/value")
      val token = UsernamePasswordAuthenticationToken(
        UserDetailsImpl("user", jwtId = "bob", userId = "userId", authorities = setOf(), name = "joe"), "pass"
      )
      handler.proceed(request, response, token)
      verify(redirectStrategy).sendRedirect(request, response, "https://other/value")
      verify(request).getParameter("redirect_uri")
      verify(clientService).isValid("https://other/value")
    }

    @Test
    fun `uses default if no request cache and no valid target url`() {
      whenever(request.getParameter(any())).thenReturn("https://other/value")
      val token = UsernamePasswordAuthenticationToken(
        UserDetailsImpl("user", jwtId = "bob", userId = "userId", authorities = setOf(), name = "joe"), "pass"
      )
      handler.proceed(request, response, token)
      verify(redirectStrategy).sendRedirect(request, response, "/")
    }

    @Test
    fun `default value not checked in client service`() {
      val token = UsernamePasswordAuthenticationToken(
        UserDetailsImpl("user", jwtId = "bob", userId = "userId", authorities = setOf(), name = "joe"), "pass"
      )
      handler.proceed(request, response, token)
      verify(redirectStrategy).sendRedirect(request, response, "/")
      verifyZeroInteractions(clientService)
    }
  }

  @Nested
  inner class updateMfaInRequest {
    @Test
    fun `mfa is updated in request`() {
      whenever(jwtAuthenticationHelper.createJwtWithId(any(), anyString(), anyBoolean())).thenReturn("newJwt")
      val token = UsernamePasswordAuthenticationToken(
        UserDetailsImpl("user", jwtId = "bob", userId = "userId", authorities = setOf(), name = "joe"), "pass"
      )
      whenever(jwtAuthenticationHelper.readAuthenticationFromJwt(anyString())).thenReturn(Optional.of(token))
      handler.updateMfaInRequest(request, response, token)

      verify(jwtCookieHelper).addCookieToResponse(request, response, "newJwt")
      verify(jwtAuthenticationHelper).createJwtWithId(token, "bob", true)
      verify(jwtAuthenticationHelper).readAuthenticationFromJwt("newJwt")
      assertThat(SecurityContextHolder.getContext().authentication).isEqualTo(token)
    }
  }

  @BeforeEach
  internal fun setupHandlers() {
    handler.setRedirectStrategy(redirectStrategy)
    handlerTokenVerificationDisabled.setRedirectStrategy(redirectStrategy)
  }
}
