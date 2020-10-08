package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordService
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordServiceImpl.NotificationClientRuntimeException
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordServiceImpl.ResetPasswordException
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException
import uk.gov.service.notify.NotificationClientException
import java.util.Map.entry
import java.util.Optional
import javax.servlet.http.HttpServletRequest

class ResetPasswordControllerTest {
  private val resetPasswordService: ResetPasswordService = mock()
  private val tokenService: TokenService = mock()
  private val userService: UserService = mock()
  private val verifyEmailService: VerifyEmailService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val request: HttpServletRequest = mock()
  private val controller = ResetPasswordController(
    resetPasswordService,
    tokenService,
    userService,
    verifyEmailService,
    telemetryClient,
    true,
    setOf("password1")
  )

  @Nested
  inner class ResetPasswordSuccess {
    @Test
    fun resetPasswordSuccess() {
      assertThat(controller.resetPasswordSuccess()).isEqualTo("resetPasswordSuccess")
    }
  }

  @Nested
  inner class ResetPasswordRequest {
    @Test
    fun resetPasswordRequest() {
      assertThat(controller.resetPasswordRequest()).isEqualTo("resetPassword")
    }

    @Test
    fun resetPasswordRequest_missing() {
      val modelAndView = controller.resetPasswordRequest("   ", request)
      assertThat(modelAndView.viewName).isEqualTo("resetPassword")
      assertThat(modelAndView.model).containsExactly(entry("error", "missing"))
    }

    @Test
    fun resetPasswordRequest_successSmokeWithLink() {
      whenever(request.requestURL).thenReturn(StringBuffer("someurl"))
      whenever(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.of("url"))
      val modelAndView = controller.resetPasswordRequest("user", request)
      assertThat(modelAndView.viewName).isEqualTo("resetPasswordSent")
      assertThat(modelAndView.model).containsExactly(entry("resetLink", "url"))
    }

    @Test
    fun resetPasswordRequest_successSmokeNoLink() {
      whenever(request.requestURL).thenReturn(StringBuffer("someurl"))
      whenever(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.empty())
      val modelAndView = controller.resetPasswordRequest("user", request)
      assertThat(modelAndView.viewName).isEqualTo("resetPasswordSent")
      assertThat(modelAndView.model).containsExactly(entry("resetLinkMissing", true))
    }

    @Test
    fun resetPasswordRequest_successNoLinkTelemetry() {
      whenever(request.requestURL).thenReturn(StringBuffer("someurl"))
      whenever(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.empty())
      controller.resetPasswordRequest("user", request)
      verify(telemetryClient).trackEvent(
        eq("ResetPasswordRequestFailure"),
        check {
          assertThat(it).containsOnly(entry("username", "user"), entry("error", "nolink"))
        },
        isNull()
      )
    }

    @Test
    fun resetPasswordRequest_successVerifyServiceCall() {
      whenever(request.requestURL).thenReturn(StringBuffer("someurl"))
      whenever(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.empty())
      controller.resetPasswordRequest("user", request)
      verify(resetPasswordService).requestResetPassword("user", "someurl")
    }

    @Test
    fun resetPasswordRequest_successLinkTelemetry() {
      whenever(request.requestURL).thenReturn(StringBuffer("someurl"))
      whenever(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.of("somelink"))
      controller.resetPasswordRequest("user", request)
      verify(telemetryClient).trackEvent(
        eq("ResetPasswordRequestSuccess"),
        check {
          assertThat(it).containsOnly(entry("username", "user"))
        },
        isNull()
      )
    }

    @Test
    fun resetPasswordRequest_success() {
      whenever(request.requestURL).thenReturn(StringBuffer("someurl"))
      whenever(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.empty())
      val modelAndView = ResetPasswordController(
        resetPasswordService,
        tokenService,
        userService,
        verifyEmailService,
        telemetryClient,
        false,
        null
      ).resetPasswordRequest("user", request)
      assertThat(modelAndView.viewName).isEqualTo("resetPasswordSent")
      assertThat(modelAndView.model).isEmpty()
    }

    @Test
    fun resetPasswordRequest_failed() {
      whenever(request.requestURL).thenReturn(StringBuffer("someurl"))
      whenever(resetPasswordService.requestResetPassword(anyString(), anyString())).thenThrow(
        NotificationClientRuntimeException(NotificationClientException("failure message"))
      )
      val modelAndView = controller.resetPasswordRequest("user", request)
      assertThat(modelAndView.viewName).isEqualTo("resetPassword")
      assertThat(modelAndView.model).containsExactly(entry("error", "other"))
    }

    @Test
    fun resetPasswordRequest_emailfailed() {
      doThrow(VerifyEmailException("reason")).whenever(verifyEmailService)
        .validateEmailAddressExcludingGsi(anyString(), eq(User.EmailType.PRIMARY))
      val modelAndView = controller.resetPasswordRequest("user@somewhere", request)
      assertThat(modelAndView.viewName).isEqualTo("resetPassword")
      assertThat(modelAndView.model).containsOnly(
        entry("error", "email.reason"),
        entry("usernameOrEmail", "user@somewhere")
      )
    }

    @Test
    fun resetPasswordRequest_emailhelperapostrophe() {
      doThrow(VerifyEmailException("reason")).whenever(verifyEmailService)
        .validateEmailAddressExcludingGsi(anyString(), eq(User.EmailType.PRIMARY))
      val modelAndView = controller.resetPasswordRequest("us.o’er@someWHERE.com   ", request)
      assertThat(modelAndView.viewName).isEqualTo("resetPassword")
      verify(verifyEmailService).validateEmailAddressExcludingGsi("us.o'er@somewhere.com", User.EmailType.PRIMARY)
    }
  }

