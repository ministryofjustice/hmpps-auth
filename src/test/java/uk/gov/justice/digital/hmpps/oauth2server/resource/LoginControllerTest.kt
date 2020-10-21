@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.provider.ClientDetailsService
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import org.springframework.security.web.savedrequest.SimpleSavedRequest
import uk.gov.justice.digital.hmpps.oauth2server.config.CookieRequestCache
import java.util.Optional
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class LoginControllerTest {
  private val clientRegistrationRepository: Optional<InMemoryClientRegistrationRepository> = Optional.empty()
  private val cookieRequestCacheMock: CookieRequestCache = mock()
  private val clientDetailsService: ClientDetailsService = mock()
  private val request: HttpServletRequest = mock()
  private val response: HttpServletResponse = mock()

  private val controller = LoginController(clientRegistrationRepository, cookieRequestCacheMock, clientDetailsService)

  @Test
  fun loginPage_NoError() {
    val modelAndView = controller.loginPage(null, null, null)
    assertThat(modelAndView.viewName).isEqualTo("login")
    assertThat(modelAndView.status).isNull()
    assertThat(modelAndView.modelMap["oauth2Clients"]).asList().isEmpty()
  }

  @Test
  fun loginPage_CurrentError() {
    val modelAndView = controller.loginPage("bad", null, null)
    assertThat(modelAndView.viewName).isEqualTo("login")
    assertThat(modelAndView.status).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(modelAndView.modelMap["oauth2Clients"]).asList().isEmpty()
  }

  @Test
  fun `login page shows links when OIDC clients are configured`() {
    val clients = listOf(
      ClientRegistration
        .withRegistrationId("test")
        .clientName("test")
        .clientId("bd4de96a-437d-4fef-b1b2-5f4c1e39c080")
        .redirectUriTemplate("{baseUrl}/login/oauth2/code/{registrationId}")
        .authorizationUri("/test")
        .tokenUri("/test")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .build()
    )

    val clientRegistrationRepository = Optional.of(InMemoryClientRegistrationRepository(clients))

    val controller = LoginController(clientRegistrationRepository, cookieRequestCacheMock, clientDetailsService)

    val modelAndView = controller.loginPage(null, request, response)
    assertThat(modelAndView.viewName).isEqualTo("login")
    assertThat(modelAndView.status).isNull()
    assertThat(modelAndView.modelMap["oauth2Clients"]).asList().hasSize(1)
  }

  @Test
  fun `redirect to Microsoft Login`() {
    val clients = listOf(
      ClientRegistration
        .withRegistrationId("test")
        .clientName("test")
        .clientId("bd4de96a-437d-4fef-b1b2-5f4c1e39c080")
        .redirectUriTemplate("{baseUrl}/login/oauth2/code/{registrationId}")
        .authorizationUri("/test")
        .tokenUri("/test")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .build()
    )

    val clientRegistrationRepository = Optional.of(InMemoryClientRegistrationRepository(clients))
    val controller = LoginController(clientRegistrationRepository, cookieRequestCacheMock, clientDetailsService)
    val returnRequest = SimpleSavedRequest("test.com/oauth/authorize?client_id=test_id")
    val clientDetailsMock: BaseClientDetails = BaseClientDetails()

    clientDetailsMock.addAdditionalInformation("skipToAzureField", true)

    whenever(cookieRequestCacheMock.getRequest(any(), any()))
      .thenReturn(returnRequest)
    whenever(clientDetailsService.loadClientByClientId(any()))
      .thenReturn(clientDetailsMock)
    val modelAndView = controller.loginPage(null, request, response)

    assertThat(modelAndView.viewName).isEqualTo("redirect:/oauth2/authorization/test")
    assertThat(modelAndView.status).isNull()
    assertThat(modelAndView.modelMap["oauth2Clients"]).asList().hasSize(1)
  }

  @Test
  fun `load login page as skip to azure not set`() {
    val returnRequest = SimpleSavedRequest("test.com/oauth/authorize?client_id=test_id")
    val clientDetailsMock = BaseClientDetails()

    whenever(cookieRequestCacheMock.getRequest(any(), any()))
      .thenReturn(returnRequest)
    whenever(clientDetailsService.loadClientByClientId(any()))
      .thenReturn(clientDetailsMock)
    val modelAndView = controller.loginPage(null, request, response)

    assertThat(modelAndView.viewName).isEqualTo("login")
    assertThat(modelAndView.status).isNull()
  }

  @Test
  fun `show login page as not an OAuth Login`() {
    val returnRequest = SimpleSavedRequest("test.com/test?client_id=test_id")

    whenever(cookieRequestCacheMock.getRequest(any(), any()))
      .thenReturn(returnRequest)
    val modelAndView = controller.loginPage(null, request, response)

    assertThat(modelAndView.viewName).isEqualTo("login")
  }
}
