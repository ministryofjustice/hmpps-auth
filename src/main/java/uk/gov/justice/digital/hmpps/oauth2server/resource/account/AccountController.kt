package uk.gov.justice.digital.hmpps.oauth2server.resource.account

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService

@Controller
class AccountController(private val userService: UserService) {
  @GetMapping("/account-details")
  fun accountDetails(authentication: Authentication): ModelAndView {
    val username = getUserName(authentication)
    val user = userService.findMasterUserPersonDetails(username).orElseThrow()
    val authUser = userService.findUser(username).orElseThrow()
    return ModelAndView("account/accountDetails")
        .addObject("user", user)
        .addObject("authUser", authUser)
  }

  @GetMapping("/account-details-cancel")
  fun cancel() = "redirect:/"

  private fun getUserName(authentication: Authentication) = (authentication.principal as UserDetailsImpl).username
}
