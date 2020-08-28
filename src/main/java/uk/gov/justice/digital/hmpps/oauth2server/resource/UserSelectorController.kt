@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.provider.AuthorizationRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.SessionAttributes
import org.springframework.web.servlet.ModelAndView

@Controller
@SessionAttributes("authorizationRequest")
class UserSelectorController {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @GetMapping("/oauth/confirm_access")
  fun userSelector(authorizationRequest: AuthorizationRequest): ModelAndView {
    log.info("Found client auth request with ${authorizationRequest.clientId}")
    // TODO: Need to look up the user to see what user accounts they have that match the scopes for the requested client
    return ModelAndView("userSelector")
  }
}
