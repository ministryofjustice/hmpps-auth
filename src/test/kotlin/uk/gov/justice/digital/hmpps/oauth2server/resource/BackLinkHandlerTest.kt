@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.ClientDetailsService
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import uk.gov.justice.digital.hmpps.oauth2server.resource.account.BackLinkHandler

class BackLinkHandlerTest {
  private val clientDetailsService: ClientDetailsService = mock()
  private val backLinkHandler = BackLinkHandler(clientDetailsService, false)

  @Test
  fun validateRedirect_NoClientId() {
    val validUri = backLinkHandler.validateRedirect("", "http://some.where")
    assertThat(validUri).isFalse
  }

  @Test
  fun validateRedirect_ClientIdNotMatched() {
    val validUri = backLinkHandler.validateRedirect("joe", "http://some.where")
    assertThat(validUri).isFalse
  }

  @Test
  fun validateRedirect_RedirectUriNotMatched() {
    whenever(clientDetailsService.loadClientByClientId("joe")).thenReturn(createClientDetails("http://tim.buk.tu"))
    val validUri = backLinkHandler.validateRedirect("joe", "http://some.where")
    assertThat(validUri).isFalse
  }

  @Test
  fun validateRedirect_NoRedirectUrisConfigured() {
    whenever(clientDetailsService.loadClientByClientId("joe")).thenReturn(null)
    val validUri = backLinkHandler.validateRedirect("joe", "http://some.where")
    assertThat(validUri).isFalse
  }

  @Test
  fun validateRedirect_RedirectUriMatched() {
    whenever(clientDetailsService.loadClientByClientId("joe")).thenReturn(
      createClientDetails(
        "http://tim.buk.tu",
        "http://some.where"
      )
    )
    val validUri = backLinkHandler.validateRedirect("joe", "http://some.where")
    assertThat(validUri).isTrue
  }

  @Test
  fun validateRedirect_RedirectUriMatchedWithPort() {
    whenever(clientDetailsService.loadClientByClientId("joe")).thenReturn(
      createClientDetails(
        "http://tim.buk.tu",
        "http://some.where:3001"
      )
    )
    val validUri = backLinkHandler.validateRedirect("joe", "http://some.where:3001")
    assertThat(validUri).isTrue
  }

  @Test
  fun validateRedirect_RedirectUriMatched_SubdomainPolicyNotSet() {
    val subdomainHandler = BackLinkHandler(clientDetailsService, false)
    whenever(clientDetailsService.loadClientByClientId("joe")).thenReturn(
      createClientDetails(
        "http://tim.buk.tu",
        "http://where"
      )
    )
    val validUri = subdomainHandler.validateRedirect("joe", "http://some.where")
    assertThat(validUri).isFalse
  }

  @Test
  fun validateRedirect_RedirectUriMatched_Subdomain() {
    val subdomainHandler = BackLinkHandler(clientDetailsService, true)
    whenever(clientDetailsService.loadClientByClientId("joe")).thenReturn(
      createClientDetails(
        "http://tim.buk.tu",
        "http://where"
      )
    )
    val validUri = subdomainHandler.validateRedirect("joe", "http://some.where")
    assertThat(validUri).isTrue
  }

  @Test
  fun validateRedirect_RedirectUriMatchedWithSlash() {
    whenever(clientDetailsService.loadClientByClientId("joe")).thenReturn(
      createClientDetails(
        "http://tim.buk.tu",
        "http://some.where"
      )
    )
    val validUri = backLinkHandler.validateRedirect("joe", "http://some.where")
    assertThat(validUri).isTrue
  }

  @Test
  fun validateRedirect_RedirectUriMatchedWithoutSlash() {
    whenever(clientDetailsService.loadClientByClientId("joe")).thenReturn(
      createClientDetails(
        "http://tim.buk.tu",
        "http://some.where/"
      )
    )
    val validUri = backLinkHandler.validateRedirect("joe", "http://some.where")
    assertThat(validUri).isTrue
  }

  @Test
  fun validateRedirect_RedirectDomainMatchedWithoutURIPath() {
    whenever(clientDetailsService.loadClientByClientId("joe")).thenReturn(
      createClientDetails(
        "http://tim.buk.tu",
        "http://some.where/"
      )
    )
    val validUri = backLinkHandler.validateRedirect("joe", "http://some.where/specific/page")
    assertThat(validUri).isTrue
  }

  @Test
  fun validateRedirect_RedirectDomainMatchedForHTTPSWithPort443() {
    whenever(clientDetailsService.loadClientByClientId("joe")).thenReturn(
      createClientDetails(
        "http://tim.buk.tu",
        "https://some.where/"
      )
    )
    val validUri = backLinkHandler.validateRedirect("joe", "https://some.where:443/specific/page")
    assertThat(validUri).isTrue
  }

  private fun createClientDetails(vararg urls: String): ClientDetails {
    val details = BaseClientDetails()
    details.registeredRedirectUri = setOf(*urls)
    details.setAuthorizedGrantTypes(listOf("authorization_code"))
    return details
  }
}
