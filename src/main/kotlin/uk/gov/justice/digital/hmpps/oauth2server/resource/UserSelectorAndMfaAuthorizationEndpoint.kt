@file:Suppress("DEPRECATION", "SpringMVCViewInspection")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.common.util.OAuth2Utils.USER_OAUTH_APPROVAL
import org.springframework.security.oauth2.provider.AuthorizationRequest
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
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaClientService

@Controller
@SessionAttributes(
  "authorizationRequest",
  "org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint.ORIGINAL_AUTHORIZATION_REQUEST"
)
class UserSelectorAndMfaAuthorizationEndpoint(
  private val authorizationEndpoint: AuthorizationEndpoint,
  private val userService: UserService,
  private val userRetriesService: UserRetriesService,
  private val telemetryClient: TelemetryClient,
  private val mfaClientService: MfaClientService,
) {
  @GetMapping("/oauth/authorize")
  fun authorize(
    model: MutableMap<String, Any>,
    @RequestParam parameters: Map<String, String>,
    sessionStatus: SessionStatus,
    authentication: Authentication,
  ): ModelAndView {

    val modelAndView = authorizationEndpoint.authorize(model, parameters, sessionStatus, authentication)
    val requireMfa = modelAndView.model["requireMfa"] as? Boolean ?: false
    val users = modelAndView.model["users"] as? List<*>
    // breakout for users that don't need authorisation
    if (users == null && !requireMfa) return modelAndView

    val selectedUser = users?.let {
      when (users.size) {
        // no users found so don't need to do anything?
        0 -> null
        1 -> {
          val user = users[0] as UserPersonDetails
          "${user.authSource}/${user.username}"
        }
        // otherwise take user to userSelector page to select their user first
        // need to go there since if end up doing role based mfa selection then need to map user before selecting role
        else -> return ModelAndView("userSelector", model)
      }
    }

    if (requireMfa) {
      selectedUser?.apply { model["selectedUser"] = selectedUser }
      return ModelAndView("forward:/service-mfa-send-challenge", model)
    }

    return ModelAndView(
      if (selectedUser.isNullOrBlank()) authorizationEndpoint.approveOrDeny(
        hashMapOf(USER_OAUTH_APPROVAL to "true"), model, sessionStatus, authentication
      ) else approveOrDeny(
        hashMapOf(USER_OAUTH_APPROVAL to selectedUser), model, sessionStatus, authentication
      ),
      model
    )
  }

  @PostMapping(value = ["/oauth/authorize"], params = [USER_OAUTH_APPROVAL])
  fun approveOrDeny(
    @RequestParam approvalParameters: MutableMap<String, String>,
    model: MutableMap<String, Any>,
    sessionStatus: SessionStatus,
    authentication: Authentication?,
  ): View {
    val account = approvalParameters[USER_OAUTH_APPROVAL]

    val user = authentication?.principal as UserDetailsImpl?
    val authorizationRequest = model["authorizationRequest"] as AuthorizationRequest?

    val clientNeedsMfa = mfaClientService.clientNeedsMfa(authorizationRequest?.clientId)
    val mfaPassed = user?.passedMfa == true

    val replacedAuthentication = if ((clientNeedsMfa && mfaPassed) || !clientNeedsMfa) {
      if (!account.isNullOrBlank() && authentication != null) {
        replaceAuthentication(account, authentication, approvalParameters)
      } else if (mfaPassed) {
        approvalParameters[USER_OAUTH_APPROVAL] = "true"
        authentication
      } else authentication
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
