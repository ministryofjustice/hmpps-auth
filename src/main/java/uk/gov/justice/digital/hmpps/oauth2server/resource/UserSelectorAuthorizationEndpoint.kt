@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.common.util.OAuth2Utils
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService
import org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.SessionAttributes
import org.springframework.web.bind.support.SessionStatus
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.View
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import java.security.Principal

@Controller
@SessionAttributes("authorizationRequest", "org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint.ORIGINAL_AUTHORIZATION_REQUEST")
class UserSelectorAuthorizationEndpoint(private val authorizationEndpoint: AuthorizationEndpoint,
                                        private val jdbcClientDetailsService: JdbcClientDetailsService) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @GetMapping(value = ["/oauth/authorize"])
  fun authorize(model: MutableMap<String, *>?, @RequestParam parameters: Map<String, String>, sessionStatus: SessionStatus?, principal: Principal?): ModelAndView? {
    return authorizationEndpoint.authorize(model, parameters, sessionStatus, principal)
  }

  @PostMapping(value = ["/oauth/authorize"], params = [OAuth2Utils.USER_OAUTH_APPROVAL])
  fun approveOrDeny(@RequestParam approvalParameters: MutableMap<String, String>, model: MutableMap<String, *>?, sessionStatus: SessionStatus?, authentication: Authentication?): View {
    val account = approvalParameters[OAuth2Utils.USER_OAUTH_APPROVAL]

    log.info("Found username of $account, need to replace principal and then carry on")

    val replacedAuthentication = if (!account.isNullOrBlank() && authentication != null) {
      val source = account.substringBefore("/")
      val username = account.substringAfter("/")

      // TODO: load the user from the auth source and username and check related to principal

      val user = authentication.principal as UserDetailsImpl

      // if we're successful with the replace then change the approval parameter to true
      approvalParameters[OAuth2Utils.USER_OAUTH_APPROVAL] = "true"

      // now replace the principal
      UsernamePasswordAuthenticationToken(UserDetailsImpl(username, user.name, user.authorities, source, username, user.jwtId), authentication.credentials, authentication.authorities)
    } else {
      authentication
    }
    return authorizationEndpoint.approveOrDeny(approvalParameters, model, sessionStatus, replacedAuthentication)
  }
}
