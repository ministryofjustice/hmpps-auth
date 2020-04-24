package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.auth
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationHelper
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtCookieHelper
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ClearAllSessionsLogoutHandlerTest {
  private val jwtCookieHelper: JwtCookieHelper = mock()
  private val jwtAuthenticationHelper: JwtAuthenticationHelper = mock()
  private val restTemplate: RestTemplate = mock()
  private val clearAllSessionsLogoutHandler = ClearAllSessionsLogoutHandler(jwtCookieHelper, jwtAuthenticationHelper, restTemplate, true)
  private val clearAllSessionsLogoutHandlerTokenVerificationDisabled = ClearAllSessionsLogoutHandler(jwtCookieHelper, jwtAuthenticationHelper, restTemplate, false)
  private val httpServletRequest: HttpServletRequest = mock()
  private val httpServletResponse: HttpServletResponse = mock()
  private val user = UserDetailsImpl("user", "name", setOf(), auth.name, "userid", "jwtId")

  @Test
  fun `logout no cookie found`() {
    clearAllSessionsLogoutHandler.logout(httpServletRequest, httpServletResponse, null)
    verifyZeroInteractions(restTemplate)
  }

  @Test
  fun `logout no authentication found`() {
    whenever(jwtCookieHelper.readValueFromCookie(any())).thenReturn(Optional.of("cookie_value"))
    clearAllSessionsLogoutHandler.logout(httpServletRequest, httpServletResponse, null)
    verifyZeroInteractions(restTemplate)
  }

  @Test
  fun `logout cookie found`() {
    whenever(jwtCookieHelper.readValueFromCookie(any())).thenReturn(Optional.of("cookie_value"))
    whenever(jwtAuthenticationHelper.readUserDetailsFromJwt(anyString())).thenReturn(Optional.of(user))
    clearAllSessionsLogoutHandler.logout(httpServletRequest, httpServletResponse, null)
    verify(restTemplate).delete("/token/{authJwtId}", "jwtId")
  }

  @Test
  fun `logout cookie found but verification disabled`() {
    whenever(jwtCookieHelper.readValueFromCookie(any())).thenReturn(Optional.of("cookie_value"))
    whenever(jwtAuthenticationHelper.readUserDetailsFromJwt(anyString())).thenReturn(Optional.of(user))
    clearAllSessionsLogoutHandlerTokenVerificationDisabled.logout(httpServletRequest, httpServletResponse, null)
    verifyZeroInteractions(restTemplate)
  }
}
