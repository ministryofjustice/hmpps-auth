package uk.gov.justice.digital.hmpps.oauth2server.resource.account

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.service.UserContextService

@Controller
class AccountController(private val userService: UserService, private val userContextService: UserContextService) {
  @GetMapping("/account-details")
  fun accountDetails(authentication: Authentication): ModelAndView {
    val username = authentication.name
    val user = userService.findMasterUserPersonDetails(username).orElseThrow()
    val authUser = userService.getUserWithContacts(username)
    val linkedAccounts = userContextService.discoverUsers(user, emptySet())
      .map {
        LinkedAccountModel(it.authSource.toUpperCase(), it.username)
      }

    return ModelAndView("account/accountDetails")
      .addObject("user", user)
      .addObject("authUser", authUser)
      .addObject("mfaPreferenceVerified", authUser.mfaPreferenceVerified())
      .addObject("linkedAccounts", linkedAccounts)
  }
}

data class LinkedAccountModel(
  val systemName: String,
  val username: String,
)
