package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.provider.ClientAlreadyExistsException
import org.springframework.security.oauth2.provider.NoSuchClientException
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService
import org.springframework.ui.ExtendedModelMap
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl

@ExtendWith(MockitoExtension::class)
class ClientControllerTest {
  private val clientDetailsService: JdbcClientDetailsService = mock()
  private val telemetryClient: TelemetryClient = com.nhaarman.mockito_kotlin.mock()
  private val controller = ClientsController(clientDetailsService, telemetryClient)
  private val authentication = TestingAuthenticationToken(UserDetailsImpl("user", "name", setOf(), AuthSource.auth.name, "userid", "jwtId"), "pass")

  @Nested
  inner class EditFormRequest {
    @Test
    fun `show edit form request view`() {
      val model = ExtendedModelMap()
      val view = controller.showEditForm(null, model)

      assertThat(view).isEqualTo("ui/form")
    }
  }

  @Nested
  inner class EditClient {

    @Test
    fun `edit client request - add client`() {
      val authClientDetails: ClientsController.AuthClientDetails = createAuthClientDetails()
      authClientDetails.clientSecret = "bob"
      val view = controller.editClient(authentication, authClientDetails, "true")
      verify(clientDetailsService).addClientDetails(authClientDetails)
      verify(telemetryClient).trackEvent("AuthClientDetailsAdd", mapOf("username" to "user", "clientId" to "client"), null)
      verify(telemetryClient).trackEvent("AuthClientSecretUpdated", mapOf("username" to "user", "clientId" to "client"), null)

      assertThat(view).isEqualTo("redirect:/ui")
    }

    @Test
    fun `edit client request - add client throws ClientAlreadyExistsException`() {
      val authClientDetails: ClientsController.AuthClientDetails = createAuthClientDetails()
      authClientDetails.clientSecret = "bob"
      val exception = ClientAlreadyExistsException("Client already exists: ")
      doThrow(exception).whenever(clientDetailsService).addClientDetails(authClientDetails)

      assertThatThrownBy { controller.editClient(authentication, authClientDetails, "true") }.isEqualTo(exception)

      verify(telemetryClient, times(0)).trackEvent("AuthClientDetailsAdd", mapOf("username" to "user", "clientId" to "client"), null)
      verify(telemetryClient, times(0)).trackEvent("AuthClientSecretUpdated", mapOf("username" to "user", "clientId" to "client"), null)
    }

    @Test
    fun `edit client request - update existing client`() {
      val authClientDetails: ClientsController.AuthClientDetails = createAuthClientDetails()
      val view = controller.editClient(authentication, authClientDetails, null)
      verify(clientDetailsService).updateClientDetails(authClientDetails)
      verify(telemetryClient).trackEvent("AuthClientDetailsUpdate", mapOf("username" to "user", "clientId" to "client"), null)
      verify(telemetryClient, times(0)).trackEvent("AuthClientSecretUpdated", mapOf("username" to "user", "clientId" to "client"), null)
      assertThat(view).isEqualTo("redirect:/ui")
    }

    @Test
    fun `edit client request - update client throws NoSuchClientException`() {
      val authClientDetails: ClientsController.AuthClientDetails = createAuthClientDetails()
      val exception = NoSuchClientException("No client found with id = ")
      doThrow(exception).whenever(clientDetailsService).updateClientDetails(authClientDetails)

      assertThatThrownBy { controller.editClient(authentication, authClientDetails, null) }.isEqualTo(exception)
      verify(telemetryClient, times(0)).trackEvent("AuthClientDetailsUpdate", mapOf("username" to "user", "clientId" to "client"), null)
      verify(telemetryClient, times(0)).trackEvent("AuthClientSecretUpdated", mapOf("username" to "user", "clientId" to "client"), null)
    }

    @Test
    fun `edit client request - update existing client secret`() {
      val authClientDetails: ClientsController.AuthClientDetails = createAuthClientDetails()
      authClientDetails.clientSecret = "bob"
      val view = controller.editClient(authentication, authClientDetails, null)
      verify(clientDetailsService).updateClientDetails(authClientDetails)
      verify(telemetryClient).trackEvent("AuthClientDetailsUpdate", mapOf("username" to "user", "clientId" to "client"), null)
      verify(telemetryClient).trackEvent("AuthClientSecretUpdated", mapOf("username" to "user", "clientId" to "client"), null)
      assertThat(view).isEqualTo("redirect:/ui")
    }

    private fun createAuthClientDetails(): ClientsController.AuthClientDetails {
      val authClientDetails: ClientsController.AuthClientDetails = ClientsController.AuthClientDetails()
      authClientDetails.clientId = "client"
      authClientDetails.setAuthorizedGrantTypes(listOf("client_credentials"))
      authClientDetails.authorities = mutableListOf(GrantedAuthority { "ROLE_CLIENT" })
      authClientDetails.clientSecret = ""
      return authClientDetails
    }
  }

  @Nested
  inner class DeleteClientRequest {

    @Test
    fun `delete Client Request view`() {
      val view = controller.deleteClient(authentication, "client")
      verify(telemetryClient).trackEvent("AuthClientDetailsDeleted", mapOf("username" to "user", "clientId" to "client"), null)
      assertThat(view).isEqualTo("redirect:/ui")
    }

    @Test
    fun `delete Client Request - delete client throws NoSuchClientException`() {

      val exception = NoSuchClientException("No client found with id = ")
      doThrow(exception).whenever(clientDetailsService).removeClientDetails(anyString())

      assertThatThrownBy { controller.deleteClient(authentication, "client") }.isEqualTo(exception)

      verify(telemetryClient, times(0)).trackEvent("AuthClientDetailsDeleted", mapOf("username" to "user", "clientId" to "client"), null)
      verify(telemetryClient, times(0)).trackEvent("AuthClientSecretUpdated", mapOf("username" to "user", "clientId" to "client"), null)
    }

  }
}
