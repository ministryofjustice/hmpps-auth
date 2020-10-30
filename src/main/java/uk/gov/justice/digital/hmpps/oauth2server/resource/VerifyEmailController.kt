package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.EmailType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException
import uk.gov.service.notify.NotificationClientException
import java.io.IOException
import java.security.Principal
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
@Validated
class VerifyEmailController(
  private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler,
  private val verifyEmailService: VerifyEmailService,
  private val userService: UserService,
  private val tokenService: TokenService,
  private val telemetryClient: TelemetryClient,
  @param:Value("\${application.smoketest.enabled}") private val smokeTestEnabled: Boolean
) {

  companion object {
    private val log = LoggerFactory.getLogger(VerifyMobileController::class.java)
  }

  @GetMapping("/verify-email")
  @Throws(IOException::class, ServletException::class)
  fun verifyEmailRequest(
    principal: Principal,
    request: HttpServletRequest,
    response: HttpServletResponse,
    @RequestParam(required = false) error: String?
  ): ModelAndView? {
    val modelAndView = ModelAndView("verifyEmail")
    if (StringUtils.isNotBlank(error)) {
      modelAndView.addObject("error", error)
    }

    // Firstly check to see if they have an email address
    val username = principal.name
    val optionalEmail = verifyEmailService.getEmail(username)
    if (optionalEmail.isPresent) {
      val email = optionalEmail.get()
      if (email.isVerified) {
        // no work to do here, so forward on
        proceedToOriginalUrl(request, response)
        return null
      }
      // need to re-verify the email address
      modelAndView.addObject("suggestion", email.email)
      return modelAndView
    }

    // retrieve email addresses that are currently in use
    val existingEmailAddresses = verifyEmailService.getExistingEmailAddressesForUsername(username)
    modelAndView.addObject("candidates", existingEmailAddresses)
    return modelAndView
  }

  @GetMapping("/verify-email-continue")
  @Throws(ServletException::class, IOException::class)
  fun verifyEmailContinue(request: HttpServletRequest, response: HttpServletResponse) {
    proceedToOriginalUrl(request, response)
  }

  @GetMapping("/verify-email-skip")
  @Throws(ServletException::class, IOException::class)
  fun verifyEmailSkip(request: HttpServletRequest, response: HttpServletResponse) {
    telemetryClient.trackEvent("VerifyEmailRequestSkip", emptyMap(), null)
    proceedToOriginalUrl(request, response)
  }

  @PostMapping("/verify-email")
  @Throws(IOException::class, ServletException::class)
  fun verifyEmail(
    @RequestParam(required = false) candidate: String,
    @RequestParam email: String?,
    @RequestParam emailType: EmailType,
    principal: Principal,
    request: HttpServletRequest,
    response: HttpServletResponse
  ): ModelAndView? {
    val username = principal.name

    // candidate will either be an email address from the selection or 'other' meaning free text
    if (StringUtils.isEmpty(candidate)) {
      return verifyEmailRequest(principal, request, response, "noselection")
    }
    val chosenEmail =
      StringUtils.trim(if (StringUtils.isBlank(candidate) || "other" == candidate || "change" == candidate) email else candidate)
    return if (userService.isSameAsCurrentVerifiedEmail(username, chosenEmail, emailType)) {
      ModelAndView("redirect:/verify-email-already", "emailType", emailType)
    } else try {
      val confirmUrl = if (emailType == EmailType.PRIMARY) "-confirm?token=" else "-secondary-confirm?token="
      val verifyLink =
        requestVerificationForUser(username, chosenEmail, request.requestURL.append(confirmUrl).toString(), emailType)
      val modelAndView = ModelAndView("verifyEmailSent", "emailType", emailType)
      if (smokeTestEnabled) {
        modelAndView.addObject("verifyLink", verifyLink)
      }
      modelAndView.addObject("email", chosenEmail)
      modelAndView
    } catch (e: VerifyEmailException) {
      log.info("Validation failed for email address due to {}", e.reason)
      telemetryClient.trackEvent("VerifyEmailRequestFailure", mapOf("username" to username, "reason" to e.reason), null)
      createChangeOrVerifyEmailError(chosenEmail, e.reason, candidate, emailType)
    } catch (e: NotificationClientException) {
      log.error("Failed to send email due to", e)
      createChangeOrVerifyEmailError(chosenEmail, "other", candidate, emailType)
    }
  }

  @GetMapping("/verify-email-already")
  fun EmailAlreadyVerified(): String {
    return "verifyEmailAlready"
  }

  @GetMapping("/backup-email-resend")
  fun secondaryEmailResendRequest(principal: Principal): String {
    val secondaryEmailVerified = verifyEmailService.secondaryEmailVerified(principal.name)
    return if (secondaryEmailVerified) "redirect:/verify-email-already" else "redirect:/verify-secondary-email-resend"
  }

  @GetMapping("/verify-secondary-email-resend")
  fun verifySecondaryEmailResend(): String {
    return "verifySecondaryEmailResend"
  }

  @PostMapping("/verify-secondary-email-resend")
  fun secondaryEmailResend(principal: Principal, request: HttpServletRequest): ModelAndView {
    val username = principal.name
    val originalUrl = request.requestURL.toString()
    val url = originalUrl.replace("verify-secondary-email-resend", "verify-email-secondary-confirm?token=")
    return try {
      val verifyCode = verifyEmailService.resendVerificationCodeSecondaryEmail(username, url)
      redirectToVerifyEmailWithVerifyCode(verifyCode.orElseThrow())
    } catch (e: VerifyEmailException) {
      log.info("Validation failed for email address due to {}", e.reason)
      telemetryClient.trackEvent("VerifyEmailRequestFailure", mapOf("username" to username, "reason" to e.reason), null)
      createChangeOrVerifyEmailError(null, e.reason, "change", EmailType.SECONDARY)
    } catch (e: NotificationClientException) {
      log.error("Failed to send email due to", e)
      createChangeOrVerifyEmailError(null, "other", "change", EmailType.SECONDARY)
    }
  }

  @GetMapping("/verify-email-sent")
  fun verifyEmailSent(): String {
    return "verifyEmailSent"
  }

  @GetMapping("/verify-email-failure")
  fun verifyEmailfailure(): String {
    return "verifyEmailFailure"
  }

  private fun redirectToVerifyEmailWithVerifyCode(verifyLink: String): ModelAndView {
    val modelAndView = ModelAndView("redirect:/verify-email-sent")
    if (smokeTestEnabled) {
      modelAndView.addObject("verifyLink", verifyLink)
    }
    return modelAndView
  }

  @Throws(NotificationClientException::class, VerifyEmailException::class)
  private fun requestVerificationForUser(
    username: String,
    emailInput: String,
    url: String,
    emailType: EmailType
  ): String {
    val userPersonDetails = userService.findMasterUserPersonDetails(username).orElseThrow()
    val firstName = userPersonDetails.firstName
    val fullName = userPersonDetails.name
    return verifyEmailService.requestVerification(username, emailInput, firstName, fullName, url, emailType)
  }

  private fun createChangeOrVerifyEmailError(
    chosenEmail: String?,
    reason: String?,
    type: String,
    emailType: EmailType
  ): ModelAndView {
    return when (emailType) {
      EmailType.PRIMARY -> {
        val view = if (StringUtils.equals(type, "change")) "changeEmail" else "verifyEmail"
        ModelAndView(view)
          .addObject("email", chosenEmail)
          .addObject("error", reason)
      }
      EmailType.SECONDARY ->
        ModelAndView("redirect:/new-backup-email")
          .addObject("email", chosenEmail)
          .addObject("error", reason)
    }
  }

  @Throws(ServletException::class, IOException::class)
  private fun proceedToOriginalUrl(request: HttpServletRequest, response: HttpServletResponse) {
    jwtAuthenticationSuccessHandler.proceed(request, response, SecurityContextHolder.getContext().authentication)
  }

  @GetMapping("/verify-email-confirm")
  fun verifyEmailConfirm(@RequestParam token: String?): ModelAndView {
    val errorOptional = verifyEmailService.confirmEmail(token!!)
    if (errorOptional.isPresent) {
      val error = errorOptional.get()
      VerifyEmailController.log.info("Failed to verify email due to: {}", error)
      return if (StringUtils.equals(error, "expired")) ModelAndView(
        "redirect:/verify-email-expired",
        "token",
        token
      ) else ModelAndView("redirect:/verify-email-failure")
    }
    return ModelAndView("verifyEmailSuccess", "emailType", "PRIMARY")
  }

  @GetMapping("/verify-email-expired")
  @Throws(VerifyEmailException::class, NotificationClientException::class)
  fun verifyEmailLinkExpired(@RequestParam token: String?, request: HttpServletRequest): ModelAndView {
    val user = tokenService.getUserFromToken(UserToken.TokenType.VERIFIED, token!!)
    val originalUrl = request.requestURL.toString()
    val url = originalUrl.replace("expired", "confirm?token=")
    val verifyLink = verifyEmailService.resendVerificationCodeEmail(user.username, url)
    val modelAndView = ModelAndView("verifyEmailExpired")
    modelAndView.addObject("email", user.maskedEmail)
    if (smokeTestEnabled) modelAndView.addObject("link", verifyLink.orElseThrow())
    return modelAndView
  }

  @GetMapping("/verify-email-secondary-confirm")
  fun verifySecondaryEmailConfirm(@RequestParam token: String?): ModelAndView {
    val errorOptional = verifyEmailService.confirmSecondaryEmail(token!!)
    if (errorOptional.isPresent) {
      val error = errorOptional.get()
      log.info("Failed to verify secondary email due to: {}", error)
      return if (StringUtils.equals(error, "expired")) ModelAndView(
        "redirect:/verify-email-secondary-expired",
        "token",
        token
      ) else ModelAndView("redirect:/verify-email-failure")
    }
    return ModelAndView("verifyEmailSuccess", "emailType", "SECONDARY")
  }

  @GetMapping("/verify-email-secondary-expired")
  @Throws(VerifyEmailException::class, NotificationClientException::class)
  fun verifySecondaryEmailLinkExpired(@RequestParam token: String?, request: HttpServletRequest): ModelAndView {
    val user = tokenService.getUserFromToken(UserToken.TokenType.SECONDARY, token!!)
    val originalUrl = request.requestURL.toString()
    val url = originalUrl.replace("expired", "confirm?token=")
    val verifyCode = verifyEmailService.resendVerificationCodeSecondaryEmail(user.username, url)
    val modelAndView = ModelAndView("verifyEmailExpired")
    modelAndView.addObject("email", verifyEmailService.maskedSecondaryEmailFromUsername(user.username))
    if (smokeTestEnabled) modelAndView.addObject("link", verifyCode.orElseThrow())
    return modelAndView
  }
}
