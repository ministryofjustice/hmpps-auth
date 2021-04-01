package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.service.ClientService

@Suppress("DEPRECATION")
@Controller
class UiController(private val clientService: ClientService) {

  @GetMapping("/ui")
  fun userIndex() = ModelAndView("ui/index", "clientDetails", clientService.listUniqueClients())
}
