package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.security.ChangePasswordService
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Slf4j
@Controller
class ChangePasswordController(private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler,
                               private val authenticationManager: AuthenticationManager,
                               changePasswordService: ChangePasswordService?,
                               private val tokenService: TokenService, userService: UserService,
                               private val telemetryClient: TelemetryClient,
                               @Value("\${application.authentication.blacklist}") passwordBlacklist: Set<String>) :
    AbstractPasswordController(changePasswordService, tokenService, userService, telemetryClient, "redirect:/login?error=%s", "changePassword", passwordBlacklist) {

  @GetMapping("/change-password")
  fun changePasswordRequest(@RequestParam token: String?) =
      createModelWithTokenAndAddIsAdmin(UserToken.TokenType.CHANGE, token, "changePassword")
          .addObject("expired", true)

  @GetMapping("/new-password")
  fun newPasswordRequest(@RequestParam token: String?) =
      createModelWithTokenAndAddIsAdmin(UserToken.TokenType.CHANGE, token, "changePassword")

  @PostMapping("/change-password")
  fun changePassword(@RequestParam token: String,
                     @RequestParam newPassword: String?, @RequestParam confirmPassword: String?,
                     request: HttpServletRequest?, response: HttpServletResponse?,
                     @RequestParam expired: Boolean?): ModelAndView? {
    val userToken = tokenService.getToken(UserToken.TokenType.CHANGE, token)
    val modelAndView = processSetPassword(UserToken.TokenType.CHANGE, "Change", token, newPassword, confirmPassword)
    if (modelAndView.isPresent) {
      return modelAndView.get().addObject("expired", expired)
    }
    // will be error if unable to get token here as set password process has been successful
    val username = userToken.orElseThrow().user.username
    // authentication with new password
    return try {
      val successToken = authenticate(username, newPassword!!)
      // success, so forward on
      telemetryClient.trackEvent("ChangePasswordAuthenticateSuccess", mapOf("username" to username), null)
      jwtAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, successToken)
      // return here is not required, since the success handler will have redirected
      null
    } catch (e: AuthenticationException) {
      val reason = e.javaClass.simpleName
      log.info("Caught unexpected {} after change password", reason, e)
      telemetryClient.trackEvent("ChangePasswordAuthenticateFailure", mapOf("username" to username, "reason" to reason), null)
      // this should have succeeded, but unable to login
      // need to tell user that the change password request has been successful though
      ModelAndView("redirect:/login?error=changepassword")
    }
  }

  private fun authenticate(username: String, password: String) =
      authenticationManager.authenticate(UsernamePasswordAuthenticationToken(username.toUpperCase(), password))

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
