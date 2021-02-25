@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.SessionAttributes
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
@SessionAttributes(
  "authorizationRequest",
  "org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint.ORIGINAL_AUTHORIZATION_REQUEST"
)
class MfaServiceBasedController(
  private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler,
) {

  @GetMapping("/service-mfa-challenge")
  fun mfaChallengeRequest(
    @RequestParam user_oauth_approval: String?,
  ): ModelAndView = ModelAndView(
    "serviceMfaChallenge",
    mapOf("smokeCode" to "123456", "user_oauth_approval" to user_oauth_approval),
  )

  @PostMapping("/service-mfa-challenge")
  fun mfaChallenge(
    request: HttpServletRequest,
    response: HttpServletResponse,
    authentication: Authentication,
  ): String {

    // TODO: Check token to ensure passed mfa
    jwtAuthenticationSuccessHandler.updateMfaInRequest(request, response, authentication)

    return "forward:/oauth/authorize"
  }
}
