package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class MfaControllerTest {
  private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler = mock()
  private val tokenService: TokenService = mock()
  private val userService: UserService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val mfaService: MfaService = mock()
  private val request: HttpServletRequest = MockHttpServletRequest()
  private val response: HttpServletResponse = MockHttpServletResponse()
  private lateinit var controller: MfaController

  @Before
  fun setUp() {
    controller = MfaController(jwtAuthenticationSuccessHandler, tokenService, userService, telemetryClient, mfaService, true);
  }

  @Test
  fun `mfaChallengeRequest check view`() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
    val modelAndView = controller.mfaChallengeRequest("some token")
    assertThat(modelAndView.viewName).isEqualTo("mfaChallenge")
  }

  @Test
  fun `mfaChallengeRequest check model`() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
    val modelAndView = controller.mfaChallengeRequest("some token")
    assertThat(modelAndView.model).containsExactly(entry("token", "some token"))
  }

  @Test
  fun `mfaChallengeRequest check service call`() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
    controller.mfaChallengeRequest("some token")
    verify(tokenService).checkToken(TokenType.MFA, "some token")
  }

  @Test
  fun `mfaChallengeRequest error`() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
    val modelAndView = controller.mfaChallengeRequest("some token")
    assertThat(modelAndView.viewName).isEqualTo("redirect:/login?error=mfainvalid")
  }

  @Test
  fun `mfaChallenge token invalid`() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
    val modelAndView = controller.mfaChallenge("some token", "some code", request, response)
    assertThat(modelAndView!!.viewName).isEqualTo("redirect:/login?error=mfainvalid")
  }

  @Test
  fun `mfaChallenge code invalid`() {
    whenever(mfaService.validateMfaCode(anyString())).thenReturn(Optional.of("invalid"))
    val modelAndView = controller.mfaChallenge("some token", "some code", request, response)
    assertThat(modelAndView!!.viewName).isEqualTo("mfaChallenge")
    assertThat(modelAndView.model).containsOnly(entry("token", "some token"), entry("error", "invalid"))
  }

  @Test
  fun `mfaChallenge success`() {
    val user = User.of("someuser")
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("otken", TokenType.MFA, null, user)))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
    val modelAndView = controller.mfaChallenge("some token", "some code", request, response)
    assertThat(modelAndView).isNull()
  }

  @Test
  fun `mfaChallenge check remove tokens`() {
    val user = User.of("someuser")
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("otken", TokenType.MFA, null, user)))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
    controller.mfaChallenge("some token", "some code", request, response)
    verify(tokenService).removeToken(TokenType.MFA, "some token");
    verify(tokenService).removeToken(TokenType.MFA_CODE, "some code");
  }

  @Test
  fun `mfaChallenge check success handler`() {
    val user = User.of("someuser")
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("otken", TokenType.MFA, null, user)))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
    controller.mfaChallenge("some token", "some code", request, response)
    verify(jwtAuthenticationSuccessHandler).onAuthenticationSuccess(eq(request), eq(response), check {
      assertThat(it.principal).isEqualTo(user)
    })
  }
}
