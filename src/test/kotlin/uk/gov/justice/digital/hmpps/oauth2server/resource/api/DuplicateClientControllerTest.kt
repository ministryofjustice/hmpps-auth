@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.oauth2.provider.NoSuchClientException
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import uk.gov.justice.digital.hmpps.oauth2server.service.ClientService
import uk.gov.justice.digital.hmpps.oauth2server.service.DuplicateClientsException

class DuplicateClientControllerTest {

  private val clientService: ClientService = mock()
  private val authentication = TestingAuthenticationToken(
    "duplicate",
    "pass",
    "ROLE_OAUTH_ADMIN"
  )
  private val duplicateClientController = DuplicateClientController(clientService)

  @Test
  fun `Duplicate client`() {
    val client = BaseClientDetails(
      "client-1", null,
      "read,write", "client_credentials", "ROLE_"
    )
    client.clientSecret = "SOME-RANDOM-STRING"
    whenever(clientService.duplicateClient(anyString())).thenReturn(client)
    val newDuplicateClient = duplicateClientController.duplicateClient(authentication, "client")

    assertThat(newDuplicateClient).isEqualTo(DuplicateClientDetail(client))
    assertThat(newDuplicateClient.clientId).isEqualTo("client-1")
    assertThat(newDuplicateClient.clientSecret).isEqualTo("SOME-RANDOM-STRING")
    assertThat(newDuplicateClient.base64ClientId).isEqualTo("Y2xpZW50LTE=")
    assertThat(newDuplicateClient.base64ClientSecret).isEqualTo("U09NRS1SQU5ET00tU1RSSU5H")
  }

  @Test
  fun `Duplicate client fails - Max Duplicates reached`() {
    doThrow(DuplicateClientsException("custard", "MaxReached")).whenever(clientService).duplicateClient(anyString())

    assertThatThrownBy { duplicateClientController.duplicateClient(authentication, "max-client") }
      .isInstanceOf(DuplicateClientsException::class.java)
      .withFailMessage("Duplicate clientId failed for max-client with reason: MaxReached")
  }

  @Test
  fun `Duplicate client fails - client not found`() {
    doThrow(NoSuchClientException("No client with requested id: non-client")).whenever(clientService)
      .duplicateClient(anyString())

    assertThatThrownBy { duplicateClientController.duplicateClient(authentication, "non-client") }
      .isInstanceOf(NoSuchClientException::class.java)
      .withFailMessage("No client with requested id: non-client")
  }
}
