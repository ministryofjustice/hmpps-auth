package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.LockedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.security.DeliusAuthenticationServiceException
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider.MfaRequiredException
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService

@Controller
class ExistingPasswordController(private val authenticationManager: AuthenticationManager,
                                 private val tokenService: TokenService,
                                 private val telemetryClient: TelemetryClient) {

  @GetMapping("/existing-email")
  fun existingPasswordRequestEmail(authentication: Authentication): ModelAndView = createModelAndViewWithUsername(authentication).addObject("type", "email")

  @GetMapping("/existing-password")
  fun existingPasswordRequestPassword(authentication: Authentication): ModelAndView = createModelAndViewWithUsername(authentication).addObject("type", "password")

  @PostMapping("/existing-password")
  fun existingPassword(@RequestParam password: String?, @RequestParam type: String, authentication: Authentication): ModelAndView {
    if (password.isNullOrBlank()) return createModelAndViewWithUsername(authentication)
        .addObject("error", "required")
        .addObject("type", type)

    val username = authentication.name
    return try {
      authenticate(username, password)
      continueToNewEmailOrPassword(username, type)

    } catch (e: MfaRequiredException) {
      // they'll have already provided their MFA credentials to login, so just allow password here
      continueToNewEmailOrPassword(username, type)

    } catch (e: AuthenticationException) {
      val reason = e.javaClass.simpleName
      log.info("Caught {} during change password", reason, e)
      telemetryClient.trackEvent("ExistingPasswordAuthenticateFailure", mapOf("username" to username, "reason" to reason), null)
      when (e::class) {
        DeliusAuthenticationServiceException::class -> createModelAndViewWithUsername(authentication).addObject("error", listOf("invalid", "deliusdown")).addObject("type", type)
        BadCredentialsException::class -> createModelAndViewWithUsername(authentication).addObject("error", "invalid").addObject("type", type)
        LockedException::class -> ModelAndView("redirect:/logout", "error", "locked")
        else -> ModelAndView("redirect:/logout", "error", "invalid")
      }
    }
  }

  private fun continueToNewEmailOrPassword(username: String, type: String): ModelAndView {
    // successfully logged in with credentials, so generate change password token
    val token = tokenService.createToken(TokenType.CHANGE, username)

    @Suppress("SpringMVCViewInspection")
    return ModelAndView("redirect:/new-$type", "token", token)

  }

  private fun createModelAndViewWithUsername(authentication: Authentication) =
      ModelAndView("user/existingPassword", "username", authentication.name)

  private fun authenticate(username: String, password: String) =
      authenticationManager.authenticate(UsernamePasswordAuthenticationToken(username.toUpperCase(), password))

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
