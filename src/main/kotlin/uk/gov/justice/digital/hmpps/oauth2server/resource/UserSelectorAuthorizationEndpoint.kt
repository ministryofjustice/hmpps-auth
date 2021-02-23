@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.common.util.OAuth2Utils.USER_OAUTH_APPROVAL
import org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.SessionAttributes
import org.springframework.web.bind.support.SessionStatus
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.View
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.UserRetriesService
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService

@Controller
@SessionAttributes(
  "authorizationRequest",
  "org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint.ORIGINAL_AUTHORIZATION_REQUEST"
)
class UserSelectorAuthorizationEndpoint(
  private val authorizationEndpoint: AuthorizationEndpoint,
  private val userService: UserService,
  private val userRetriesService: UserRetriesService,
  private val telemetryClient: TelemetryClient,
) {
  @GetMapping("/oauth/authorize")
  fun authorize(
    model: MutableMap<String, *>,
    @RequestParam parameters: Map<String, String>,
    sessionStatus: SessionStatus,
    authentication: Authentication,
  ): ModelAndView {

    val modelAndView = authorizationEndpoint.authorize(model, parameters, sessionStatus, authentication)
    // breakout for users that don't need authorisation
    val users = modelAndView.model["users"] as? List<*> ?: return modelAndView

    // otherwise processes the user count
    return when (users.size) {
      // no discovered users, continue by explicitly approving the request
      0 -> ModelAndView(
        authorizationEndpoint.approveOrDeny(
          hashMapOf(USER_OAUTH_APPROVAL to "true"), model, sessionStatus, authentication
        ),
        model
      )
      // only one discovered user, pass on to approveOrDeny for further verification
      1 -> with(users[0] as UserPersonDetails) {
        val account = "$authSource/$username"
        ModelAndView(
          approveOrDeny(
            hashMapOf(USER_OAUTH_APPROVAL to account), model, sessionStatus, authentication
          ),
          model
        )
      }
      // take the user to select what user they would like to use
      else -> ModelAndView("userSelector", model)
    }
  }

  @PostMapping(value = ["/oauth/authorize"], params = [USER_OAUTH_APPROVAL])
  fun approveOrDeny(
    @RequestParam approvalParameters: MutableMap<String, String>,
    model: MutableMap<String, *>?,
    sessionStatus: SessionStatus?,
    authentication: Authentication?,
  ): View {
    val account = approvalParameters[USER_OAUTH_APPROVAL]

    val replacedAuthentication = if (!account.isNullOrBlank() && authentication != null) {
      replaceAuthentication(account, authentication, approvalParameters)
    } else authentication
    return authorizationEndpoint.approveOrDeny(approvalParameters, model, sessionStatus, replacedAuthentication)
  }

  private fun replaceAuthentication(
    account: String,
    authentication: Authentication,
    approvalParameters: MutableMap<String, String>
  ): Authentication? {
    val source = account.substringBefore("/")
    val username = account.substringAfter("/")

    val azureUser = authentication.principal as UserDetailsImpl

    val user = userService.getMasterUserPersonDetailsWithEmailCheck(
      username, AuthSource.fromNullableString(source), azureUser.userId
    )
    return user.map { upd ->
      // if we're successful with the replace then change the approval parameter to true
      approvalParameters[USER_OAUTH_APPROVAL] = "true"

      // now replace the principal
      val newAuth: Authentication = UsernamePasswordAuthenticationToken(
        UserDetailsImpl(
          upd.username,
          upd.name,
          upd.authorities,
          source,
          upd.userId,
          azureUser.jwtId
        ),
        authentication.credentials,
        upd.authorities
      )

      userRetriesService.resetRetriesAndRecordLogin(upd)

      telemetryClient.trackEvent(
        "UserForAccessToken",
        mapOf("azureuser" to azureUser.userId, "username" to upd.username, "auth_source" to upd.authSource),
        null
      )

      newAuth
    }.orElse(authentication)
  }
}
