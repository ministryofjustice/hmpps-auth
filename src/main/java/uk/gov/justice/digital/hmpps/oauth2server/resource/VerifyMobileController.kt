package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyMobileService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyMobileService.VerifyMobileException
import uk.gov.service.notify.NotificationClientException
import java.security.Principal
import java.util.Optional

@Controller
@Validated
class VerifyMobileController(
  private val verifyMobileService: VerifyMobileService,
  private val telemetryClient: TelemetryClient,
  @param:Value("\${application.smoketest.enabled}") private val smokeTestEnabled: Boolean
) {

  companion object {
    private val log = LoggerFactory.getLogger(VerifyMobileController::class.java)
  }
  @GetMapping("/verify-mobile")
  fun verifyMobile(): String {
    return "verifyMobileSent"
  }

  @GetMapping("/verify-mobile-already")
  fun verifyMobileAlready(): String {
    return "verifyMobileAlready"
  }

  @PostMapping("/verify-mobile")
  fun verifyMobileConfirm(@RequestParam code: String?): ModelAndView {
    val errorOptional: Optional<Map<String, String>> = verifyMobileService.confirmMobile(
      code!!
    )
    return errorOptional.map { error: Map<String, String> ->
      log.info("Failed to verify mobile phone number due to: {}", error)
      val modelAndView = ModelAndView("verifyMobileSent", "error", error["error"])
      if (smokeTestEnabled) {
        modelAndView.addObject("verifyCode", error["verifyCode"])
      }
      modelAndView
    }.orElse(ModelAndView("verifyMobileSuccess"))
  }

  @GetMapping("/mobile-resend")
  fun mobileResendRequest(principal: Principal): String {
    val mobileVerified = verifyMobileService.mobileVerified(principal.name)
    return if (mobileVerified) "redirect:/verify-mobile-already" else "verifyMobileResend"
  }

  @PostMapping("/verify-mobile-resend")
  fun mobileResend(principal: Principal): ModelAndView {
    val username = principal.name
    return try {
      val verifyCode = verifyMobileService.resendVerificationCode(username)
      redirectToVerifyMobileWithVerifyCode(verifyCode.orElseThrow())
    } catch (e: VerifyMobileException) {
      log.info("Validation failed for mobile phone number due to {}", e.reason)
      telemetryClient.trackEvent(
        "VerifyMobileRequestFailure",
        java.util.Map.of("username", username, "reason", e.reason),
        null
      )
      createChangeOrVerifyMobileError(e.reason)
    } catch (e: NotificationClientException) {
      log.error("Failed to send sms due to", e)
      createChangeOrVerifyMobileError("other")
    }
  }

  private fun createChangeOrVerifyMobileError(reason: String): ModelAndView {
    return ModelAndView("redirect:/change-mobile")
      .addObject("error", reason)
  }

  private fun redirectToVerifyMobileWithVerifyCode(verifyCode: String): ModelAndView {
    val modelAndView = ModelAndView("redirect:/verify-mobile")
    if (smokeTestEnabled) {
      modelAndView.addObject("verifyCode", verifyCode)
    }
    return modelAndView
  }
}
