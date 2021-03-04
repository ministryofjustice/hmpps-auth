@file:Suppress("SpringMVCViewInspection")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.EmailType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.LinkAndEmail
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException
import uk.gov.service.notify.NotificationClientException
import java.security.Principal
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
      if (email.verified) {
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
  fun verifyEmailContinue(request: HttpServletRequest, response: HttpServletResponse) {
    proceedToOriginalUrl(request, response)
  }

  @GetMapping("/verify-email-skip")
  fun verifyEmailSkip(request: HttpServletRequest, response: HttpServletResponse) {
    telemetryClient.trackEvent("VerifyEmailRequestSkip", emptyMap(), null)
    proceedToOriginalUrl(request, response)
  }

  @PostMapping("/verify-email")
  fun verifyEmail(
    @RequestParam(required = false) candidate: String,
    @RequestParam email: String?,
    @RequestParam emailType: EmailType,
    @RequestParam token: String?,
    @RequestParam resend: Boolean,
    principal: Principal,
    request: HttpServletRequest,
    response: HttpServletResponse
  ): ModelAndView? {
    val username = principal.name

    if (!resend) {
      if (token.isNullOrEmpty()) return ModelAndView("redirect:/account-details?error=tokeninvalid")
      val optionalErrorForToken = tokenService.checkToken(TokenType.ACCOUNT, token)
      if (optionalErrorForToken.isPresent) {
        return ModelAndView("redirect:/account-details?error=token${optionalErrorForToken.get()}")
      }
    }

    // candidate will either be an email address from the selection or 'other' meaning free text
    if (StringUtils.isEmpty(candidate)) {
      return verifyEmailRequest(principal, request, response, "noselection")
    }
    val chosenEmail =
      StringUtils.trim(if (StringUtils.isBlank(candidate) || "other" == candidate || "change" == candidate) email else candidate)
    if (userService.isSameAsCurrentVerifiedEmail(username, chosenEmail, emailType)) {
      removeAccountToken(token)
      return ModelAndView("redirect:/verify-email-already", "emailType", emailType)
    } else return try {
      val confirmUrl = if (emailType == EmailType.PRIMARY) "-confirm?token=" else "-secondary-confirm?token="
      val (verifyLink, newEmail) =
        changeEmailAndRequestVerification(
          username,
          chosenEmail,
          request.requestURL.append(confirmUrl).toString(),
          emailType
        )
      removeAccountToken(token)
      val modelAndView = ModelAndView("verifyEmailSent", "emailType", emailType)
      if (smokeTestEnabled) {
        modelAndView.addObject("verifyLink", verifyLink)
      }
      modelAndView.addObject("email", newEmail)

      // if we have changed the username, then need to update the token otherwise we won't be able to find the user
      // in subsequent requests
      if (username != null && username.contains('@')) {
        val userPersonDetails = userService.findMasterUserPersonDetails(newEmail).orElseThrow()
        val successToken = UsernamePasswordAuthenticationToken(userPersonDetails, null, userPersonDetails.authorities)
        jwtAuthenticationSuccessHandler.updateAuthenticationInRequest(request, response, successToken)
      }
      return modelAndView
    } catch (e: VerifyEmailException) {
      log.info("Validation failed for email address due to {}", e.reason)
      telemetryClient.trackEvent("VerifyEmailRequestFailure", mapOf("username" to username, "reason" to e.reason), null)
      createChangeOrVerifyEmailError(token, chosenEmail, e.reason, candidate, emailType)
    } catch (e: NotificationClientException) {
      log.error("Failed to send email due to", e)
      createChangeOrVerifyEmailError(token, chosenEmail, "other", candidate, emailType)
    }
  }

  @GetMapping("/verify-email-already")
  fun emailAlreadyVerified(): String {
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
  fun secondaryEmailResend(
    @RequestParam token: String?,
    principal: Principal,
    request: HttpServletRequest
  ): ModelAndView {
    val username = principal.name
    val originalUrl = request.requestURL.toString()
    val url = originalUrl.replace("verify-secondary-email-resend", "verify-email-secondary-confirm?token=")
    return try {
      val verifyCode = verifyEmailService.resendVerificationCodeSecondaryEmail(username, url)
      redirectToVerifyEmailWithVerifyCode(verifyCode.orElseThrow())
    } catch (e: VerifyEmailException) {
      log.info("Validation failed for email address due to {}", e.reason)
      telemetryClient.trackEvent("VerifyEmailRequestFailure", mapOf("username" to username, "reason" to e.reason), null)
      createChangeOrVerifyEmailError(token, null, e.reason, "change", EmailType.SECONDARY)
    } catch (e: NotificationClientException) {
      log.error("Failed to send email due to", e)
      createChangeOrVerifyEmailError(token, null, "other", "change", EmailType.SECONDARY)
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

  private fun removeAccountToken(token: String?) {
    if (!token.isNullOrEmpty()) {
      tokenService.removeToken(TokenType.ACCOUNT, token)
    }
  }

  private fun redirectToVerifyEmailWithVerifyCode(verifyLink: String): ModelAndView {
    val modelAndView = ModelAndView("redirect:/verify-email-sent")
    if (smokeTestEnabled) {
      modelAndView.addObject("verifyLink", verifyLink)
    }
    return modelAndView
  }

  private fun changeEmailAndRequestVerification(
    username: String,
    emailInput: String,
    url: String,
    emailType: EmailType
  ): LinkAndEmail {
    val userPersonDetails = userService.findMasterUserPersonDetails(username).orElseThrow()
    val firstName = userPersonDetails.firstName
    val fullName = userPersonDetails.name
    return verifyEmailService.changeEmailAndRequestVerification(
      username,
      emailInput,
      firstName,
      fullName,
      url,
      emailType
    )
  }

  private fun createChangeOrVerifyEmailError(
    token: String?,
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
        ModelAndView("redirect:/new-backup-email?token=$token")
          .addObject("email", chosenEmail)
          .addObject("error", reason)
    }
  }

  private fun proceedToOriginalUrl(request: HttpServletRequest, response: HttpServletResponse) {
    jwtAuthenticationSuccessHandler.proceed(request, response, SecurityContextHolder.getContext().authentication)
  }

  @GetMapping("/verify-email-confirm")
  fun verifyEmailConfirm(@RequestParam token: String?): ModelAndView {
    val errorOptional = verifyEmailService.confirmEmail(token!!)
    if (errorOptional.isPresent) {
      val error = errorOptional.get()
      log.info("Failed to verify email due to: {}", error)
      return if (StringUtils.equals(error, "expired")) ModelAndView(
        "redirect:/verify-email-expired",
        "token",
        token
      ) else ModelAndView("redirect:/verify-email-failure")
    }
    return ModelAndView("verifyEmailSuccess", "emailType", "PRIMARY")
  }

  @GetMapping("/verify-email-expired")
  fun verifyEmailLinkExpired(@RequestParam token: String?, request: HttpServletRequest): ModelAndView {
    val user = tokenService.getUserFromToken(TokenType.VERIFIED, token!!)
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
  fun verifySecondaryEmailLinkExpired(@RequestParam token: String?, request: HttpServletRequest): ModelAndView {
    val user = tokenService.getUserFromToken(TokenType.SECONDARY, token!!)
    val originalUrl = request.requestURL.toString()
    val url = originalUrl.replace("expired", "confirm?token=")
    val verifyCode = verifyEmailService.resendVerificationCodeSecondaryEmail(user.username, url)
    val modelAndView = ModelAndView("verifyEmailExpired")
    modelAndView.addObject("email", user.maskedSecondaryEmail)
    if (smokeTestEnabled) modelAndView.addObject("link", verifyCode.orElseThrow())
    return modelAndView
  }
}
