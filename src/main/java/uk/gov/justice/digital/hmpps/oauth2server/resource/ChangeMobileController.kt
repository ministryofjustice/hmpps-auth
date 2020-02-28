package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService

@Controller
class ChangeMobileController(private val userService: UserService) {

  @GetMapping("/new-mobile")
  fun newMobileRequest(authentication: Authentication): ModelAndView {
    val currentMobile = userService.findUser(authentication.name).orElseThrow().mobile;
    return ModelAndView("changeMobile", "mobile", currentMobile)
  }
}

