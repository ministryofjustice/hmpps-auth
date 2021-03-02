package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
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
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.MfaPreferenceType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider.MfaUnavailableException
import uk.gov.justice.digital.hmpps.oauth2server.service.LoginFlowException
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaData
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaFlowException
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import java.time.LocalDateTime
import java.util.Optional
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Suppress("ClassName")
class MfaControllerAccountDetailsTest {
  private val tokenService: TokenService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val mfaService: MfaService = mock()
  private val request: HttpServletRequest = MockHttpServletRequest()
  private val response: HttpServletResponse = MockHttpServletResponse()
  private val controller =
    MfaControllerAccountDetails(tokenService, telemetryClient, mfaService, false)
  private val controllerSmokeTestEnabled =
    MfaControllerAccountDetails(tokenService, telemetryClient, mfaService, true)
  private val authentication = UsernamePasswordAuthenticationToken("bob", "pass")

  @Nested
  inner class mfaChallengeRequestAccountDetail {
    @Test
    fun `mfaChallengeRequest check view`() {
      whenever(mfaService.createTokenAndSendMfaCode(anyString())).thenReturn(
        MfaData("token", "code", MfaPreferenceType.EMAIL)
      )
      whenever(mfaService.getCodeDestination(anyString(), any())).thenReturn("")
      val modelAndView = controller.mfaChallengeRequestAccountDetail(authentication, "TEXT", null)
      assertThat(modelAndView.viewName).isEqualTo("mfaChallengeAccountDetails")
    }

    @Test
    fun `mfaChallengeRequest check model email`() {
      whenever(mfaService.createTokenAndSendMfaCode(anyString())).thenReturn(
        MfaData("some token", "code", MfaPreferenceType.EMAIL)
      )
      whenever(mfaService.getCodeDestination(any(), eq(MfaPreferenceType.EMAIL))).thenReturn("auth******@******.gov.uk")
      val modelAndView =
        controller.mfaChallengeRequestAccountDetail(authentication, "email", "password token")
      assertThat(modelAndView.model).containsOnly(
        entry("mfaPreference", MfaPreferenceType.EMAIL),
        entry("codeDestination", "auth******@******.gov.uk"),
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
        "expired token"
      )
      assertThat(view.viewName).isEqualTo("redirect:/account-details")
      assertThat(view.model).containsOnly(entry("error", "tokenexpired"))
    }

    @Test
    fun `Primary email mfaChallenge Request invalid password token`() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))

      val view = controller.mfaChallengeRequestAccountDetail(
        authentication,
        "email",
        "invalid token"
      )
      assertThat(view.viewName).isEqualTo("redirect:/account-details")
      assertThat(view.model).containsOnly(entry("error", "tokeninvalid"))
    }

    @Test
    fun `Primary email mfaChallenge Request without password token returns token invalid error`() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))

      val view = controller.mfaChallengeRequestAccountDetail(
        authentication,
        "email",
        null
      )
      assertThat(view.viewName).isEqualTo("redirect:/account-details")
      assertThat(view.model).containsOnly(entry("error", "tokeninvalid"))
    }

    @Test
    fun `mfaChallengeRequest check model text`() {
      whenever(mfaService.createTokenAndSendMfaCode(anyString())).thenReturn(
        MfaData("some token", "code", MfaPreferenceType.TEXT)
      )
      whenever(mfaService.getCodeDestination(any(), eq(MfaPreferenceType.TEXT))).thenReturn("*******0321")
      val modelAndView =
        controller.mfaChallengeRequestAccountDetail(authentication, "email", "password token")
      assertThat(modelAndView.model).containsOnly(
        entry("mfaPreference", MfaPreferenceType.TEXT),
        entry("codeDestination", "*******0321"),
        entry("token", "some token"),
        entry("passToken", "password token"),
        entry("contactType", "email")
      )
    }

    @Test
    fun `mfaChallengeRequest unavailable`() {
      whenever(mfaService.createTokenAndSendMfaCode(anyString())).thenThrow(MfaUnavailableException("some msg"))
      whenever(mfaService.getCodeDestination(anyString(), any())).thenReturn("")
      val modelAndView = controller.mfaChallengeRequestAccountDetail(authentication, "TEXT", null)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/account-details")
      assertThat(modelAndView.model).containsOnly(entry("error", "mfaunavailable"))
    }
  }

  @Nested
  inner class mfaChallengeRequestAccountDetailError {
    @Test
    fun `mfaChallengeRequest check model contains when error when error in param`() {
      whenever(mfaService.getCodeDestination(any(), eq(MfaPreferenceType.EMAIL))).thenReturn("auth******@******.gov.uk")
      val modelAndView = controller.mfaChallengeRequestAccountDetailError(
        "email",
        "invalid",
        "some token",
        "password token",
        MfaPreferenceType.EMAIL
      )
      assertThat(modelAndView.viewName).isEqualTo("mfaChallengeAccountDetails")
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

      val view = controller.mfaChallengeRequestAccountDetailError(
        "email",
        "some error",
        null,
        "expired token",
        MfaPreferenceType.EMAIL
      )
      assertThat(view.viewName).isEqualTo("redirect:/account-details")
      assertThat(view.model).containsOnly(entry("error", "tokenexpired"))
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
      assertThat(modelAndView!!.viewName).isEqualTo("redirect:/account-details", "error", "mfainvalid")
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
      assertThat(modelAndView!!.viewName).isEqualTo("redirect:/account/mfa-challenge-error")
      assertThat(modelAndView.model).containsOnly(
        entry("token", "some token"),
        entry("passToken", "pass token"),
        entry("error", "invalid"),
        entry("mfaPreference", MfaPreferenceType.EMAIL),
        entry("contactType", "email")
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
      assertThat(modelAndView!!.viewName).isEqualTo("redirect:/account/mfa-challenge-error")
      assertThat(modelAndView.model).containsOnly(
        entry("token", "some token"),
        entry("passToken", "pass token"),
        entry("error", "invalid"),
        entry("mfaPreference", MfaPreferenceType.TEXT),
        entry("contactType", "email")
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
      assertThat(modelAndView!!.viewName).isEqualTo("redirect:/logout")
      assertThat(modelAndView.model).containsOnly(entry("error", "locked"))
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
      assertThat(modelAndView!!.viewName).isEqualTo("redirect:/logout")
      assertThat(modelAndView.model).containsOnly(entry("error", "locked"))
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
  inner class mfaResendRequest {
    @Test
    fun `email preference`() {
      val user = createSampleUser(
        email = "auth.user@digital.justice.gov.uk",
        mobile = "07700900321",
        mfaPreference = MfaPreferenceType.TEXT,
        verified = true
      )
      whenever(tokenService.getUserFromToken(any(), any())).thenReturn(user)
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      val modelAndView = controller.mfaResendRequest("EMAIL", "some token", "pass token", MfaPreferenceType.EMAIL)
      assertThat(modelAndView.viewName).isEqualTo("mfaResendAccountDetails")
      assertThat(modelAndView.model).containsOnly(
        entry("token", "some token"), entry("passToken", "pass token"), entry("mfaPreference", MfaPreferenceType.EMAIL),
        entry("contactType", "EMAIL"), entry("email", "auth******@******.gov.uk")
      )
    }

    @Test
    fun `text preference`() {
      val user = createSampleUser(
        email = "auth.user@digital.justice.gov.uk",
        mobile = "07700900321",
        mfaPreference = MfaPreferenceType.TEXT,
        mobileVerified = true,
      )
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      whenever(tokenService.getUserFromToken(any(), any())).thenReturn(user)
      val modelAndView = controller.mfaResendRequest("EMAIL", "some token", "pass token", MfaPreferenceType.TEXT)
      verify(tokenService).checkToken(TokenType.MFA, "some token")
      assertThat(modelAndView.viewName).isEqualTo("mfaResendAccountDetails")
      assertThat(modelAndView.model).containsOnly(
        entry("token", "some token"), entry("passToken", "pass token"), entry("mfaPreference", MfaPreferenceType.TEXT),
        entry("contactType", "EMAIL"), entry("mobile", "*******0321")
      )
    }

    @Test
    fun `secondary email preference`() {
      val user = createSampleUser(
        secondaryEmail = "auth.user@digital.justice.gov.uk",
        mobile = "07700900321",
        mfaPreference = MfaPreferenceType.TEXT,
        secondaryEmailVerified = true,
      )
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      whenever(tokenService.getUserFromToken(any(), any())).thenReturn(user)
      val modelAndView =
        controller.mfaResendRequest("EMAIL", "some token", "pass token", MfaPreferenceType.SECONDARY_EMAIL)
      verify(tokenService).checkToken(TokenType.MFA, "some token")
      assertThat(modelAndView.viewName).isEqualTo("mfaResendAccountDetails")
      assertThat(modelAndView.model).containsOnly(
        entry("token", "some token"),
        entry("passToken", "pass token"),
        entry("mfaPreference", MfaPreferenceType.SECONDARY_EMAIL),
        entry("contactType", "EMAIL"),
        entry("secondaryemail", "auth******@******.gov.uk")
      )
    }

    @Test
    fun `all verified`() {
      val user = createSampleUser(
        email = "auth.user@digital.justice.gov.uk",
        verified = true,
        secondaryEmail = "secondary@digital.justice.gov.uk",
        secondaryEmailVerified = true,
        mobile = "07700900321",
        mobileVerified = true,
        mfaPreference = MfaPreferenceType.TEXT,
      )
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
      whenever(tokenService.getUserFromToken(any(), any())).thenReturn(user)
      val modelAndView =
        controller.mfaResendRequest("EMAIL", "some token", "pass token", MfaPreferenceType.SECONDARY_EMAIL)
      verify(tokenService).checkToken(TokenType.MFA, "some token")
      assertThat(modelAndView.viewName).isEqualTo("mfaResendAccountDetails")
      assertThat(modelAndView.model).containsOnly(
        entry("token", "some token"),
        entry("passToken", "pass token"),
        entry("mfaPreference", MfaPreferenceType.SECONDARY_EMAIL),
        entry("contactType", "EMAIL"),
        entry("secondaryemail", "seco******@******.gov.uk"),
        entry("email", "auth******@******.gov.uk"),
        entry("mobile", "*******0321")
      )
    }

    @Test
    fun `mfaResendEmailRequest error`() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
      val modelAndView = controller.mfaResendRequest("EMAIL", "some token", "pass token", MfaPreferenceType.EMAIL)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/account-details")
      assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(mapOf("error" to "mfainvalid"))
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
      val modelAndView = controller.mfaResendRequest("EMAIL", "some token", "pass token", MfaPreferenceType.TEXT)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/account-details")
      assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(mapOf("error" to "mfainvalid"))
    }
  }

  @Test
  fun `mfaResendEmail token invalid`() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
    val modelAndView =
      controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.EMAIL)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/account-details")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(mapOf("error" to "mfainvalid"))
  }

  @Test
  fun `mfaResendText token invalid`() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
    val modelAndView =
      controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.TEXT)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/account-details")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(mapOf("error" to "mfainvalid"))
  }

  @Test
  fun `mfaResendEmail no code found`() {
    val modelAndView =
      controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.EMAIL)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/account-details")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(mapOf("error" to "mfainvalid"))
  }

  @Test
  fun `mfaResendText no code found`() {
    val modelAndView =
      controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.TEXT)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/account-details")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(mapOf("error" to "mfainvalid"))
  }

  @Test
  fun `mfaResendEmail check view`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.EMAIL))).thenReturn("code")
    val modelAndView =
      controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.EMAIL)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/account/mfa-challenge")
  }

  @Test
  fun `mfaResendText check view`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.TEXT))).thenReturn("code")
    val modelAndView =
      controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.TEXT)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/account/mfa-challenge")
  }

  @Test
  fun `mfaResendEmail check model`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.EMAIL))).thenReturn("code")
    val modelAndView =
      controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.EMAIL)
    assertThat(modelAndView.model).containsOnly(
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
      controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.TEXT)
    assertThat(modelAndView.model).containsOnly(
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
        MfaPreferenceType.EMAIL
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
        MfaPreferenceType.TEXT
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
        MfaPreferenceType.EMAIL
      )
    assertThat(modelAndView.model).containsOnly(
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
        MfaPreferenceType.TEXT
      )
    assertThat(modelAndView.model).containsOnly(
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
    controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.EMAIL)
    verify(mfaService).resendMfaCode("some token", MfaPreferenceType.EMAIL)
  }

  @Test
  fun `mfaResendText check service call`() {
    whenever(mfaService.resendMfaCode(anyString(), eq(MfaPreferenceType.TEXT))).thenReturn("code")
    controller.mfaResend("email", "some token", "pass token", MfaPreferenceType.TEXT)
    verify(mfaService).resendMfaCode("some token", MfaPreferenceType.TEXT)
  }
}
