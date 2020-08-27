package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType

class LoginControllerTest {
  private val iterableClientRegistrations: Iterable<ClientRegistration> = mock()

  private val controller = LoginController(iterableClientRegistrations)
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
  fun `login page when azure active directory client configured`() {
    val clients = listOf(ClientRegistration
            .withRegistrationId("test")
            .clientName("Test")
            .clientId("bd4de96a-437d-4fef-b1b2-5f4c1e39c080")
            .redirectUriTemplate("{baseUrl}/login/oauth2/code/{registrationId}")
            .authorizationUri("/test")
            .tokenUri("/test")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .build())

    val controller = LoginController(clients)

    val modelAndView = controller.loginPage(null)
    assertThat(modelAndView.viewName).isEqualTo("login")
    assertThat(modelAndView.status).isNull()
    assertThat(modelAndView.modelMap["oauth2Clients"]).asList().hasSize(1)
  }
}
