package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView

@Suppress("DEPRECATION")
@Controller
class UiController(private val clientDetailsService: JdbcClientDetailsService) {

  @GetMapping("/ui")
  fun userIndex() = ModelAndView("ui/index", "clientDetails", clientDetailsService.listClientDetails())
}
