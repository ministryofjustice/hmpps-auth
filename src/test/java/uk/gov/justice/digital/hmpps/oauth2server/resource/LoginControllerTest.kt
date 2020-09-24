package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import java.util.*

class LoginControllerTest {
  private val clientRegistrationRepository: Optional<InMemoryClientRegistrationRepository> = Optional.empty()

  private val controller = LoginController(clientRegistrationRepository)
  @Test
  fun loginPage_NoError() {
    val modelAndView = controller.loginPage(null)
    assertThat(modelAndView.viewName).isEqualTo("login")
    assertThat(modelAndView.status).isNull()
    assertThat(modelAndView.modelMap["oauth2Clients"]).asList().isEmpty()
  }

  @Test
  fun loginPage_CurrentError() {
    val modelAndView = controller.loginPage("bad")
    assertThat(modelAndView.viewName).isEqualTo("login")
    assertThat(modelAndView.status).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(modelAndView.modelMap["oauth2Clients"]).asList().isEmpty()
  }

  @Test
  fun `login page shows links when OIDC clients are configured`() {
    val clients = listOf(ClientRegistration
        .withRegistrationId("test")
        .clientName("Test")
        .clientId("bd4de96a-437d-4fef-b1b2-5f4c1e39c080")
        .redirectUriTemplate("{baseUrl}/login/oauth2/code/{registrationId}")
        .authorizationUri("/test")
        .tokenUri("/test")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .build())

    val clientRegistrationRepository = Optional.of(InMemoryClientRegistrationRepository(clients))

    val controller = LoginController(clientRegistrationRepository)

    val modelAndView = controller.loginPage(null)
    assertThat(modelAndView.viewName).isEqualTo("login")
    assertThat(modelAndView.status).isNull()
    assertThat(modelAndView.modelMap["oauth2Clients"]).asList().hasSize(1)
  }
}
