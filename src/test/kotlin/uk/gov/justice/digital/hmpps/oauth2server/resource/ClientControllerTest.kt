@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.provider.ClientAlreadyExistsException
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.NoSuchClientException
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService
import org.springframework.ui.ExtendedModelMap
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Client
import uk.gov.justice.digital.hmpps.oauth2server.resource.ClientsController.AuthClientDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.service.ClientDetailsWithCopies
import uk.gov.justice.digital.hmpps.oauth2server.service.ClientService
import uk.gov.justice.digital.hmpps.oauth2server.service.DuplicateClientsException

class ClientControllerTest {
  private val clientDetailsService: JdbcClientDetailsService = mock()
  private val clientService: ClientService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val controller = ClientsController(clientDetailsService, clientService, telemetryClient)
  private val authentication = TestingAuthenticationToken(
    UserDetailsImpl("user", "name", setOf(), AuthSource.auth.name, "userid", "jwtId"),
    "pass"
  )

  @Nested
  inner class EditFormRequest {
    @Test
    fun `show edit form new client`() {
      val model = ExtendedModelMap()
      val view = controller.showEditForm(null, model)

      assertThat(view).isEqualTo("ui/form")
      assertThat(model["clients"] as List<*>).isEmpty()
      assertThat(model["clientDetails"] as ClientDetails).isNotNull
    }

    @Test
    fun `show edit form existing client`() {
      val model = ExtendedModelMap()
      whenever(clientService.loadClientWithCopies(anyString())).thenReturn(
        ClientDetailsWithCopies(BaseClientDetails(), listOf(Client("client-1")))
      )
      val view = controller.showEditForm("client-id", model)

      assertThat(view).isEqualTo("ui/form")
      assertThat(model["clients"] as List<*>).extracting("id").containsOnly("client-1")
      assertThat(model["clientDetails"] as ClientDetails).isNotNull
    }
  }

  @Nested
  inner class EditClient {

    @Test
    fun `edit client request - add client`() {
      val authClientDetails: AuthClientDetails = createAuthClientDetails()
      authClientDetails.clientSecret = "bob"
      val modelAndView = controller.editClient(authentication, authClientDetails, "true")
      verify(clientDetailsService).addClientDetails(authClientDetails)
      verify(telemetryClient).trackEvent(
        "AuthClientDetailsAdd",
        mapOf("username" to "user", "clientId" to "client"),
        null
      )
      verify(telemetryClient).trackEvent(
        "AuthClientSecretUpdated",
        mapOf("username" to "user", "clientId" to "client"),
        null
      )

      assertThat(modelAndView.viewName).isEqualTo("redirect:/ui/clients/client-success")
      assertThat(modelAndView.model).containsOnly(
        entry("newClient", "true"),
        entry("clientId", "client"),
        entry("clientSecret", "bob"),
        entry("base64ClientId", "Y2xpZW50"),
        entry("base64ClientSecret", "Ym9i"),
      )
    }

    @Test
    fun `edit client request - add client throws ClientAlreadyExistsException`() {
      val authClientDetails: AuthClientDetails = createAuthClientDetails()
      authClientDetails.clientSecret = "bob"
      val exception = ClientAlreadyExistsException("Client already exists: ")
      doThrow(exception).whenever(clientDetailsService).addClientDetails(authClientDetails)

      assertThatThrownBy { controller.editClient(authentication, authClientDetails, "true") }.isEqualTo(exception)

      verifyZeroInteractions(telemetryClient)
    }

    @Test
    fun `edit client request - update existing client`() {
      val authClientDetails: AuthClientDetails = createAuthClientDetails()
      val modelAndView = controller.editClient(authentication, authClientDetails, null)
      verify(clientDetailsService).updateClientDetails(authClientDetails)
      verify(telemetryClient).trackEvent(
        "AuthClientDetailsUpdate",
        mapOf("username" to "user", "clientId" to "client"),
        null
      )
      verify(telemetryClient, times(0)).trackEvent(
        "AuthClientSecretUpdated",
        mapOf("username" to "user", "clientId" to "client"),
        null
      )
      assertThat(modelAndView.viewName).isEqualTo("redirect:/ui")
    }

    @Test
    fun `edit client request - update client throws NoSuchClientException`() {
      val authClientDetails: AuthClientDetails = createAuthClientDetails()
      val exception = NoSuchClientException("No client found with id = ")
      doThrow(exception).whenever(clientDetailsService).updateClientDetails(authClientDetails)

      assertThatThrownBy { controller.editClient(authentication, authClientDetails, null) }.isEqualTo(exception)

      verifyZeroInteractions(telemetryClient)
    }

    @Test
    fun `edit client request - update existing client secret`() {
      val authClientDetails: AuthClientDetails = createAuthClientDetails()
      authClientDetails.clientSecret = "bob"
      val modelAndView = controller.editClient(authentication, authClientDetails, null)
      verify(clientDetailsService).updateClientDetails(authClientDetails)
      verify(telemetryClient).trackEvent(
        "AuthClientDetailsUpdate",
        mapOf("username" to "user", "clientId" to "client"),
        null
      )
      verify(telemetryClient).trackEvent(
        "AuthClientSecretUpdated",
        mapOf("username" to "user", "clientId" to "client"),
        null
      )
      verify(clientService).findAndUpdateDuplicates("client")
      assertThat(modelAndView.viewName).isEqualTo("redirect:/ui/clients/client-success")
      assertThat(modelAndView.model).containsOnly(
        entry("newClient", "false"),
        entry("clientId", "client"),
        entry("clientSecret", "bob"),
        entry("base64ClientId", "Y2xpZW50"),
        entry("base64ClientSecret", "Ym9i"),
      )
    }

    private fun createAuthClientDetails(): AuthClientDetails {
      val authClientDetails = AuthClientDetails()
      authClientDetails.clientId = "client"
      authClientDetails.setAuthorizedGrantTypes(listOf("client_credentials"))
      authClientDetails.authorities = mutableListOf(GrantedAuthority { "ROLE_CLIENT" })
      authClientDetails.clientSecret = ""
      return authClientDetails
    }
  }

