@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.provider.AuthorizationRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.SessionAttributes
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.service.UserContextService

@Controller
@SessionAttributes("authorizationRequest")
class UserSelectorController(private val userContextService: UserContextService) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @GetMapping("/oauth/confirm_access")
  fun userSelector(
    authentication: Authentication,
    authorizationRequest: AuthorizationRequest,
    model: Map<String, *>,
  ): ModelAndView {
    log.info("Found client auth request with ${authorizationRequest.clientId}")
    return ModelAndView("userSelector", model)
  }
}
