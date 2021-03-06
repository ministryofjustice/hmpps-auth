package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ClientType
import uk.gov.justice.digital.hmpps.oauth2server.service.ClientFilter
import uk.gov.justice.digital.hmpps.oauth2server.service.ClientService
import uk.gov.justice.digital.hmpps.oauth2server.service.SortBy

@Suppress("DEPRECATION")
@Controller
class UiController(private val clientService: ClientService) {

  @GetMapping("/ui")
  fun userIndex(
    @RequestParam(defaultValue = "client") sort: SortBy,
    @RequestParam role: String? = null,
    @RequestParam grantType: String? = null,
    @RequestParam clientType: ClientType? = null,
  ) = ModelAndView(
    "ui/index",
    "clientDetails",
    clientService.listUniqueClients(sort, ClientFilter(grantType = grantType, role = role, clientType = clientType))
  )
}
