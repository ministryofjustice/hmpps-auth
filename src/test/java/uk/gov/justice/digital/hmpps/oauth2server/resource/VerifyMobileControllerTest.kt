package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.MapEntry.entry
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyMobileService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyMobileService.VerifyMobileException
import uk.gov.service.notify.NotificationClientException
import java.util.*
import java.util.Map
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class VerifyMobileControllerTest {
  private val request: HttpServletRequest = mock()
  private val response: HttpServletResponse = mock()
  private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler = mock()
  private val verifyMobileService: VerifyMobileService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val userService: UserService = mock()
  private val verifyMobileController = VerifyMobileController(jwtAuthenticationSuccessHandler, verifyMobileService, telemetryClient, userService, true)
  private val principal = UsernamePasswordAuthenticationToken("user", "pass")

  @Test
  fun verifyEmailContinue() {
    SecurityContextHolder.getContext().authentication = principal
    verifyMobileController.verifyMobileContinue(request, response)
    verify(jwtAuthenticationSuccessHandler).proceed(request, response, principal)
  }

  @Test
  fun verifyMobile_Exception() {
    whenever(userService.isSameAsCurrentVerifiedMobile(anyString(), anyString())).thenReturn(false)
    whenever(userService.findUser(anyString())).thenReturn(Optional.of(User.of("AUTH_MOBILE")))
    whenever(verifyMobileService.requestVerification(anyString(), anyString())).thenThrow(NotificationClientException("something went wrong"))
    val modelAndView = verifyMobileController.verifyMobile(null, principal)
    assertThat(modelAndView.viewName).isEqualTo("verifyMobileSent")
    assertThat(modelAndView.model).containsExactly(entry("verifyCode", null))
  }

  @Test
  fun verifyMobile_Success() {
    whenever(userService.isSameAsCurrentVerifiedMobile(anyString(), anyString())).thenReturn(false)
    whenever(userService.findUser(anyString())).thenReturn(Optional.of(User.of("AUTH_MOBILE")))
    whenever(verifyMobileService.requestVerification(anyString(), anyString())).thenReturn("123456")
    val mobile = "07700900321"
    val modelAndView = verifyMobileController.verifyMobile(mobile, principal)
    assertThat(modelAndView.viewName).isEqualTo("verifyMobileSent")
    assertThat(modelAndView.model).containsExactly(entry("verifyCode", "123456"))
    verify(verifyMobileService).requestVerification("user", mobile)
  }

  @Test
  fun verifyMobile_Already() {
    whenever(userService.isSameAsCurrentVerifiedMobile(anyString(), anyString())).thenReturn(true)
    val modelAndView = verifyMobileController.verifyMobile("07700900321", principal)
    assertThat(modelAndView.viewName).isEqualTo("verifyMobileAlready")
  }

  @Test
  fun verifyMobileConfirm() {
    whenever(verifyMobileService.confirmMobile(anyString())).thenReturn(Optional.empty())
    val modelAndView = verifyMobileController.verifyMobileConfirm("token")
    assertThat(modelAndView.viewName).isEqualTo("verifyMobileSuccess")
    assertThat(modelAndView.model).isEmpty()
  }

  @Test
  fun verifyMobileConfirm_Failure() {
    whenever(verifyMobileService.confirmMobile(anyString())).thenReturn(Optional.of(mapOf("error" to "failed")))
    val modelAndView = verifyMobileController.verifyMobileConfirm("token")
    assertThat(modelAndView.viewName).isEqualTo("verifyMobileSent")
    assertThat(modelAndView.model).containsExactly(entry("error", "failed"), entry("verifyCode", null))
  }

  @Test
  fun verifyMobileConfirm_Expired() {
    whenever(verifyMobileService.confirmMobile(anyString())).thenReturn(Optional.of(mapOf("error" to "expired", "verifyCode" to "123456")))
    val modelAndView = verifyMobileController.verifyMobileConfirm("token")
    assertThat(modelAndView.viewName).isEqualTo("verifyMobileSent")
    assertThat(modelAndView.model).containsExactly(entry("error", "expired"), entry("verifyCode", "123456"))
  }

  @Test
  fun verifyMobileConfirm_Invalid() {
    whenever(verifyMobileService.confirmMobile(anyString())).thenReturn(Optional.of(mapOf("error" to "invalid")))
    val modelAndView = verifyMobileController.verifyMobileConfirm("token")
    assertThat(modelAndView.viewName).isEqualTo("verifyMobileSent")
    assertThat(modelAndView.model).containsExactly(entry("error", "invalid"), entry("verifyCode", null))
  }

  @Test
  fun mobileResendRequest_notVerified() {
    whenever(verifyMobileService.mobileVerified(anyString())).thenReturn(false)
    val modelAndView = verifyMobileController.mobileResendRequest(principal)
    assertThat(modelAndView.viewName).isEqualTo("verifyMobileResend")
  }

  @Test
  fun mobileResendRequest_alreadyVerified() {
    whenever(verifyMobileService.mobileVerified(anyString())).thenReturn(true)
    val modelAndView = verifyMobileController.mobileResendRequest(principal)
    assertThat(modelAndView.viewName).isEqualTo("verifyMobileAlready")
  }

  @Test
  fun mobileResend_sendCode() {
    whenever(userService.findUser(anyString())).thenReturn(Optional.of(User.builder().mobile("077009000000").build()))
    whenever(verifyMobileService.resendVerificationCode(anyString())).thenReturn(Optional.of("123456"))
    val modelAndView = verifyMobileController.mobileResend(principal)
    assertThat(modelAndView.viewName).isEqualTo("verifyMobileSent")
  }

  @Test
  fun mobileResend_sendCode_smokeTestEnabled() {
    whenever(userService.findUser(anyString())).thenReturn(Optional.of(User.builder().mobile("077009000000").build()))
    whenever(verifyMobileService.resendVerificationCode(anyString())).thenReturn(Optional.of("123456"))
    val modelAndView = VerifyMobileController(jwtAuthenticationSuccessHandler, verifyMobileService, telemetryClient, userService, true)
        .mobileResend(principal)
    assertThat(modelAndView.viewName).isEqualTo("verifyMobileSent")
    assertThat(modelAndView.model).containsExactly(entry("verifyCode", "123456"))
  }

  @Test
  fun mobileResend_verifyMobileException() {
    whenever(userService.findUser(anyString())).thenReturn(Optional.of(User.builder().mobile("077009000000").build()))
    whenever(verifyMobileService.resendVerificationCode(anyString())).thenThrow(VerifyMobileException("reason"))
    val modelAndView = verifyMobileController.mobileResend(principal)
    assertThat(modelAndView.viewName).isEqualTo("changeMobile")
    assertThat(modelAndView.model).containsExactly(entry("error", "reason"), entry("mobile", "077009000000"))
    verify(telemetryClient).trackEvent(eq("VerifyMobileRequestFailure"), check {
      assertThat(it).containsOnly(Map.entry("username", "user"), Map.entry("reason", "reason"))
    }, isNull())
  }

  @Test
  fun mobileResend_notificationClientException() {
    whenever(userService.findUser(anyString())).thenReturn(Optional.of(User.builder().mobile("077009000000").build()))
    whenever(verifyMobileService.resendVerificationCode(anyString())).thenThrow(NotificationClientException("something went wrong"))
    val modelAndView = verifyMobileController.mobileResend(principal)
    assertThat(modelAndView.viewName).isEqualTo("changeMobile")
    assertThat(modelAndView.model).containsExactly(entry("error", "other"), entry("mobile", "077009000000"))
  }

}
