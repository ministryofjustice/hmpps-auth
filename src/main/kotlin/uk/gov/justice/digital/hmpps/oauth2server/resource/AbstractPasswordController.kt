package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.LockedException
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.security.PasswordValidationFailureException
import uk.gov.justice.digital.hmpps.oauth2server.security.ReusedPasswordException
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.PasswordService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import java.util.Optional

open class AbstractPasswordController(
  private val passwordService: PasswordService,
  private val tokenService: TokenService,
  private val userService: UserService,
  private val telemetryClient: TelemetryClient,
  private val startAgainViewOrUrl: String,
  private val failureViewName: String,
  private val passwordBlacklist: Set<String>,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createModelWithTokenUsernameAndIsAdmin(
    tokenType: UserToken.TokenType,
    token: String,
    viewName: String
  ): ModelAndView {
    val userToken = tokenService.getToken(tokenType, token)
    val modelAndView = ModelAndView(viewName, "token", token)
    addUsernameAndIsAdminToModel(userToken.orElseThrow(), modelAndView)
    return modelAndView
  }

  fun processSetPassword(
    tokenType: UserToken.TokenType,
    metricsPrefix: String,
    token: String,
    newPassword: String?,
    confirmPassword: String?
  ): Optional<ModelAndView> {
    val userTokenOptional = tokenService.checkToken(tokenType, token)
    if (userTokenOptional.isPresent) {
      val modelAndView: ModelAndView = if (startAgainViewOrUrl.startsWith("redirect")) {
        ModelAndView(String.format(startAgainViewOrUrl, userTokenOptional.get()))
      } else {
        ModelAndView(startAgainViewOrUrl, "error", userTokenOptional.get())
      }
      return Optional.of(modelAndView)
    }
    // token checked already by service, so can just get it here
    val userToken = tokenService.getToken(tokenType, token).orElseThrow()
    val username = userToken.user.username
    val validationResult = validate(username, newPassword, confirmPassword)
    if (!validationResult.isEmpty()) {
      val modelAndView = ModelAndView(failureViewName, "token", token)
      addUsernameAndIsAdminToModel(userToken, modelAndView)
      return trackAndReturn(tokenType, username, modelAndView, validationResult)
    }
    try {
      passwordService.setPassword(token, newPassword)
    } catch (e: Exception) {
      val modelAndView = ModelAndView(failureViewName, "token", token)
      addUsernameAndIsAdminToModel(userToken, modelAndView)
      if (e is PasswordValidationFailureException) {
        return trackAndReturn(tokenType, username, modelAndView, "validation")
      }
      if (e is ReusedPasswordException) {
        return trackAndReturn(tokenType, username, modelAndView, "reused")
      }
      if (e is LockedException) {
        return trackAndReturn(tokenType, username, modelAndView, "state")
      }
      // let any other exception bubble up
      log.info("Failed to ${tokenType.description} due to ${e.javaClass.name}", e)
      telemetryClient.trackEvent(
        "${tokenType.description}Failure",
        mapOf("username" to username, "reason" to e.javaClass.simpleName),
        null
      )
      throw e
    }
    log.info("Successfully changed password for {}", username)
    telemetryClient.trackEvent(
      "${metricsPrefix}PasswordSuccess",
      mapOf("username" to username),
      null
    )
    return Optional.empty()
  }

  private fun validate(username: String, newPassword: String?, confirmPassword: String?): MultiValueMap<String, Any?> {
    val builder = LinkedMultiValueMap<String, Any?>()

    if (newPassword.isNullOrBlank() || confirmPassword.isNullOrBlank()) {

      if (newPassword.isNullOrBlank()) {
        builder.add("errornew", "newmissing")
      }
      if (confirmPassword.isNullOrBlank()) {
        builder.add("errorconfirm", "confirmmissing")
      }

      // Bomb out now as either new password or confirm new password is missing
      return builder
    }

    // user must be present in order for authenticate to work above
    val user = userService.findMasterUserPersonDetails(username).orElseThrow()

    // Ensuring alphanumeric will ensure that we can't get SQL Injection attacks - since for oracle the password
    // cannot be used in a prepared statement
    if (!StringUtils.isAlphanumeric(newPassword)) {
      builder.add("errornew", "alphanumeric")
    }
    val digits = StringUtils.getDigits(newPassword)
    if (digits.isEmpty()) {
      builder.add("errornew", "nodigits")
    }
    if (digits.length == newPassword.length) {
      builder.add("errornew", "alldigits")
    }
    if (passwordBlacklist.contains(newPassword.toLowerCase())) {
      builder.add("errornew", "blacklist")
    }
    if (StringUtils.containsIgnoreCase(newPassword, username)) {
      builder.add("errornew", "username")
    }
    if (newPassword.chars().distinct().count() < 4) {
      builder.add("errornew", "four")
    }
    if (!StringUtils.equals(newPassword, confirmPassword)) {
      builder.add("errorconfirm", "mismatch")
    }
    if (user.isAdmin) {
      if (newPassword.length < 14) {
        builder.add("errornew", "length14")
      }
    } else if (newPassword.length < 9) {
      builder.add("errornew", "length9")
    }
    if (newPassword.length > 30) {
      builder.add("errornew", "long")
    }
    return builder
  }

  private fun trackAndReturn(
    tokenType: UserToken.TokenType,
    username: String,
    modelAndView: ModelAndView,
    validationResult: MultiValueMap<String, Any?>
  ): Optional<ModelAndView> {
    log.info("Failed to ${tokenType.description}  due to $validationResult ")
    telemetryClient.trackEvent(
      "${tokenType.description}Failure",
      mapOf("username" to username, "reason" to validationResult.toString()),
      null
    )
    modelAndView.addAllObjects(validationResult)
    modelAndView.addObject("error", true)
    return Optional.of(modelAndView)
  }

  private fun trackAndReturn(
    tokenType: UserToken.TokenType,
    username: String,
    modelAndView: ModelAndView,
    reason: String
  ): Optional<ModelAndView> {
    log.info("Failed to ${tokenType.description} due to $reason")
    telemetryClient.trackEvent(
      "${tokenType.description}Failure",
      mapOf("username" to username, "reason" to reason),
      null
    )
    modelAndView.addObject("errornew", reason)
    modelAndView.addObject("error", true)
    return Optional.of(modelAndView)
  }

  private fun addUsernameAndIsAdminToModel(userToken: UserToken, modelAndView: ModelAndView) {
    val username = userToken.user.username
    modelAndView.addObject("username", username)
    val isAdmin = userService.findMasterUserPersonDetails(username).orElseThrow().isAdmin
    modelAndView.addObject("isAdmin", isAdmin)
  }
}
