package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.service.AuthServicesService

@Controller
@RequestMapping("ui/services")
class ServicesController(private val authServicesService: AuthServicesService) {

  @GetMapping
  fun userIndex() = ModelAndView("ui/services", mapOf("serviceDetails" to authServicesService.list()))
}
