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
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService

@Controller
class ExistingPasswordController(private val authenticationManager: AuthenticationManager,
                                 private val tokenService: TokenService,
                                 private val telemetryClient: TelemetryClient) {

  @GetMapping("/existing-password")
  fun existingPasswordRequest(authentication: Authentication) = createModelAndViewWithUsername(authentication)

  @PostMapping("/existing-password")
  fun existingPassword(@RequestParam password: String?, authentication: Authentication): ModelAndView {
    if (password.isNullOrBlank()) return createModelAndViewWithUsername(authentication).addObject("error", "required")

    val username = getUserName(authentication)
    return try {
      authenticate(username, password)
      continueToNewPassword(username)

    } catch (e: MfaRequiredException) {
      // they'll have already provided their MFA credentials to login, so just allow password here
      continueToNewPassword(username)

    } catch (e: AuthenticationException) {
      val reason = e.javaClass.simpleName
      log.info("Caught {} during change password", reason, e)
      telemetryClient.trackEvent("ExistingPasswordAuthenticateFailure", mapOf("username" to username, "reason" to reason), null)
      when (e::class) {
        DeliusAuthenticationServiceException::class -> createModelAndViewWithUsername(authentication).addObject("error", listOf("invalid", "deliusdown"))
        BadCredentialsException::class -> createModelAndViewWithUsername(authentication).addObject("error", "invalid")
        LockedException::class -> ModelAndView("redirect:/logout", "error", "locked")
        else -> ModelAndView("redirect:/logout", "error", "invalid")
      }
    }
  }

  private fun continueToNewPassword(username: String): ModelAndView {
    // successfully logged in with credentials, so generate change password token
    val token = tokenService.createToken(TokenType.CHANGE, username)

    // and take them to existing change passsword pages to continue flow
    return ModelAndView("redirect:/new-password", "token", token)
  }

  private fun createModelAndViewWithUsername(authentication: Authentication) =
      ModelAndView("user/existingPassword").addObject("username", getUserName(authentication))

  private fun authenticate(username: String, password: String) =
      authenticationManager.authenticate(UsernamePasswordAuthenticationToken(username.toUpperCase(), password))

  private fun getUserName(authentication: Authentication) = (authentication.principal as UserDetailsImpl).username

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
