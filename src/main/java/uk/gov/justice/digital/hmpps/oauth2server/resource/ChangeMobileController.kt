package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView

@Controller
class ChangeMobileController() {

  @GetMapping("/new-mobile")
  fun newMobileRequest(): ModelAndView {
    return ModelAndView("changeMobile")
  }
}

