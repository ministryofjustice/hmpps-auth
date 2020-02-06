package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.service.LoginFlowException
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaFlowException
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
  private val controller = MfaController(jwtAuthenticationSuccessHandler, tokenService, userService, telemetryClient, mfaService, false)

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
  fun `mfaChallengeRequest no token`() {
    val modelAndView = controller.mfaChallengeRequest(null)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/login?error=mfainvalid")
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
    val user = User.of("someuser")
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("otken", TokenType.MFA, null, user)))
    whenever(mfaService.validateAndRemoveMfaCode(anyString(), anyString())).thenThrow(MfaFlowException("invalid"))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
    val modelAndView = controller.mfaChallenge("some token", "some code", request, response)
    assertThat(modelAndView!!.viewName).isEqualTo("mfaChallenge")
    assertThat(modelAndView.model).containsOnly(entry("token", "some token"), entry("error", "invalid"))
  }

  @Test
  fun `mfaChallenge code locked`() {
    val user = User.of("someuser")
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("otken", TokenType.MFA, null, user)))
    whenever(mfaService.validateAndRemoveMfaCode(anyString(), anyString())).thenThrow(LoginFlowException("locked"))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
    val modelAndView = controller.mfaChallenge("some token", "some code", request, response)
    assertThat(modelAndView!!.viewName).isEqualTo("redirect:/login?error=locked")
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
  fun `mfaChallenge success telemetry`() {
    val user = User.of("someuser")
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("otken", TokenType.MFA, null, user)))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
    controller.mfaChallenge("some token", "some code", request, response)
    verify(telemetryClient).trackEvent("MFAAuthenticateSuccess", mapOf("username" to "someuser"), null)
  }

  @Test
  fun `mfaChallenge check success handler`() {
    val user = User.builder().authorities(setOf("ROLE_BOB", "ROLE_JOE").map { Authority(it, "role name") }.toSet()).username("someuser").build()
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("otken", TokenType.MFA, null, user)))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
    controller.mfaChallenge("some token", "some code", request, response)
    verify(jwtAuthenticationSuccessHandler).onAuthenticationSuccess(eq(request), eq(response), check {
      assertThat(it.principal).isEqualTo(user)
      assertThat(it.authorities.map { a -> a.authority }).containsOnly("ROLE_BOB", "ROLE_JOE")
    })
  }

  @Test
  fun `mfaResendRequest check view`() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
    val modelAndView = controller.mfaResendRequest("some token")
    assertThat(modelAndView.viewName).isEqualTo("mfaResend")
  }

  @Test
  fun `mfaResendRequest check model`() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
    val modelAndView = controller.mfaResendRequest("some token")
    assertThat(modelAndView.model).containsExactly(entry("token", "some token"))
  }

  @Test
  fun `mfaResendRequest check service call`() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
    controller.mfaResendRequest("some token")
    verify(tokenService).checkToken(TokenType.MFA, "some token")
  }

  @Test
  fun `mfaResendRequest error`() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
    val modelAndView = controller.mfaResendRequest("some token")
    assertThat(modelAndView.viewName).isEqualTo("redirect:/login?error=mfainvalid")
  }

  @Test
  fun `mfaResend token invalid`() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
    val modelAndView = controller.mfaResend("some token", request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/login?error=mfainvalid")
  }

  @Test
  fun `mfaResend no code found`() {
    val modelAndView = controller.mfaResend("some token", request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/login?error=mfainvalid")
  }

  @Test
  fun `mfaResend check view`() {
    whenever(mfaService.resendMfaCode(anyString())).thenReturn("code")
    val modelAndView = controller.mfaResend("some token", request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/mfa-challenge?token=some token")
  }

  @Test
  fun `mfaResend check view some test enabled`() {
    whenever(mfaService.resendMfaCode(anyString())).thenReturn("code")
    val modelAndView = MfaController(jwtAuthenticationSuccessHandler, tokenService, userService, telemetryClient, mfaService, true)
        .mfaResend("some token", request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/mfa-challenge?token=some token&smokeCode=code")
  }

  @Test
  fun `mfaResend check service call`() {
    whenever(mfaService.resendMfaCode(anyString())).thenReturn("code")
    controller.mfaResend("some token", request, response)
    verify(mfaService).resendMfaCode("some token")
  }
}
