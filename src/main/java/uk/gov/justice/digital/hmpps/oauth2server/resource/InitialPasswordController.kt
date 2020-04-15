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
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import javax.servlet.http.HttpServletRequest

@Slf4j
@Controller
@Validated
class InitialPasswordController(private val resetPasswordService: ResetPasswordService,
                                private val tokenService: TokenService, userService: UserService,
                                private val telemetryClient: TelemetryClient,
                                @Value("\${application.authentication.blacklist}") passwordBlacklist: Set<String?>?) :
    AbstractPasswordController(resetPasswordService, tokenService, userService, telemetryClient, "resetPassword", "initialPassword", passwordBlacklist) {


  @GetMapping("/initial-password-success")
  fun initialPasswordSuccess(): String = "initialPasswordSuccess"

  @GetMapping("/initial-password", "/initial-password-expired-confirm")
  fun initialPassword(@RequestParam token: String?, request: HttpServletRequest): ModelAndView {
    val optionalErrorCode = tokenService.checkToken(UserToken.TokenType.RESET, token!!)

    return optionalErrorCode.map {
      if (it == "expired") {

        ModelAndView("redirect:/initial-password-expired", "token", token)
      } else {
        ModelAndView("redirect:/reset-password")
      }
    }
        .orElseGet {
          createModelWithTokenUsernameAndIsAdmin(UserToken.TokenType.RESET, token, "initialPassword")
              .addObject("initial", true)
        }
  }

  @GetMapping("/initial-password-expired")
  fun initialPasswordLinkExpired(@RequestParam token: String, request: HttpServletRequest): ModelAndView {
    val user = tokenService.getUserFromToken(UserToken.TokenType.RESET, token)
    val newToken = resetPasswordService.requestResetPassword(user.username, request.requestURL.toString())
    return ModelAndView("createPasswordExpired", "link", newToken.get())
  }
}

