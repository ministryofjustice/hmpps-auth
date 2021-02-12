package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.MapEntry.entry
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyMobileService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyMobileService.VerifyMobileException
import uk.gov.service.notify.NotificationClientException
import java.util.Optional

class VerifyMobileControllerTest {
  private val verifyMobileService: VerifyMobileService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val controller = VerifyMobileController(verifyMobileService, telemetryClient, false)
  private val controllerSmokeEnabled = VerifyMobileController(verifyMobileService, telemetryClient, true)
  private val principal = UsernamePasswordAuthenticationToken("user", "pass")

  @Nested
  inner class VerifyMobile {
    @Test
    fun `verifyMobile success`() {
      val view = controller.verifyMobile()
      assertThat(view).isEqualTo("verifyMobileSent")
    }
  }

  @Nested
  inner class VerifyMobileAlready {
    @Test
    fun `verifyMobileAlready success`() {
      val view = controller.verifyMobileAlready()
      assertThat(view).isEqualTo("verifyMobileAlready")
    }
  }

  @Nested
  inner class VerifyMobileConfirm {
    @Test
    fun verifyMobileConfirm() {
      whenever(verifyMobileService.confirmMobile(anyString())).thenReturn(Optional.empty())
      val modelAndView = controller.verifyMobileConfirm("token")
      assertThat(modelAndView.viewName).isEqualTo("verifyMobileSuccess")
      assertThat(modelAndView.model).isEmpty()
    }

    @Test
    fun verifyMobileConfirm_Failure() {
      whenever(verifyMobileService.confirmMobile(anyString())).thenReturn(Optional.of(mapOf("error" to "failed")))
      val modelAndView = controller.verifyMobileConfirm("token")
      assertThat(modelAndView.viewName).isEqualTo("verifyMobileSent")
      assertThat(modelAndView.model).containsExactly(entry("error", "failed"))
    }

    @Test
    fun verifyMobileConfirm_Expired() {
      whenever(verifyMobileService.confirmMobile(anyString())).thenReturn(
        Optional.of(
          mapOf(
            "error" to "expired",
            "verifyCode" to "123456"
          )
        )
      )
      val modelAndView = controller.verifyMobileConfirm("token")
      assertThat(modelAndView.viewName).isEqualTo("verifyMobileSent")
      assertThat(modelAndView.model).containsExactly(entry("error", "expired"))
    }

    @Test
    fun `verifyMobileConfirm expired smoke enabled`() {
      whenever(verifyMobileService.confirmMobile(anyString())).thenReturn(
        Optional.of(
          mapOf(
            "error" to "expired",
            "verifyCode" to "123456"
          )
        )
      )
      val modelAndView = controllerSmokeEnabled.verifyMobileConfirm("token")
      assertThat(modelAndView.viewName).isEqualTo("verifyMobileSent")
      assertThat(modelAndView.model).containsExactly(entry("error", "expired"), entry("verifyCode", "123456"))
    }

    @Test
    fun verifyMobileConfirm_Invalid() {
      whenever(verifyMobileService.confirmMobile(anyString())).thenReturn(Optional.of(mapOf("error" to "invalid")))
      val modelAndView = controller.verifyMobileConfirm("token")
      assertThat(modelAndView.viewName).isEqualTo("verifyMobileSent")
      assertThat(modelAndView.model).containsExactly(entry("error", "invalid"))
    }

    @Test
    fun `verifyMobileConfirm invalid smoke test enabled`() {
      whenever(verifyMobileService.confirmMobile(anyString())).thenReturn(Optional.of(mapOf("error" to "invalid")))
      val modelAndView = controllerSmokeEnabled.verifyMobileConfirm("token")
      assertThat(modelAndView.viewName).isEqualTo("verifyMobileSent")
      assertThat(modelAndView.model).containsExactly(entry("error", "invalid"), entry("verifyCode", null))
    }
  }

  @Nested
  inner class MobileResendRequest {
    @Test
    fun mobileResendRequest_notVerified() {
      whenever(verifyMobileService.mobileVerified(anyString())).thenReturn(false)
      val view = controller.mobileResendRequest(principal)
      assertThat(view).isEqualTo("verifyMobileResend")
    }

    @Test
    fun mobileResendRequest_alreadyVerified() {
      whenever(verifyMobileService.mobileVerified(anyString())).thenReturn(true)
      val view = controller.mobileResendRequest(principal)
      assertThat(view).isEqualTo("redirect:/verify-mobile-already")
    }
  }

  @Nested
  inner class MobileResend {
    @Test
    fun mobileResend_sendCode() {
      whenever(verifyMobileService.resendVerificationCode(anyString())).thenReturn(Optional.of("123456"))
      val modelAndView = controller.mobileResend(principal)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/verify-mobile")
      assertThat(modelAndView.model).isEmpty()
    }

    @Test
    fun `mobileResend send code smoke enabled`() {
      whenever(verifyMobileService.resendVerificationCode(anyString())).thenReturn(Optional.of("123456"))
      val modelAndView = controllerSmokeEnabled.mobileResend(principal)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/verify-mobile")
      assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(mapOf("verifyCode" to "123456"))
    }

    @Test
    fun mobileResend_verifyMobileException() {
      whenever(verifyMobileService.resendVerificationCode(anyString())).thenThrow(VerifyMobileException("reason"))
      val modelAndView = controller.mobileResend(principal)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/new-mobile")
      assertThat(modelAndView.model).containsExactly(entry("error", "reason"))
      verify(telemetryClient).trackEvent(
        eq("VerifyMobileRequestFailure"),
        check {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(mapOf("username" to "user", "reason" to "reason"))
        },
        isNull()
      )
    }

    @Test
    fun mobileResend_notificationClientException() {
      whenever(verifyMobileService.resendVerificationCode(anyString())).thenThrow(NotificationClientException("something went wrong"))
      val modelAndView = controller.mobileResend(principal)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/new-mobile")
      assertThat(modelAndView.model).containsExactly(entry("error", "other"))
    }
  }
}
