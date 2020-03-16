package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.MfaPreferenceType
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

  @Nested
  inner class MfaChallengeRequest {
    @Test
    fun `mfaChallengeRequest check view`() {
      val user = User.of("someuser")
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("token", TokenType.MFA, null, user)))
      val modelAndView = controller.mfaChallengeRequest("some token", MfaPreferenceType.TEXT)
      assertThat(modelAndView.viewName).isEqualTo("mfaChallenge")
    }

    @Test
    fun `mfaChallengeRequest check model email`() {
      val user = User.builder().mfaPreference(MfaPreferenceType.EMAIL).build()
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("token", TokenType.MFA, null, user)))
      val modelAndView = controller.mfaChallengeRequest("some token", MfaPreferenceType.EMAIL)
      assertThat(modelAndView.model).containsOnly(entry("mfaPreference", MfaPreferenceType.EMAIL), entry("token", "some token"))
    }

    @Test
    fun `mfaChallengeRequest check model text`() {
      val user = User.builder().mfaPreference(MfaPreferenceType.TEXT).build()
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("token", TokenType.MFA, null, user)))
      val modelAndView = controller.mfaChallengeRequest("some token", MfaPreferenceType.TEXT)
      assertThat(modelAndView.model).containsOnly(entry("mfaPreference", MfaPreferenceType.TEXT), entry("token", "some token"))
    }

    @Test
    fun `mfaChallengeRequest check service call`() {
      val user = User.of("someuser")
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("token", TokenType.MFA, null, user)))
      controller.mfaChallengeRequest("some token", MfaPreferenceType.TEXT)
      verify(tokenService).checkToken(TokenType.MFA, "some token")
    }

    @Test
    fun `mfaChallengeRequest no token`() {
      val modelAndView = controller.mfaChallengeRequest(null, MfaPreferenceType.TEXT)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/login?error=mfainvalid")
    }

    @Test
    fun `mfaChallengeRequest error`() {
      val user = User.of("someuser")
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
      whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("token", TokenType.MFA, null, user)))
      val modelAndView = controller.mfaChallengeRequest("some token", MfaPreferenceType.TEXT)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/login?error=mfainvalid")
    }
  }

  @Nested
  inner class MfaChallenge {
    @Test
    fun `mfaChallenge token invalid`() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
      val modelAndView = controller.mfaChallenge("some token", MfaPreferenceType.EMAIL, "some code", request, response)
      assertThat(modelAndView!!.viewName).isEqualTo("redirect:/login?error=mfainvalid")
    }

    @Test
    fun `mfaEmailChallenge code invalid`() {
      val user = User.of("someuser")
      whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("otken", TokenType.MFA, null, user)))
      whenever(mfaService.validateAndRemoveMfaCode(anyString(), anyString())).thenThrow(MfaFlowException("invalid"))
      whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
      val modelAndView = controller.mfaChallenge("some token", MfaPreferenceType.EMAIL, "some code", request, response)
      assertThat(modelAndView!!.viewName).isEqualTo("mfaChallenge")
      assertThat(modelAndView.model).containsOnly(entry("token", "some token"), entry("error", "invalid"), entry("mfaPreference", MfaPreferenceType.EMAIL))
    }

    @Test
    fun `mfaTextChallenge code invalid`() {
      val user = User.of("someuser")
      whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("otken", TokenType.MFA, null, user)))
      whenever(mfaService.validateAndRemoveMfaCode(anyString(), anyString())).thenThrow(MfaFlowException("invalid"))
      whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
      val modelAndView = controller.mfaChallenge("some token", MfaPreferenceType.TEXT, "some code", request, response)
      assertThat(modelAndView!!.viewName).isEqualTo("mfaChallenge")
      assertThat(modelAndView.model).containsOnly(entry("token", "some token"), entry("error", "invalid"), entry("mfaPreference", MfaPreferenceType.TEXT))
    }

    @Test
    fun `mfaEmailChallenge code locked`() {
      val user = User.of("someuser")
      whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("token", TokenType.MFA, null, user)))
      whenever(mfaService.validateAndRemoveMfaCode(anyString(), anyString())).thenThrow(LoginFlowException("locked"))
      whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
      val modelAndView = controller.mfaChallenge("some token", MfaPreferenceType.EMAIL, "some code", request, response)
      assertThat(modelAndView!!.viewName).isEqualTo("redirect:/login?error=locked")
    }

    @Test
    fun `mfaTextChallenge code locked`() {
      val user = User.of("someuser")
      whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("otken", TokenType.MFA, null, user)))
      whenever(mfaService.validateAndRemoveMfaCode(anyString(), anyString())).thenThrow(LoginFlowException("locked"))
      whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
      val modelAndView = controller.mfaChallenge("some token", MfaPreferenceType.TEXT, "some code", request, response)
      assertThat(modelAndView!!.viewName).isEqualTo("redirect:/login?error=locked")
    }

    @Test
    fun `mfaEmailChallenge success`() {
      val user = User.of("someuser")
      whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("otken", TokenType.MFA, null, user)))
      whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
      val modelAndView = controller.mfaChallenge("some token", MfaPreferenceType.EMAIL, "some code", request, response)
      assertThat(modelAndView).isNull()
    }

    @Test
    fun `mfaTextChallenge success`() {
      val user = User.of("someuser")
      whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("otken", TokenType.MFA, null, user)))
      whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
      val modelAndView = controller.mfaChallenge("some token", MfaPreferenceType.TEXT, "some code", request, response)
      assertThat(modelAndView).isNull()
    }

    @Test
    fun `mfaEmailChallenge success telemetry`() {
      val user = User.of("someuser")
      whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("otken", TokenType.MFA, null, user)))
      whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
      controller.mfaChallenge("some token", MfaPreferenceType.EMAIL, "some code", request, response)
      verify(telemetryClient).trackEvent("MFAAuthenticateSuccess", mapOf("username" to "someuser"), null)
    }

    @Test
    fun `mfaEmailChallenge check success handler`() {
      val user = User.builder().authorities(setOf("ROLE_BOB", "ROLE_JOE").map { Authority(it, "role name") }.toSet()).username("someuser").build()
      whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(UserToken("otken", TokenType.MFA, null, user)))
      whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
      controller.mfaChallenge("some token", MfaPreferenceType.EMAIL, "some code", request, response)
      verify(jwtAuthenticationSuccessHandler).onAuthenticationSuccess(eq(request), eq(response), check {
        assertThat(it.principal).isEqualTo(user)
        assertThat(it.authorities.map { a -> a.authority }).containsOnly("ROLE_BOB", "ROLE_JOE")
      })
    }
  }

  @Nested
  inner class MfaResendEmailRequest {
    @Test
    fun `mfaEmailResendRequest check view`() {
      whenever(mfaService.buildModelAndViewWithMfaResendOptions(any(), eq(MfaPreferenceType.EMAIL))).thenReturn(ModelAndView("mfaResend"))
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      val modelAndView = controller.mfaResendRequest("some token", MfaPreferenceType.EMAIL)
      assertThat(modelAndView.viewName).isEqualTo("mfaResend")
    }

    @Test
    fun `mfaTextResendRequest check view`() {
      whenever(mfaService.buildModelAndViewWithMfaResendOptions(any(), eq(MfaPreferenceType.TEXT))).thenReturn(ModelAndView("mfaResend"))
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      val modelAndView = controller.mfaResendRequest("some token", MfaPreferenceType.TEXT)
      assertThat(modelAndView.viewName).isEqualTo("mfaResend")
    }

    @Test
    fun `mfaEmailResendRequest check model`() {
      whenever(mfaService.buildModelAndViewWithMfaResendOptions(any(), eq(MfaPreferenceType.EMAIL)))
          .thenReturn(ModelAndView("mfaResend", "token", "some token").addObject("mfaPreference", MfaPreferenceType.EMAIL))
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      val modelAndView = controller.mfaResendRequest("some token", MfaPreferenceType.EMAIL)
      assertThat(modelAndView.model).containsExactly(entry("token", "some token"), entry("mfaPreference", MfaPreferenceType.EMAIL))
    }

    @Test
    fun `mfaTextResendRequest check model`() {
      whenever(mfaService.buildModelAndViewWithMfaResendOptions(any(), eq(MfaPreferenceType.TEXT)))
          .thenReturn(ModelAndView("mfaResend", "token", "some token").addObject("mfaPreference", MfaPreferenceType.TEXT))
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      val modelAndView = controller.mfaResendRequest("some token", MfaPreferenceType.TEXT)
      assertThat(modelAndView.model).containsExactly(entry("token", "some token"), entry("mfaPreference", MfaPreferenceType.TEXT))
    }

    @Test
    fun `mfaEmailResendRequest check service call`() {
      whenever(mfaService.buildModelAndViewWithMfaResendOptions(any(), eq(MfaPreferenceType.EMAIL))).thenReturn(ModelAndView("mfaResend"))
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      controller.mfaResendRequest("some token", MfaPreferenceType.EMAIL)
      verify(tokenService).checkToken(TokenType.MFA, "some token")
    }

    @Test
    fun `mfaTextResendRequest check service call`() {
      whenever(mfaService.buildModelAndViewWithMfaResendOptions(any(), eq(MfaPreferenceType.TEXT))).thenReturn(ModelAndView("mfaResend"))
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      controller.mfaResendRequest("some token", MfaPreferenceType.TEXT)
      verify(tokenService).checkToken(TokenType.MFA, "some token")
    }

    @Test
    fun `mfaResendEmailRequest error`() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
      val modelAndView = controller.mfaResendRequest("some token", MfaPreferenceType.EMAIL)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/login?error=mfainvalid")
    }
  }

  @Test
  fun `mfaResendTextRequest error`() {
    val user = User.builder().email("auth.user@digital.justice.gov.uk").mobile("07700900321").mfaPreference(MfaPreferenceType.TEXT).build()
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
    whenever(tokenService.getUserFromToken(any(), anyString())).thenReturn(user)
    val modelAndView = controller.mfaResendRequest("some token", MfaPreferenceType.TEXT)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/login?error=mfainvalid")
  }

  @Test
  fun `mfaResendEmail token invalid`() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
    val modelAndView = controller.mfaResend("some token", MfaPreferenceType.EMAIL, request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/login?error=mfainvalid")
  }

  @Test
  fun `mfaResendText token invalid`() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
    val modelAndView = controller.mfaResend("some token", MfaPreferenceType.TEXT, request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/login?error=mfainvalid")
  }

  @Test
  fun `mfaResendEmail no code found`() {
    val modelAndView = controller.mfaResend("some token", MfaPreferenceType.EMAIL, request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/login?error=mfainvalid")
  }

  @Test
  fun `mfaResendText no code found`() {
    val modelAndView = controller.mfaResend("some token", MfaPreferenceType.TEXT, request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/login?error=mfainvalid")
  }

  @Test
  fun `mfaResendEmail check view`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.EMAIL))).thenReturn("code")
    val modelAndView = controller.mfaResend("some token", MfaPreferenceType.EMAIL, request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/mfa-challenge")
  }

  @Test
  fun `mfaResendText check view`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.TEXT))).thenReturn("code")
    val modelAndView = controller.mfaResend("some token", MfaPreferenceType.TEXT, request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/mfa-challenge")
  }

  @Test
  fun `mfaResendEmail check model`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.EMAIL))).thenReturn("code")
    val modelAndView = controller.mfaResend("some token", MfaPreferenceType.EMAIL, request, response)
    assertThat(modelAndView.model).containsExactly(entry("token", "some token"), entry("mfaPreference", MfaPreferenceType.EMAIL))
  }

  @Test
  fun `mfaResendText check model`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.TEXT))).thenReturn("code")
    val modelAndView = controller.mfaResend("some token", MfaPreferenceType.TEXT, request, response)
    assertThat(modelAndView.model).containsExactly(entry("token", "some token"), entry("mfaPreference", MfaPreferenceType.TEXT))
  }

  @Test
  fun `mfaResendEmail check view smoke test enabled`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.EMAIL))).thenReturn("code")
    val modelAndView = MfaController(jwtAuthenticationSuccessHandler, tokenService, userService, telemetryClient, mfaService, true)
        .mfaResend("some token", MfaPreferenceType.EMAIL, request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/mfa-challenge")
  }

  @Test
  fun `mfaResendText check view smoke test enabled`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.TEXT))).thenReturn("code")
    val modelAndView = MfaController(jwtAuthenticationSuccessHandler, tokenService, userService, telemetryClient, mfaService, true)
        .mfaResend("some token", MfaPreferenceType.TEXT, request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/mfa-challenge")
  }

  @Test
  fun `mfaResendEmail check model smoke test enabled`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.EMAIL))).thenReturn("code")
    val modelAndView = MfaController(jwtAuthenticationSuccessHandler, tokenService, userService, telemetryClient, mfaService, true)
        .mfaResend("some token", MfaPreferenceType.EMAIL, request, response)
    assertThat(modelAndView.model).containsExactly(entry("token", "some token"), entry("mfaPreference", MfaPreferenceType.EMAIL), entry("smokeCode", "code"))
  }

  @Test
  fun `mfaResendText check model smoke test enabled`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.TEXT))).thenReturn("code")
    val modelAndView = MfaController(jwtAuthenticationSuccessHandler, tokenService, userService, telemetryClient, mfaService, true)
        .mfaResend("some token", MfaPreferenceType.TEXT, request, response)
    assertThat(modelAndView.model).containsExactly(entry("token", "some token"), entry("mfaPreference", MfaPreferenceType.TEXT), entry("smokeCode", "code"))
  }

  @Test
  fun `mfaResendEmail check service call`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.EMAIL))).thenReturn("code")
    controller.mfaResend("some token", MfaPreferenceType.EMAIL, request, response)
    verify(mfaService).resendMfaCode("some token", MfaPreferenceType.EMAIL)
  }

  @Test
  fun `mfaResendText check service call`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.TEXT))).thenReturn("code")
    controller.mfaResend("some token", MfaPreferenceType.TEXT, request, response)
    verify(mfaService).resendMfaCode("some token", MfaPreferenceType.TEXT)
  }
}
