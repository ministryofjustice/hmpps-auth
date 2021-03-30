@file:Suppress("ClassName", "DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Client
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.ClientRepository
import uk.gov.justice.digital.hmpps.oauth2server.resource.ClientsController.AuthClientDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.PasswordGenerator

internal class ClientServiceTest {
  private val clientRepository: ClientRepository = mock()
  private val clientDetailsService: JdbcClientDetailsService = mock()
  private val passwordGenerator: PasswordGenerator = mock()
  private val clientService = ClientService(clientDetailsService, passwordGenerator, clientRepository)

  @Nested
  inner class findAndUpdateDuplicates {
    @Test
    internal fun `no replacement`() {
      whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(createAuthClientDetails())
      clientService.findAndUpdateDuplicates("some-client")
      verify(clientRepository).findByIdStartsWith("some-client")
      verify(clientDetailsService, never()).updateClientDetails(any())
    }

    @Test
    internal fun duplicate() {
      val authClientDetails = createAuthClientDetails()
      whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(authClientDetails)
      whenever(clientRepository.findByIdStartsWith(any())).thenReturn(listOf(Client("some-client-24"), Client("copy-2")))
      clientService.findAndUpdateDuplicates("some-client-24")
      verify(clientRepository).findByIdStartsWith("some-client")
      verify(clientDetailsService).updateClientDetails(
        check {
          assertThat(it).usingRecursiveComparison().isEqualTo(createBaseClientDetails(authClientDetails))
        }
      )
    }

    @Test
    internal fun `other-client-with-numbers`() {
      whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(createAuthClientDetails())
      clientService.findAndUpdateDuplicates("some-client-24-id")
      verify(clientRepository).findByIdStartsWith("some-client-24-id")
    }
  }

  @Nested
  inner class duplicateClient {
    @Test
    internal fun `duplicate original client`() {
      val authClientDetails = createAuthClientDetails()
      whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(authClientDetails)
      whenever(clientRepository.findByIdStartsWith(any())).thenReturn(listOf(Client("some-client")))
      whenever(passwordGenerator.generatePassword()).thenReturn("O)Xbqg6F–Q7211cj&jUL)oC=E;s9^pFZ:3#")

      clientService.duplicateClient("some-client")

      verify(clientDetailsService).addClientDetails(
        check {
          assertThat(it).usingRecursiveComparison().ignoringFields("clientId", "clientSecret").isEqualTo(createBaseClientDetails(authClientDetails))
          assertThat(it.clientId).isEqualTo("some-client-1")
          assertThat(it.clientSecret).isEqualTo("O)Xbqg6F–Q7211cj&jUL)oC=E;s9^pFZ:3#")
        }
      )
    }

    @Test
    internal fun `duplicate client incrementing number correctly`() {
      val authClientDetails = createAuthClientDetails()
      whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(authClientDetails)
      whenever(clientRepository.findByIdStartsWith(any())).thenReturn(listOf(Client("some-client"), Client("some-client-1")))
      whenever(passwordGenerator.generatePassword()).thenReturn("O)Xbqg6F–Q7211cj&jUL)oC=E;s9^pFZ:3#")

      clientService.duplicateClient("some-client-1")

      verify(clientDetailsService).addClientDetails(
        check {
          assertThat(it).usingRecursiveComparison().ignoringFields("clientId", "clientSecret").isEqualTo(createBaseClientDetails(authClientDetails))
          assertThat(it.clientId).isEqualTo("some-client-2")
          assertThat(it.clientSecret).isEqualTo("O)Xbqg6F–Q7211cj&jUL)oC=E;s9^pFZ:3#")
        }
      )
    }
    @Test
    internal fun `duplicate client incrementing number correctly when original client duplicated`() {
      val authClientDetails = createAuthClientDetails()
      whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(authClientDetails)
      whenever(clientRepository.findByIdStartsWith(any())).thenReturn(listOf(Client("some-client"), Client("some-client-4")))
      whenever(passwordGenerator.generatePassword()).thenReturn("O)Xbqg6F–Q7211cj&jUL)oC=E;s9^pFZ:3#")

      clientService.duplicateClient("some-client")

      verify(clientDetailsService).addClientDetails(
        check {
          assertThat(it).usingRecursiveComparison().ignoringFields("clientId", "clientSecret").isEqualTo(createBaseClientDetails(authClientDetails))
          assertThat(it.clientId).isEqualTo("some-client-5")
          assertThat(it.clientSecret).isEqualTo("O)Xbqg6F–Q7211cj&jUL)oC=E;s9^pFZ:3#")
        }
      )
    }

    @Test
    internal fun `will throw error if 3 clients already exist for base client id`() {
      val authClientDetails = createAuthClientDetails()
      whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(authClientDetails)
      whenever(clientRepository.findByIdStartsWith(any())).thenReturn(listOf(Client("some-client"), Client("some-client-1"), Client("some-client-2")))

      assertThatThrownBy { clientService.duplicateClient("some-client") }
        .isInstanceOf(DuplicateClientsException::class.java).hasMessage("Duplicate clientId failed for some-client with reason: MaxReached")
    }
  }

  private fun createAuthClientDetails(): AuthClientDetails {
    val authClientDetails = AuthClientDetails()
    authClientDetails.clientId = "client"
    authClientDetails.clientSecret = ""
    authClientDetails.setScope(listOf("read", "write"))
    authClientDetails.setResourceIds(listOf("resourceId"))
    authClientDetails.setAuthorizedGrantTypes(listOf("token", "client"))
    authClientDetails.setRegisteredRedirectUri(setOf("some://url"))
    authClientDetails.setAutoApproveScopes(listOf("read", "delius"))
    authClientDetails.authorities = listOf(SimpleGrantedAuthority("role1"), SimpleGrantedAuthority("role2"))
    authClientDetails.accessTokenValiditySeconds = 10
    authClientDetails.refreshTokenValiditySeconds = 20
    authClientDetails.additionalInformation = mapOf("additional" to "info")
    return authClientDetails
  }

  private fun createBaseClientDetails(authClientDetails: AuthClientDetails): BaseClientDetails {
    val baseClientDetails = BaseClientDetails()
    baseClientDetails.clientId = "copy-2"
    with(authClientDetails) {
      baseClientDetails.clientSecret = clientSecret
      baseClientDetails.setScope(scope)
      baseClientDetails.setResourceIds(resourceIds)
      baseClientDetails.setAuthorizedGrantTypes(authorizedGrantTypes)
      baseClientDetails.registeredRedirectUri = authClientDetails.registeredRedirectUri
      baseClientDetails.setAutoApproveScopes(autoApproveScopes)
      baseClientDetails.authorities = authorities
      baseClientDetails.accessTokenValiditySeconds = accessTokenValiditySeconds
      baseClientDetails.refreshTokenValiditySeconds = refreshTokenValiditySeconds
      baseClientDetails.additionalInformation = additionalInformation
    }
    return baseClientDetails
  }
}
