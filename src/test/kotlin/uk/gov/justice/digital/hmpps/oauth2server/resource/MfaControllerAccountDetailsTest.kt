package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.MfaPreferenceType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.service.LoginFlowException
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaData
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaFlowException
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import java.time.LocalDateTime
import java.util.Optional
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class MfaControllerAccountDetailsTest {
  private val tokenService: TokenService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val mfaService: MfaService = mock()
  private val request: HttpServletRequest = MockHttpServletRequest()
  private val response: HttpServletResponse = MockHttpServletResponse()
  private val controller =
    MfaControllerAccountDetails(
      tokenService,
      telemetryClient,
      mfaService,
      false
    )
  private val controllerSmokeTestEnabled =
    MfaControllerAccountDetails(
      tokenService,
      telemetryClient,
      mfaService,
      true
    )
  private val authentication = UsernamePasswordAuthenticationToken("bob", "pass")

  @Nested
  inner class MfaChallengeRequest {
    @Test
    fun `mfaChallengeRequest check view`() {
      whenever(mfaService.createTokenAndSendMfaCode(anyString())).thenReturn(
        MfaData(
          "token",
          "code",
          MfaPreferenceType.EMAIL
        )
      )
      whenever(mfaService.getCodeDestination(anyString(), any())).thenReturn(
        ""
      )
      val modelAndView = controller.mfaChallengeRequestAccountDetail(authentication, "TEXT", null, null, null, null)
      assertThat(modelAndView.viewName).isEqualTo("mfaChallengeAccountDetails")
    }

    @Test
    fun `mfaChallengeRequest check model email`() {
      whenever(mfaService.createTokenAndSendMfaCode(anyString())).thenReturn(
        MfaData(
          "some token",
          "code",
          MfaPreferenceType.EMAIL
        )
      )
      whenever(mfaService.getCodeDestination(any(), eq(MfaPreferenceType.EMAIL))).thenReturn("auth******@******.gov.uk")
      val modelAndView =
        controller.mfaChallengeRequestAccountDetail(authentication, "email", null, null, "password token", null)
      assertThat(modelAndView.model).containsOnly(
        entry("mfaPreference", MfaPreferenceType.EMAIL),
        entry("codeDestination", "auth******@******.gov.uk"),
        entry("token", "some token"),
        entry("passToken", "password token"),
        entry("contactType", "email")
      )
    }

    @Test
    fun `mfaChallengeRequest check model contains when error when error in param`() {
      whenever(mfaService.getCodeDestination(any(), eq(MfaPreferenceType.EMAIL))).thenReturn("auth******@******.gov.uk")
      val modelAndView = controller.mfaChallengeRequestAccountDetail(
        authentication,
        "email",
        "invalid",
        "some token",
        "password token",
        MfaPreferenceType.EMAIL
      )
      assertThat(modelAndView.model).containsOnly(
        entry("mfaPreference", MfaPreferenceType.EMAIL),
        entry("codeDestination", "auth******@******.gov.uk"),
        entry("error", "invalid"),
        entry("token", "some token"),
        entry("passToken", "password token"),
        entry("contactType", "email")
      )
    }

    @Test
    fun `Primary email mfaChallenge Request expired password token`() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"))

      val view = controller.mfaChallengeRequestAccountDetail(
        authentication,
        "email",
        null,
        null,
        "expired token",
        MfaPreferenceType.EMAIL
      )
      assertThat(view.viewName).isEqualTo("redirect:/account-details?error=tokenexpired")
    }

    @Test
    fun `Primary email mfaChallenge Request invalid password token`() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))

      val view = controller.mfaChallengeRequestAccountDetail(
        authentication,
        "email",
        null,
        null,
        "invalid token",
        MfaPreferenceType.EMAIL
      )
      assertThat(view.viewName).isEqualTo("redirect:/account-details?error=tokeninvalid")
    }

    @Test
    fun `Primary email mfaChallenge Request without password token returns token invalid error`() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))

      val view = controller.mfaChallengeRequestAccountDetail(
        authentication,
        "email",
        null,
        null,
        null,
        MfaPreferenceType.EMAIL
      )
      assertThat(view.viewName).isEqualTo("redirect:/account-details?error=tokeninvalid")
    }

    @Test
    fun `mfaChallengeRequest check model text`() {
      whenever(mfaService.createTokenAndSendMfaCode(anyString())).thenReturn(
        MfaData(
          "some token",
          "code",
          MfaPreferenceType.TEXT
        )
      )
      whenever(mfaService.getCodeDestination(any(), eq(MfaPreferenceType.TEXT))).thenReturn("*******0321")
      val modelAndView =
        controller.mfaChallengeRequestAccountDetail(authentication, "email", null, null, "password token", null)
      assertThat(modelAndView.model).containsOnly(
        entry("mfaPreference", MfaPreferenceType.TEXT),
        entry("codeDestination", "*******0321"),
        entry("token", "some token"),
        entry("passToken", "password token"),
        entry("contactType", "email")
      )
    }

    @Test
    fun `mfaChallengeRequest verify call to createTokenAndSendMfaCode when no errors in params`() {
      whenever(mfaService.createTokenAndSendMfaCode(anyString())).thenReturn(
        MfaData(
          "some token",
          "code",
          MfaPreferenceType.TEXT
        )
      )
      whenever(mfaService.getCodeDestination(any(), eq(MfaPreferenceType.TEXT))).thenReturn("*******0321")
      controller.mfaChallengeRequestAccountDetail(authentication, "email", null, null, "password token", null)
      verify(mfaService, times(1)).createTokenAndSendMfaCode(anyString())
    }

    @Test
    fun `mfaChallengeRequest verify createTokenAndSendMfaCode is not called when errors in params`() {
      whenever(mfaService.getCodeDestination(any(), eq(MfaPreferenceType.TEXT))).thenReturn("*******0321")
      controller.mfaChallengeRequestAccountDetail(
        authentication,
        "email",
        "INVALID",
        "some token",
        "password token",
        MfaPreferenceType.TEXT
      )
      verify(mfaService, never()).createTokenAndSendMfaCode(anyString())
    }
  }

  @Nested
  inner class MfaChallenge {
    @Test
    fun `mfaChallenge token invalid`() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
      val modelAndView = controller.mfaChallengeAccountDetail(
        "some token",
        "pass token",
        MfaPreferenceType.EMAIL,
        "some code",
        "email",
        request,
        response
      )
      assertThat(modelAndView!!.viewName).isEqualTo("redirect:/account-details?error=mfainvalid")
    }

    @Test
    fun `mfaEmailChallenge code invalid`() {
      val user = createSampleUser(username = "someuser")
      whenever(tokenService.getToken(any(), anyString())).thenReturn(
        Optional.of(
          UserToken(
            "otken",
            TokenType.MFA,
            LocalDateTime.now().plusHours(1L),
            user
          )
        )
      )
      whenever(mfaService.validateAndRemoveMfaCode(anyString(), anyString())).thenThrow(MfaFlowException("invalid"))
      val modelAndView = controller.mfaChallengeAccountDetail(
        "some token",
        "pass token",
        MfaPreferenceType.EMAIL,
        "some code",
        "email",
        request,
        response
      )
      assertThat(modelAndView!!.viewName).isEqualTo("redirect:/account/mfa-challenge?contactType=email")
      assertThat(modelAndView.model).containsOnly(
        entry("token", "some token"),
        entry("passToken", "pass token"),
        entry("error", "invalid"),
        entry("mfaPreference", MfaPreferenceType.EMAIL)
      )
    }

    @Test
    fun `mfaTextChallenge code invalid`() {
      val user = createSampleUser(username = "someuser")
      whenever(tokenService.getToken(any(), anyString())).thenReturn(
        Optional.of(
          UserToken(
            "otken",
            TokenType.MFA,
            LocalDateTime.now().plusHours(1L),
            user
          )
        )
      )
      whenever(mfaService.validateAndRemoveMfaCode(anyString(), anyString())).thenThrow(MfaFlowException("invalid"))
      val modelAndView = controller.mfaChallengeAccountDetail(
        "some token",
        "pass token",
        MfaPreferenceType.TEXT,
        "some code",
        "email",
        request,
        response
      )
      assertThat(modelAndView!!.viewName).isEqualTo("redirect:/account/mfa-challenge?contactType=email")
      assertThat(modelAndView.model).containsOnly(
        entry("token", "some token"),
        entry("passToken", "pass token"),
        entry("error", "invalid"),
        entry("mfaPreference", MfaPreferenceType.TEXT)
      )
    }

    @Test
    fun `mfaEmailChallenge code locked`() {
      val user = createSampleUser(username = "someuser")
      whenever(tokenService.getToken(any(), anyString())).thenReturn(
        Optional.of(
          UserToken(
            "token",
            TokenType.MFA,
            LocalDateTime.now().plusHours(1L),
            user
          )
        )
      )
      whenever(mfaService.validateAndRemoveMfaCode(anyString(), anyString())).thenThrow(LoginFlowException("locked"))
      val modelAndView = controller.mfaChallengeAccountDetail(
        "some token",
        "pass token",
        MfaPreferenceType.EMAIL,
        "some code",
        "email",
        request,
        response
      )
      assertThat(modelAndView!!.viewName).isEqualTo("redirect:/login?error=locked")
    }

    @Test
    fun `mfaTextChallenge code locked`() {
      val user = createSampleUser(username = "someuser")
      whenever(tokenService.getToken(any(), anyString())).thenReturn(
        Optional.of(
          UserToken(
            "otken",
            TokenType.MFA,
            LocalDateTime.now().plusHours(1L),
            user
          )
        )
      )
      whenever(mfaService.validateAndRemoveMfaCode(anyString(), anyString())).thenThrow(LoginFlowException("locked"))
      val modelAndView = controller.mfaChallengeAccountDetail(
        "some token",
        "pass token",
        MfaPreferenceType.TEXT,
        "some code",
        "email",
        request,
        response
      )
      assertThat(modelAndView!!.viewName).isEqualTo("redirect:/login?error=locked")
    }

    @Test
    fun `mfaEmailChallenge success`() {
      val user = createSampleUser(username = "someuser")
      whenever(tokenService.getToken(any(), anyString())).thenReturn(
        Optional.of(
          UserToken(
            "otken",
            TokenType.MFA,
            LocalDateTime.now().plusHours(1L),
            user
          )
        )
      )
      whenever(tokenService.createToken(any(), anyString())).thenReturn("a token")
      val modelAndView = controller.mfaChallengeAccountDetail(
        "some token",
        "pass token",
        MfaPreferenceType.EMAIL,
        "some code",
        "email",
        request,
        response
      )
      assertThat(modelAndView!!.viewName).isEqualTo("redirect:/new-email")
      assertThat(modelAndView.model).containsOnly(
        entry("token", "a token")
      )
    }

    @Test
    fun `mfaTextChallenge success`() {
      val user = createSampleUser(username = "someuser")
      whenever(tokenService.getToken(any(), anyString())).thenReturn(
        Optional.of(
          UserToken(
            "otken",
            TokenType.MFA,
            LocalDateTime.now().plusHours(1L),
            user
          )
        )
      )
      whenever(tokenService.createToken(any(), anyString())).thenReturn("a token")
      val modelAndView = controller.mfaChallengeAccountDetail(
        "some token",
        "pass token",
        MfaPreferenceType.TEXT,
        "some code",
        "email",
        request,
        response
      )
      assertThat(modelAndView!!.viewName).isEqualTo("redirect:/new-email")
      assertThat(modelAndView.model).containsOnly(
        entry("token", "a token")
      )
    }

    @Test
    fun `mfaEmailChallenge success telemetry`() {
      val user = createSampleUser(username = "someuser")
      whenever(tokenService.getToken(any(), anyString())).thenReturn(
        Optional.of(
          UserToken(
            "otken",
            TokenType.MFA,
            LocalDateTime.now().plusHours(1L),
            user
          )
        )
      )
      controller.mfaChallengeAccountDetail(
        "some token",
        "pass token",
        MfaPreferenceType.EMAIL,
        "some code",
        "email",
        request,
        response
      )
      verify(telemetryClient).trackEvent("MFAAuthenticateSuccess", mapOf("username" to "someuser"), null)
    }
  }

  @Nested
  inner class MfaResendEmailRequest {
    @Test
    fun `mfaEmailResendRequest check view`() {
      whenever(
        mfaService.buildModelAndViewWithMfaResendOptions(
          anyString(),
          anyString(),
          anyString(),
          eq(MfaPreferenceType.EMAIL),
          anyString()
        )
      ).thenReturn(
        ModelAndView("mfaResend")
      )
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      val modelAndView = controller.mfaResendRequest("email", "some token", "pass token", MfaPreferenceType.EMAIL)
      assertThat(modelAndView.viewName).isEqualTo("mfaResend")
    }

    @Test
    fun `mfaTextResendRequest check view`() {
      whenever(
        mfaService.buildModelAndViewWithMfaResendOptions(
          anyString(),
          anyString(),
          anyString(),
          eq(MfaPreferenceType.TEXT),
          anyString()
        )
      ).thenReturn(
        ModelAndView("mfaResend")
      )
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      val modelAndView = controller.mfaResendRequest("email", "some token", "pass token", MfaPreferenceType.TEXT)
      assertThat(modelAndView.viewName).isEqualTo("mfaResend")
    }

    @Test
    fun `mfaEmailResendRequest check model`() {
      whenever(
        mfaService.buildModelAndViewWithMfaResendOptions(
          anyString(),
          anyString(),
          anyString(),
          eq(MfaPreferenceType.EMAIL),
          anyString()
        )
      )
        .thenReturn(
          ModelAndView("mfaResend", "token", "some token").addObject(
            "mfaPreference",
            MfaPreferenceType.EMAIL
          )
        )
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      val modelAndView = controller.mfaResendRequest("email", "some token", "pass token", MfaPreferenceType.EMAIL)
      assertThat(modelAndView.model).containsExactly(
        entry("token", "some token"),
        entry("mfaPreference", MfaPreferenceType.EMAIL)
      )
    }

    @Test
    fun `mfaTextResendRequest check model`() {
      whenever(
        mfaService.buildModelAndViewWithMfaResendOptions(
          anyString(),
          anyString(),
          anyString(),
          eq(MfaPreferenceType.TEXT),
          anyString()
        )
      )
        .thenReturn(ModelAndView("mfaResend", "token", "some token").addObject("mfaPreference", MfaPreferenceType.TEXT))
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      val modelAndView = controller.mfaResendRequest("email", "some token", "pass token", MfaPreferenceType.TEXT)
      assertThat(modelAndView.model).containsExactly(
        entry("token", "some token"),
        entry("mfaPreference", MfaPreferenceType.TEXT)
      )
    }

    @Test
    fun `mfaEmailResendRequest check service call`() {
      whenever(
        mfaService.buildModelAndViewWithMfaResendOptions(
          anyString(),
          anyString(),
          anyString(),
          eq(MfaPreferenceType.EMAIL),
          anyString()
        )
      ).thenReturn(
        ModelAndView("mfaResend")
      )
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      controller.mfaResendRequest("email", "some token", "pass token", MfaPreferenceType.EMAIL)
      verify(tokenService).checkToken(TokenType.MFA, "some token")
    }

    @Test
    fun `mfaTextResendRequest check service call`() {
      whenever(
        mfaService.buildModelAndViewWithMfaResendOptions(
          anyString(),
          anyString(),
          anyString(),
          eq(MfaPreferenceType.TEXT),
          anyString()
        )
      ).thenReturn(
        ModelAndView("mfaResend")
      )
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      controller.mfaResendRequest("email", "some token", "pass token", MfaPreferenceType.TEXT)
      verify(tokenService).checkToken(TokenType.MFA, "some token")
    }

    @Test
    fun `mfaResendEmailRequest error`() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
      val modelAndView = controller.mfaResendRequest("email", "some token", "pass token", MfaPreferenceType.EMAIL)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/account-detail?error=mfainvalid")
    }
  }

  @Test
  fun `mfaResendTextRequest error`() {
    val user = createSampleUser(
      email = "auth.user@digital.justice.gov.uk",
      mobile = "07700900321",
      mfaPreference = MfaPreferenceType.TEXT
    )
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
    whenever(tokenService.getUserFromToken(any(), anyString())).thenReturn(user)
    val modelAndView = controller.mfaResendRequest("email", "some token", "pass token", MfaPreferenceType.TEXT)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/account-detail?error=mfainvalid")
  }

  @Test
  fun `mfaResendEmail token invalid`() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
    val modelAndView =
      controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.EMAIL, request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/account-detail?error=mfainvalid")
  }

  @Test
  fun `mfaResendText token invalid`() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
    val modelAndView =
      controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.TEXT, request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/account-detail?error=mfainvalid")
  }

  @Test
  fun `mfaResendEmail no code found`() {
    val modelAndView =
      controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.EMAIL, request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/account-detail?error=mfainvalid")
  }

  @Test
  fun `mfaResendText no code found`() {
    val modelAndView =
      controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.TEXT, request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/account-detail?error=mfainvalid")
  }

  @Test
  fun `mfaResendEmail check view`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.EMAIL))).thenReturn("code")
    val modelAndView =
      controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.EMAIL, request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/account/mfa-challenge")
  }

  @Test
  fun `mfaResendText check view`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.TEXT))).thenReturn("code")
    val modelAndView =
      controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.TEXT, request, response)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/account/mfa-challenge")
  }

  @Test
  fun `mfaResendEmail check model`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.EMAIL))).thenReturn("code")
    val modelAndView =
      controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.EMAIL, request, response)
    assertThat(modelAndView.model).containsExactly(
      entry("token", "some token"),
      entry("contactType", "email"),
      entry("mfaPreference", MfaPreferenceType.EMAIL),
      entry("passToken", "pass token")
    )
  }

  @Test
  fun `mfaResendText check model`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.TEXT))).thenReturn("code")
    val modelAndView =
      controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.TEXT, request, response)
    assertThat(modelAndView.model).containsExactly(
      entry("token", "some token"),
      entry("contactType", "email"),
      entry("mfaPreference", MfaPreferenceType.TEXT),
      entry("passToken", "pass token")
    )
  }

  @Test
  fun `mfaResendEmail check view smoke test enabled`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.EMAIL))).thenReturn("code")
    val modelAndView =
      controllerSmokeTestEnabled.mfaResend(
        "email",
        "some token",
        "pass token",
        MfaPreferenceType.EMAIL,
        request,
        response
      )
    assertThat(modelAndView.viewName).isEqualTo("redirect:/account/mfa-challenge")
  }

  @Test
  fun `mfaResendText check view smoke test enabled`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.TEXT))).thenReturn("code")
    val modelAndView =
      controllerSmokeTestEnabled.mfaResend(
        "email",
        "some token",
        "pass token",
        MfaPreferenceType.TEXT,
        request,
        response
      )
    assertThat(modelAndView.viewName).isEqualTo("redirect:/account/mfa-challenge")
  }

  @Test
  fun `mfaResendEmail check model smoke test enabled`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.EMAIL))).thenReturn("code")
    val modelAndView =
      controllerSmokeTestEnabled.mfaResend(
        "email",
        "some token",
        "pass token",
        MfaPreferenceType.EMAIL,
        request,
        response
      )
    assertThat(modelAndView.model).containsExactly(
      entry("token", "some token"),
      entry("contactType", "email"),
      entry("mfaPreference", MfaPreferenceType.EMAIL),
      entry("passToken", "pass token"),
      entry("smokeCode", "code")

    )
  }

  @Test
  fun `mfaResendText check model smoke test enabled`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.TEXT))).thenReturn("code")
    val modelAndView =
      controllerSmokeTestEnabled.mfaResend(
        "email",
        "some token",
        "pass token",
        MfaPreferenceType.TEXT,
        request,
        response
      )
    assertThat(modelAndView.model).containsExactly(
      entry("token", "some token"),
      entry("contactType", "email"),
      entry("mfaPreference", MfaPreferenceType.TEXT),
      entry("passToken", "pass token"),
      entry("smokeCode", "code")
    )
  }

  @Test
  fun `mfaResendEmail check service call`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.EMAIL))).thenReturn("code")
    controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.EMAIL, request, response)
    verify(mfaService).resendMfaCode("some token", MfaPreferenceType.EMAIL)
  }

  @Test
  fun `mfaResendText check service call`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.TEXT))).thenReturn("code")
    controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.TEXT, request, response)
    verify(mfaService).resendMfaCode("some token", MfaPreferenceType.TEXT)
  }
}
