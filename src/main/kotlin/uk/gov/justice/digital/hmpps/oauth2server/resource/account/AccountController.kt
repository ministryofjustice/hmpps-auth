package uk.gov.justice.digital.hmpps.oauth2server.resource.account

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.service.UserContextService

@Controller
class AccountController(private val userService: UserService, private val userContextService: UserContextService) {
  @GetMapping("/account-details")
  fun accountDetails(authentication: Authentication): ModelAndView {
    val username = authentication.name
    val user = userService.findMasterUserPersonDetails(username).orElseThrow()
    val userInAuth = userService.getUserWithContacts(username)
    val linkedAccounts = userContextService.discoverUsers(user)
      .map { LinkedAccountModel(it.authSource.toUpperCase(), it.username) }

    val email = userInAuth.email
    val canSwitchUsernameToEmail = userInAuth.source == AuthSource.auth && email != null &&
      !user.username.contains('@') && userService.findUser(email).isEmpty

    return ModelAndView("account/accountDetails")
      .addObject("user", user)
      .addObject("authUser", userInAuth)
      .addObject("mfaPreferenceVerified", userInAuth.mfaPreferenceVerified())
      .addObject("linkedAccounts", linkedAccounts)
      .addObject("canSwitchUsernameToEmail", canSwitchUsernameToEmail)
  }
}

data class LinkedAccountModel(
  val systemName: String,
  val username: String,
)
