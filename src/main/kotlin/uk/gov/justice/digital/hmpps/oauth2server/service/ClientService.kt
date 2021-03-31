@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.service

import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Client
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.ClientRepository
import uk.gov.justice.digital.hmpps.oauth2server.security.PasswordGenerator

@Service
class ClientService(
  private val clientsDetailsService: JdbcClientDetailsService,
  private val passwordGenerator: PasswordGenerator,
  private val clientRepository: ClientRepository,
) {

  fun findAndUpdateDuplicates(clientId: String) {
    val clientDetails = clientsDetailsService.loadClientByClientId(clientId)
    find(clientId).filter { it.id != clientId }
      .map { copyClient(it.id, clientDetails as BaseClientDetails) }
      .forEach { clientsDetailsService.updateClientDetails(it) }
  }

  private fun find(clientId: String): List<Client> {
    val searchClientId = baseClientId(clientId)
    return clientRepository.findByIdStartsWith(searchClientId)
  }

  private fun copyClient(clientId: String, clientDetails: BaseClientDetails): BaseClientDetails {
    val client = BaseClientDetails(clientDetails)
    client.clientId = clientId
    // copy constructor doesn't copy all the fields over so need to copy the extra ones
    client.additionalInformation = clientDetails.additionalInformation
    client.setAutoApproveScopes(clientDetails.autoApproveScopes)
    return client
  }

  @Throws(DuplicateClientsException::class)
  fun duplicateClient(clientId: String): ClientDetails {
    val clientDetails = clientsDetailsService.loadClientByClientId(clientId)
    val duplicateClientDetails = copyClient(incrementClientId(clientId), clientDetails as BaseClientDetails)
    duplicateClientDetails.clientSecret = passwordGenerator.generatePassword()
    clientsDetailsService.addClientDetails(duplicateClientDetails)
    return duplicateClientDetails
  }

  @Throws(DuplicateClientsException::class)
  private fun incrementClientId(clientId: String): String {
    val clients = find(clientId)
    if (clients.size > 2) {
      throw DuplicateClientsException(clientId, "MaxReached")
    }

    val baseClientId = baseClientId(clientId)
    val ids = clients.map {
      clientNumber(it.id)
    }

    val increment = ids.maxOrNull()?.plus(1)

    return "$baseClientId-$increment"
  }

  private fun baseClientId(clientId: String): String = clientId.replace(regex = "-[0-9]*$".toRegex(), replacement = "")
  private fun clientNumber(clientId: String): Int = clientId.substringAfterLast("-").toIntOrNull() ?: 0
}

open class DuplicateClientsException(clientId: String, errorCode: String) :
  Exception("Duplicate clientId failed for $clientId with reason: $errorCode")
