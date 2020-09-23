package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service
import uk.gov.justice.digital.hmpps.oauth2server.landing.LandingService

@Suppress("UNCHECKED_CAST")
class HomeControllerTest {
  private val landingService: LandingService = mock()

  @Test
  fun home() {
    val homeController = HomeController(landingService)
    val modelAndView = homeController.home(authenticationWithRole())
    assertThat(modelAndView.viewName).isEqualTo("landing")
  }

  @Test
  fun home_model() {
    whenever(landingService.findAllServices()).thenReturn(ALL_SERVICES)
    val homeController = HomeController(landingService)
    val modelAndView = homeController.home(authenticationWithRole("ROLE_LICENCE_DM"))
    val allocatedServices = modelAndView.model["services"] as List<Service>?
    assertThat(allocatedServices).extracting<String> { it.code }.containsExactly("DM", "LIC", "NOMIS")
  }

  @Test
  fun home_view() {
    whenever(landingService.findAllServices()).thenReturn(ALL_SERVICES)
    val homeController = HomeController(landingService)
    val modelAndView = homeController.home(authenticationWithRole("ROLE_LICENCE_DM"))
    assertThat(modelAndView.viewName).isEqualTo("landing")
  }

  private fun authenticationWithRole(vararg roles: String): Authentication {
    val authorities = roles.map { SimpleGrantedAuthority(it) }.toList()
    return UsernamePasswordAuthenticationToken("user", "pass", authorities)
  }

  companion object {
    private val ALL_SERVICES = listOf(
        createService("DM", "ROLE_LICENCE_DM", "a@b.com"),  // single role
        createService("LIC", "ROLE_LICENCE_CA,ROLE_LICENCE_DM,ROLE_LICENCE_RO", null),  // multiple role
        createService("NOMIS", null, "c@d.com"),  // available to all roles
        createService("OTHER", "ROLE_OTHER", null)) // not available

    private fun createService(code: String, roles: String?, email: String?): Service =
        Service(code, "NAME", "Description", roles, "http://some.url", true, email)
  }
}
