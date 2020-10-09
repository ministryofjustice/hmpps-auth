package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.InitialPasswordService
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import javax.servlet.http.HttpServletRequest

@Slf4j
@Controller
@Validated
class InitialPasswordController(
  resetPasswordService: ResetPasswordService,
  private val initialPasswordService: InitialPasswordService,
  private val tokenService: TokenService,
  userService: UserService,
  telemetryClient: TelemetryClient,
  @Value("\${application.authentication.blacklist}") passwordBlacklist: Set<String?>?,
  @Value("\${application.smoketest.enabled}") private val smokeTestEnabled: Boolean,
) :
  AbstractPasswordController(
    resetPasswordService,
    tokenService,
    userService,
    telemetryClient,
    "resetPassword",
    "setPassword",
    passwordBlacklist
  ) {

  @GetMapping("/initial-password-success")
  fun initialPasswordSuccess(): String = "initialPasswordSuccess"

  @GetMapping("/initial-password")
  fun initialPassword(@RequestParam token: String?, request: HttpServletRequest): ModelAndView {
    if (token.isNullOrBlank()) return ModelAndView("redirect:/reset-password")

    val optionalErrorCode = tokenService.checkToken(UserToken.TokenType.RESET, token)

    return optionalErrorCode.map {
      if (it == "expired") {

        ModelAndView("redirect:/initial-password-expired", "token", token)
      } else {
        ModelAndView("redirect:/reset-password")
      }
    }
      .orElseGet {
        createModelWithTokenUsernameAndIsAdmin(UserToken.TokenType.RESET, token, "setPassword")
          .addObject("initial", true)
      }
  }

  @GetMapping("/initial-password-expired")
  fun initialPasswordLinkExpired(@RequestParam token: String, request: HttpServletRequest): ModelAndView {
    val user = tokenService.getUserFromToken(UserToken.TokenType.RESET, token)
    val newToken = initialPasswordService.resendInitialPasswordLink(user.username, request.requestURL.toString())
    val modelAndView = ModelAndView("createPasswordExpired")
    if (smokeTestEnabled) modelAndView.addObject("link", newToken)
    return modelAndView
  }
}
