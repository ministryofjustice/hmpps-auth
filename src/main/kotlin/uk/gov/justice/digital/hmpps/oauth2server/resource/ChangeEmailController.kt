package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService

@Controller
class ChangeEmailController(private val tokenService: TokenService, private val userService: UserService) {

  @GetMapping("/new-email")
  fun newEmailRequest(@RequestParam token: String): ModelAndView {
    val userToken = tokenService.getToken(TokenType.ACCOUNT, token)
    val modelAndView = ModelAndView("changeEmail", "token", token)
    addUsernameAndIsAdminToModel(userToken.orElseThrow(), modelAndView)
    return modelAndView
  }

  private fun addUsernameAndIsAdminToModel(userToken: UserToken, modelAndView: ModelAndView) {
    val username = userToken.user.username
    val user = userService.findMasterUserPersonDetails(username).orElseThrow()
    val isAdmin = user.isAdmin
    val currentEmail = userToken.user.email
    val source = user.authSource
    modelAndView.addObject("username", username)
      .addObject("isAdmin", isAdmin)
      .addObject("source", source)
      .addObject("email", currentEmail)

    if (AuthSource.fromNullableString(user.authSource) == AuthSource.auth &&
      user.username.toLowerCase() == currentEmail
    ) {
      modelAndView.addObject("changingUsername", true)
    }
  }

  @GetMapping("/new-backup-email")
  fun newSecondaryEmailRequest(@RequestParam token: String, authentication: Authentication): ModelAndView {
    val optionalErrorForToken = tokenService.checkToken(TokenType.ACCOUNT, token)
    if (optionalErrorForToken.isPresent) {
      return ModelAndView("redirect:/account-details?error=mfa${optionalErrorForToken.get()}")
    }
    val currentSecondaryEmail = userService.getUserWithContacts(authentication.name).secondaryEmail
    return ModelAndView("account/changeBackupEmail", "secondaryEmail", currentSecondaryEmail)
      .addObject("token", token)
  }
}