  @Nested
  inner class ResetPasswordConfirm {
    @Test
    fun resetPasswordConfirm_checkView() {
      setupGetUserCallForProfile()
      setupCheckAndGetTokenValid()
      val modelAndView = controller.resetPasswordConfirm("token")
      assertThat(modelAndView.viewName).isEqualTo("setPassword")
    }

    @Test
    fun resetPasswordConfirm_checkModel() {
      setupGetUserCallForProfile()
      setupCheckAndGetTokenValid()
      val modelAndView = controller.resetPasswordConfirm("sometoken")
      assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
        mapOf(
          "token" to "sometoken",
          "isAdmin" to false,
          "username" to "someuser"
        )
      )
    }

    @Test
    fun resetPasswordConfirm_checkModelAdminUser() {
      val user = setupGetUserCallForProfile()
      user.accountDetail.profile = "TAG_ADMIN"
      setupCheckAndGetTokenValid()
      val modelAndView = controller.resetPasswordConfirm("sometoken")
      assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
        mapOf(
          "token" to "sometoken",
          "isAdmin" to true,
          "username" to "someuser"
        )
      )
    }

    @Test
    fun resetPasswordConfirm_FailureCheckView() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
      val modelAndView = controller.resetPasswordConfirm("token")
      assertThat(modelAndView.viewName).isEqualTo("resetPassword")
    }

    @Test
    fun resetPasswordConfirm_FailureCheckModel() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"))
      val modelAndView = controller.resetPasswordConfirm("sometoken")
      assertThat(modelAndView.model).containsOnly(entry("error", "expired"))
    }
  }

  @Nested
  inner class SetPassword {
    @Test
    fun setPassword_Success() {
      setupCheckAndGetTokenValid()
      setupGetUserCallForProfile()
      val modelAndView = controller.setPassword("d", "password123456", "password123456", null)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/reset-password-success")
    }

    @Test
    fun setPassword_Failure() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"))
      val modelAndView = controller.setPassword("sometoken", "new", "confirm", null)
      assertThat(modelAndView.viewName).isEqualTo("resetPassword")
      assertThat(modelAndView.model).containsOnly(entry("error", "expired"))
    }

    @Test
    fun setPassword_SuccessWithContext() {
      setupCheckAndGetTokenValid()
      setupGetUserCallForProfile()
      val modelAndView = controller.setPassword("d", "password123456", "password123456", true)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/initial-password-success")
    }

    @Test
    fun setPassword_FailureWithContext() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"))
      val modelAndView = controller.setPassword("sometoken", "new", "confirm", true)
      assertThat(modelAndView.viewName).isEqualTo("resetPassword")
      assertThat(modelAndView.model).containsOnly(entry("error", "expired"), entry("initial", true))
    }
  }

  @Nested
  inner class ResetPasswordSelect {
    @Test
    fun setPasswordSelect_checkView() {
      setupCheckTokenValid()
      val modelAndView = controller.resetPasswordSelect("token")
      assertThat(modelAndView.viewName).isEqualTo("setPasswordSelect")
    }

    @Test
    fun setPasswordSelect_checkModel() {
      setupCheckTokenValid()
      val modelAndView = controller.resetPasswordSelect("sometoken")
      assertThat(modelAndView.model).containsOnly(entry("token", "sometoken"))
    }

    @Test
    fun setPasswordSelect_tokenInvalid_checkView() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"))
      val modelAndView = controller.resetPasswordSelect("sometoken")
      assertThat(modelAndView.viewName).isEqualTo("resetPassword")
    }

    @Test
    fun setPasswordSelect_tokenInvalid_checkModel() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"))
      val modelAndView = controller.resetPasswordSelect("sometoken")
      assertThat(modelAndView.model).containsOnly(entry("error", "expired"))
    }
  }

  @Nested
  inner class ResetPasswordChosen {
    @Test
    fun setPasswordChosen_tokenInvalid_checkView() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"))
      val modelAndView = controller.resetPasswordChosen("sometoken", "user")
      assertThat(modelAndView.viewName).isEqualTo("resetPassword")
    }

    @Test
    fun setPasswordChosen_tokenInvalid_checkModel() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"))
      val modelAndView = controller.resetPasswordChosen("sometoken", "user")
      assertThat(modelAndView.model).containsOnly(entry("error", "expired"))
    }

    @Test
    fun setPasswordChosen_checkView() {
      setupGetUserCallForProfile()
      setupGetToken()
      whenever(resetPasswordService.moveTokenToAccount(anyString(), anyString())).thenReturn("token")
      val modelAndView = controller.resetPasswordChosen("sometoken", "user")
      assertThat(modelAndView.viewName).isEqualTo("setPassword")
    }

    @Test
    fun setPasswordChosen_checkModel() {
      setupGetUserCallForProfile()
      setupGetToken()
      whenever(resetPasswordService.moveTokenToAccount(anyString(), anyString())).thenReturn("token")
      val modelAndView = controller.resetPasswordChosen("sometoken", "user")
      assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
        mapOf(
          "token" to "token",
          "isAdmin" to false,
          "username" to "someuser"
        )
      )
    }

    @Test
    fun setPasswordChosen_validationFailure_checkView() {
      setupGetUserCallForProfile()
      setupCheckAndGetTokenValid()
      whenever(
        resetPasswordService.moveTokenToAccount(
          anyString(),
          anyString()
        )
      ).thenThrow(ResetPasswordException("reason"))
      val modelAndView = controller.resetPasswordChosen("sometoken", "user")
      assertThat(modelAndView.viewName).isEqualTo("setPasswordSelect")
    }

    @Test
    fun setPasswordChosen_validationFailure_checkModel() {
      setupGetUserCallForProfile()
      setupCheckAndGetTokenValid()
      whenever(
        resetPasswordService.moveTokenToAccount(
          anyString(),
          anyString()
        )
      ).thenThrow(ResetPasswordException("reason"))
      val modelAndView = controller.resetPasswordChosen("sometoken", "someuser")
      assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
        mapOf(
          "token" to "sometoken",
          "error" to "reason",
          "username" to "someuser"
        )
      )
    }
  }

  private fun setupCheckTokenValid() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
  }

  private fun setupCheckAndGetTokenValid() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
    val user = User.builder().username("someuser").email("email@somewhere.com").verified(true).build()
    whenever(
      tokenService.getToken(
        any(),
        anyString()
      )
    ).thenReturn(Optional.of(user.createToken(UserToken.TokenType.RESET)))
  }

  private fun setupGetToken() {
    val user = User.builder().username("someuser").email("email@somewhere.com").verified(true).build()
    whenever(
      tokenService.getToken(
        any(),
        anyString()
      )
    ).thenReturn(Optional.of(user.createToken(UserToken.TokenType.RESET)))
  }

  private fun setupGetUserCallForProfile(): NomisUserPersonDetails {
    val user = NomisUserPersonDetails()
    user.accountDetail = AccountDetail()
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
    return user
  }
}