  @Nested
  inner class GenerateNewClientSecretPrompt {

    @Test
    fun `new client secret prompt`() {
      val modelAndView = controller.newClientSecretPrompt(authentication, "client", "2021-01-01T12:12:12.482760")
      assertThat(modelAndView.viewName).isEqualTo("ui/generateSecretPrompt")
      assertThat(modelAndView.model).containsOnly(
        entry("clientId", "client"),
        entry("lastAccessed", "2021-01-01T12:12:12.482760"),
      )
    }
  }

  @Nested
  inner class GenerateNewClientSecret {

    @Test
    fun `new client secret`() {
      whenever(clientService.generateClientSecret(anyString())).thenReturn("Some-Secret")
      val modelAndView = controller.generateNewClientSecret(authentication, "client")
      assertThat(modelAndView.viewName).isEqualTo("redirect:/ui/clients/client-success")
      assertThat(modelAndView.model).containsOnly(
        entry("newClient", "false"),
        entry("clientId", "client"),
        entry("clientSecret", "Some-Secret"),
        entry("base64ClientId", "Y2xpZW50"),
        entry("base64ClientSecret", "U29tZS1TZWNyZXQ="),
      )
    }
  }

  @Nested
  inner class DuplicateClientRequest {

    @Test
    fun `Duplicate client`() {
      whenever(clientService.duplicateClient(anyString())).thenReturn(createAuthClientDetails())
      val mandv = controller.duplicateClient(authentication, "client")
      verify(telemetryClient).trackEvent(
        "AuthClientDetailsDuplicated",
        mapOf("username" to "user", "clientId" to "client-1"),
        null
      )
      assertThat(mandv.viewName).isEqualTo("redirect:/ui/clients/duplicate-client-success")
      assertThat(mandv.model).containsOnly(
        entry("clientId", "client-1"),
        entry("clientSecret", "Some-Secret"),
        entry("base64ClientId", "Y2xpZW50LTE="),
        entry("base64ClientSecret", "U29tZS1TZWNyZXQ="),
      )
    }

    @Test
    fun `Duplicate client throw exception max duplicated`() {
      doThrow(
        DuplicateClientsException(
          "client",
          "Duplicate clientId failed for some-client with reason: MaxReached"
        )
      ).whenever(clientService).duplicateClient(anyString())

      val mandv = controller.duplicateClient(authentication, "client")

      verifyZeroInteractions(telemetryClient)
      assertThat(mandv.viewName).isEqualTo("redirect:/ui/clients/form")
      assertThat(mandv.model).containsOnly(
        entry("client", "client"),
        entry("error", "maxDuplicates"),
      )
    }

    private fun createAuthClientDetails(): AuthClientDetails {
      val authClientDetails = AuthClientDetails()
      authClientDetails.clientId = "client-1"
      authClientDetails.setAuthorizedGrantTypes(listOf("client_credentials"))
      authClientDetails.authorities = mutableListOf(GrantedAuthority { "ROLE_CLIENT" })
      authClientDetails.clientSecret = "Some-Secret"
      return authClientDetails
    }
  }

  @Nested
  inner class DeleteClientRequest {

    @Test
    fun `delete Client Request view`() {
      val view = controller.deleteClient(authentication, "client")
      verify(telemetryClient).trackEvent(
        "AuthClientDetailsDeleted",
        mapOf("username" to "user", "clientId" to "client"),
        null
      )
      assertThat(view).isEqualTo("redirect:/ui")
    }

    @Test
    fun `delete Client Request - delete client throws NoSuchClientException`() {

      val exception = NoSuchClientException("No client found with id = ")
      doThrow(exception).whenever(clientDetailsService).removeClientDetails(anyString())

      assertThatThrownBy { controller.deleteClient(authentication, "client") }.isEqualTo(exception)

      verifyZeroInteractions(telemetryClient)
    }
  }

  @Nested
  inner class AuthClientDetailsTest {
    @Test
    fun `set mfa`() {
      val authClientDetails = AuthClientDetails()
      authClientDetails.mfa = MfaAccess.all
      assertThat(authClientDetails.additionalInformation).containsExactlyEntriesOf(mapOf("mfa" to MfaAccess.all))
      assertThat(authClientDetails.mfa).isEqualTo(MfaAccess.all)
    }

    @Test
    fun `get mfa`() {
      val authClientDetails = AuthClientDetails()
      authClientDetails.addAdditionalInformation("mfa", MfaAccess.untrusted)
      assertThat(authClientDetails.mfa).isEqualTo(MfaAccess.untrusted)
    }

    @Test
    fun `get mfa not set`() {
      val authClientDetails = AuthClientDetails()
      assertThat(authClientDetails.mfa).isNull()
    }
  }
}
