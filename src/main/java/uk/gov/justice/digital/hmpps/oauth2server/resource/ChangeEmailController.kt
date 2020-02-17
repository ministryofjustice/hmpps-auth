package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService

@Controller
class ChangeEmailController(private val tokenService: TokenService, private val userService: UserService) {

  @GetMapping("/new-email")
  fun newEmailRequest(@RequestParam token: String): ModelAndView {
    val userToken = tokenService.getToken(UserToken.TokenType.CHANGE, token)
    val modelAndView = ModelAndView("changeEmail", "token", token)
    addUsernameAndIsAdminToModel(userToken.orElseThrow(), modelAndView)
    return modelAndView
  }

  private fun addUsernameAndIsAdminToModel(userToken: UserToken, modelAndView: ModelAndView) {
    val username = userToken.user.username
    modelAndView.addObject("username", username)
    val user = userService.findMasterUserPersonDetails(username).orElseThrow()
    val isAdmin = user.isAdmin
    val source = user.authSource
    modelAndView.addObject("isAdmin", isAdmin).addObject("source", source)
  }
}

