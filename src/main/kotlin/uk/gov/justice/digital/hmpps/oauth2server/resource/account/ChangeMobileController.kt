package uk.gov.justice.digital.hmpps.oauth2server.resource.account

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyMobileService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyMobileService.VerifyMobileException
import uk.gov.service.notify.NotificationClientException

@Controller
class ChangeMobileController(
  private val userService: UserService,
  private val verifyMobileService: VerifyMobileService,
  private val tokenService: TokenService,
  private val telemetryClient: TelemetryClient,
  @Value("\${application.smoketest.enabled}") private val smokeTestEnabled: Boolean,
) {

  @GetMapping("/new-mobile")
  fun changeMobileRequest(@RequestParam token: String, authentication: Authentication): ModelAndView {
    val optionalErrorForToken = tokenService.checkToken(UserToken.TokenType.ACCOUNT, token)
    if (optionalErrorForToken.isPresent) {
      return ModelAndView("redirect:/account-details?error=token${optionalErrorForToken.get()}")
    }
    val currentMobile = userService.getUserWithContacts(authentication.name).mobile
    val requestType = if (currentMobile.isNullOrEmpty()) "add" else "Change"
    return ModelAndView("account/changeMobile", "mobile", currentMobile)
      .addObject("requestType", requestType)
      .addObject("token", token)
  }

  @PostMapping("/new-mobile")
  fun changeMobile(
    @RequestParam token: String,
    @RequestParam mobile: String?,
    requestType: String,
    authentication: Authentication
  ): ModelAndView {
    val optionalErrorForToken = tokenService.checkToken(UserToken.TokenType.ACCOUNT, token)
    if (optionalErrorForToken.isPresent) {
      return ModelAndView("redirect:/account-details?error=mobiletoken${optionalErrorForToken.get()}")
    }
    val username = authentication.name
    if (userService.isSameAsCurrentVerifiedMobile(username, mobile)) {
      return ModelAndView("redirect:/verify-mobile-already")
    }
    return try {
      val verifyCode = verifyMobileService.changeMobileAndRequestVerification(username, mobile)
      redirectToVerifyMobileWithVerifyCode(verifyCode)
    } catch (e: VerifyMobileException) {
      log.info("Validation failed for mobile phone number due to {}", e.reason)
      telemetryClient.trackEvent(
        "VerifyMobileRequestFailure",
        mapOf("username" to username, "reason" to e.reason),
        null
      )

      createChangeOrVerifyMobileError(e.reason, mobile, requestType)
    } catch (e: NotificationClientException) {
      log.error("Failed to send sms due to", e)
      telemetryClient.trackEvent(
        "VerifyMobileRequestFailure",
        mapOf("username" to username, "reason" to "notify"),
        null
      )

      createChangeOrVerifyMobileError("other", mobile, requestType)
    }
  }

  private fun createChangeOrVerifyMobileError(
    reason: String,
    currentMobile: String?,
    requestType: String,
  ): ModelAndView =
    ModelAndView("account/changeMobile")
      .addObject("error", reason)
      .addObject("mobile", currentMobile)
      .addObject("requestType", requestType)

  private fun redirectToVerifyMobileWithVerifyCode(verifyCode: String): ModelAndView {
    val modelAndView = ModelAndView("redirect:/verify-mobile")
    if (smokeTestEnabled) {
      modelAndView.addObject("verifyCode", verifyCode)
    }
    return modelAndView
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
