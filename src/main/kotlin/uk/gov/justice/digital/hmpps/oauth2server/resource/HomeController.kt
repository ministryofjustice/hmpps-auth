package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.azuread
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.service.AuthServicesService
import uk.gov.justice.digital.hmpps.oauth2server.service.UserContextService

@Controller
class HomeController(
  private val authServicesService: AuthServicesService,
  private val userContextService: UserContextService,
) {
  @GetMapping("/")
  fun home(authentication: Authentication): ModelAndView {
    // special case for azure users - grab all their accounts and combine roles from them too
    val azureUserDetails = authentication.principal as UserDetailsImpl
    val authSource = AuthSource.fromNullableString(azureUserDetails.authSource)
    val authorities = if (authSource == azuread) {
      val users = userContextService.discoverUsers(authentication.principal as UserDetailsImpl, setOf())
      users.flatMap { it.authorities }
    } else authentication.authorities

    val services = authServicesService.listEnabled(authorities)

    return ModelAndView("landing", "services", services)
  }

  @GetMapping("/terms")
  fun terms(): String = "terms"
}
