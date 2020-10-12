@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.provider.AuthorizationRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.SessionAttributes
import org.springframework.web.servlet.ModelAndView

@Controller
@SessionAttributes("authorizationRequest")
class UserSelectorController {
  @GetMapping("/oauth/confirm_access")
  fun userSelector(authentication: Authentication, authorizationRequest: AuthorizationRequest, model: Map<String, *>):
    ModelAndView = ModelAndView("userSelector", model)
}
