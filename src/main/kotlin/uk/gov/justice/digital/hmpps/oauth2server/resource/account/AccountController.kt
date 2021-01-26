package uk.gov.justice.digital.hmpps.oauth2server.resource.account

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.util.Base64Utils
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.service.UserContextService
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Suppress("DEPRECATION")
@Controller
class AccountController(
  private val userService: UserService,
  private val userContextService: UserContextService,
  private val backLinkHandler: BackLinkHandler,
) {

  @GetMapping("/account-details")
  fun accountDetails(
    @RequestParam(required = false, name = "redirect_uri") redirectUri: String?,
    @RequestParam(required = false, name = "client_id") client: String?,
    authentication: Authentication,
    request: HttpServletRequest,
    response: HttpServletResponse,
    @CookieValue(value = "returnTo", defaultValue = "Lw==") returnToFromCookie: String
  ):
    ModelAndView {
      val username = authentication.name
      val user = userService.findMasterUserPersonDetails(username).orElseThrow()
      val userInAuth = userService.getUserWithContacts(username)
      val linkedAccounts = userContextService.discoverUsers(user)
        .map { LinkedAccountModel(it.authSource.toUpperCase(), it.username) }

      val email = userInAuth.email
      val canSwitchUsernameToEmail = userInAuth.source == AuthSource.auth && email != null &&
        !user.username.contains('@') && userService.findUser(email).isEmpty

      val usernameNotEmail = email != username.toLowerCase()

      val redirectOk: Boolean = if (client != null && redirectUri != null) {
        backLinkHandler.validateRedirect(client, redirectUri)
      } else false

      val returnToUri =
        when {
          redirectUri.isNullOrEmpty() -> String(Base64Utils.decodeFromString(returnToFromCookie))
          redirectOk -> redirectUri
          else -> "/"
        }

      addReturnCookie(returnToUri, request, response)

      return ModelAndView("account/accountDetails")
        .addObject("returnTo", returnToUri)
        .addObject("user", user)
        .addObject("authUser", userInAuth)
        .addObject("mfaPreferenceVerified", userInAuth.mfaPreferenceVerified())
        .addObject("linkedAccounts", linkedAccounts)
        .addObject("canSwitchUsernameToEmail", canSwitchUsernameToEmail)
        .addObject("usernameNotEmail", usernameNotEmail)
    }

  private fun addReturnCookie(
    returnToUrl: String,
    request: HttpServletRequest,
    response: HttpServletResponse
  ) {
    val returnToUrlBase64 = Base64Utils.encodeToString(returnToUrl.toByteArray())
    val returnCookie = Cookie("returnTo", returnToUrlBase64)
    returnCookie.isHttpOnly = true
    returnCookie.path = "/auth/account-details"
    returnCookie.secure = request.isSecure
    response.addCookie(returnCookie)
  }
}

data class LinkedAccountModel(
  val systemName: String,
  val username: String,
)
