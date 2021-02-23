@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping

@Controller
class UserSelectorController {
  @PostMapping("/user-selector")
  fun approveOrDeny(): String = "forward:/oauth/authorize"
}
