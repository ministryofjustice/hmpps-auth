package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.landing.LandingService
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.azuread
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.service.UserContextService

@Controller
class HomeController(private val landingService: LandingService, private val userContextService: UserContextService) {
  @GetMapping("/")
  fun home(authentication: Authentication): ModelAndView {
    val services = landingService.findAllServices()

    // special case for azure users - grab all their accounts and combine roles from them too
    val azureUserDetails = authentication.principal as UserDetailsImpl
    val authSource = AuthSource.fromNullableString(azureUserDetails.authSource)
    val authorities = if (authSource == azuread) {
      val users = userContextService.discoverUsers(authSource, azureUserDetails.userId, setOf())
      users.flatMap { it.authorities }
    } else authentication.authorities

    // create a list of services that the user can see
    val allowedServices = services.filter { s ->
      s.roles.isEmpty() || authorities.any { a -> s.roles.contains(a.authority) }
    }
    return ModelAndView("landing", "services", allowedServices)
  }

  @GetMapping("/terms")
  fun terms(): String = "terms"
}
