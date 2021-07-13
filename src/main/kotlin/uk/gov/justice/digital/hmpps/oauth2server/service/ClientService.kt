@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.oauth2.provider.ClientAlreadyExistsException
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.ClientDetailsService
import org.springframework.security.oauth2.provider.ClientRegistrationService
import org.springframework.security.oauth2.provider.NoSuchClientException
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Client
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ClientDeployment
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ClientType
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.ClientDeploymentRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.ClientRepository
import uk.gov.justice.digital.hmpps.oauth2server.security.PasswordGenerator
import uk.gov.justice.digital.hmpps.oauth2server.service.SortBy.count
import uk.gov.justice.digital.hmpps.oauth2server.service.SortBy.lastAccessed
import uk.gov.justice.digital.hmpps.oauth2server.service.SortBy.secretUpdated
import uk.gov.justice.digital.hmpps.oauth2server.service.SortBy.team
import uk.gov.justice.digital.hmpps.oauth2server.service.SortBy.type
import java.time.LocalDateTime

@Service
class ClientService(
  private val clientsDetailsService: ClientDetailsService,
  private val clientRegistrationService: ClientRegistrationService,
  private val passwordGenerator: PasswordGenerator,
  private val clientRepository: ClientRepository,
  private val clientDeploymentRepository: ClientDeploymentRepository,
) {

  @Throws(ClientAlreadyExistsException::class)
  fun addClient(clientDetails: ClientDetails): String {
    clientRegistrationService.addClientDetails(clientDetails)
    val clientSecret = passwordGenerator.generatePassword()
    clientRegistrationService.updateClientSecret(clientDetails.clientId, clientSecret)
    return clientSecret
  }

  fun findAndUpdateDuplicates(clientId: String) {
    val clientDetails = clientsDetailsService.loadClientByClientId(clientId)
    find(clientId).filter { it.id != clientId }
      .map { copyClient(it.id, clientDetails as BaseClientDetails) }
      .forEach { clientRegistrationService.updateClientDetails(it) }
  }

  fun listUniqueClients(sortBy: SortBy, filterBy: ClientFilter?): List<ClientSummary> {
    val baseClients = clientRepository.findAll().groupBy { baseClientId(it.id) }.toSortedMap()
    val deployments = clientDeploymentRepository.findAll().associateBy { it.baseClientId }
    return baseClients.toList().map {
      val deployment: ClientDeployment? = deployments[it.first]
      val firstClient = it.second[0]
      val lastAccessed = it.second.map { it.lastAccessed }.maxOrNull()
      val secretUpdated = it.second.map { it.secretUpdated }.maxOrNull()
      ClientSummary(
        baseClientId = it.first,
        clientType = deployment?.type,
        teamName = deployment?.team,
        grantTypes = firstClient.authorizedGrantTypes.sorted().joinToString("\n"),
        roles = firstClient.authoritiesWithoutPrefix.sorted().joinToString("\n"),
        lastAccessed = lastAccessed,
        secretUpdated = secretUpdated,
        count = it.second.size
      )
    }.filter { cs ->
      filterBy?.let { filter ->
        (filter.clientType == null || filter.clientType == cs.clientType) &&
          (filter.grantType.isNullOrBlank() || cs.grantTypes.contains(filter.grantType)) &&
          (filter.role.isNullOrBlank() || cs.roles.contains(filter.role.uppercase()))
      } ?: true
    }
      .sortedWith(
        compareBy {
          when (sortBy) {
            type -> it.clientType
            team -> it.teamName
            count -> it.count
            lastAccessed -> it.lastAccessed
            secretUpdated -> it.secretUpdated
            else -> it.baseClientId
          }
        }
      )
  }

  fun isValid(url: String): Boolean {
    val (urlWithSlash, urlWithoutSlash) =
      if (url.endsWith("/")) url to url.subSequence(0, url.length - 1)
      else "$url/" to url

    return clientRepository.findAll()
      .flatMap { it.webServerRedirectUri }
      .any { it == urlWithSlash || it == urlWithoutSlash }
  }

  fun loadClientWithCopies(baseClientId: String): ClientDetailsWithCopies {
    val clients = find(baseClientId)
    return ClientDetailsWithCopies(clientsDetailsService.loadClientByClientId(clients.first().id), clients)
  }

  fun loadClientAndDeployment(clientId: String): ClientDuplicateIdsAndDeployment {

    val clientIds = find(clientId).map { it.id }

    if (clientIds.isEmpty()) {
      throw NoSuchClientException("No client with requested id: $clientId")
    }

    return ClientDuplicateIdsAndDeployment(
      clientId, clientIds, loadClientDeploymentDetails(baseClientId(clientId))
    )
  }

  private fun find(clientId: String): List<Client> {
    val searchClientId = baseClientId(clientId)
    return clientRepository.findByIdStartsWithOrderById(searchClientId)
      .filter { it.id == searchClientId || it.id.substringAfter(searchClientId).matches(clientIdSuffixRegex) }
  }

  fun loadClientDeploymentDetails(clientId: String): ClientDeployment? {
    val searchClientId = baseClientId(clientId)
    return clientDeploymentRepository.findByIdOrNull(searchClientId)
  }

  fun getClientDeploymentDetailsAndBaseClientId(clientId: String): Pair<ClientDeployment?, String> {
    val baseClientId = baseClientId(clientId)
    return Pair(clientDeploymentRepository.findByIdOrNull(baseClientId), baseClientId)
  }

  @Transactional(transactionManager = "authTransactionManager")
  fun saveClientDeploymentDetails(clientDeployment: ClientDeployment) {
    clientDeploymentRepository.save(clientDeployment)
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(NoSuchClientException::class)
  fun removeClient(clientId: String) {
    val clients = find(clientId)
    if (clients.size == 1) {
      val baseClientId = baseClientId(clientId)
      clientDeploymentRepository.deleteByBaseClientId(baseClientId)
    }
    clientRegistrationService.removeClientDetails(clientId)
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
    val clientIdFromDB = find(clientId).map { it.id }
    if (clientIdFromDB.isEmpty()) {
      throw NoSuchClientException("No client with requested id: $clientId")
    }

    val clientDetails = clientsDetailsService.loadClientByClientId(clientIdFromDB.last())
    val duplicateClientDetails =
      copyClient(incrementClientId(clientIdFromDB.last()), clientDetails as BaseClientDetails)
    duplicateClientDetails.clientSecret = passwordGenerator.generatePassword()
    clientRegistrationService.addClientDetails(duplicateClientDetails)
    return duplicateClientDetails
  }

  @Throws(DuplicateClientsException::class)
  private fun incrementClientId(clientId: String): String {
    val clients = find(clientId)
    val baseClientId = baseClientId(clientId)
    if (clients.size > 2) {
      throw DuplicateClientsException(baseClientId, "MaxReached")
    }

    val ids = clients.map {
      clientNumber(it.id)
    }

    val increment = ids.maxOrNull()?.plus(1)

    return "$baseClientId-$increment"
  }

  fun baseClientId(clientId: String): String = clientId.replace(regex = clientIdSuffixRegex, replacement = "")
  private fun clientNumber(clientId: String): Int = clientId.substringAfterLast("-").toIntOrNull() ?: 0

  private val clientIdSuffixRegex = "-[0-9]*$".toRegex()
}

data class ClientDetailsWithCopies(val clientDetails: ClientDetails, val duplicates: List<Client>)
data class ClientDuplicateIdsAndDeployment(
  val requestedClientId: String,
  val duplicates: List<String>,
  val clientDeployment: ClientDeployment?
)

data class ClientSummary(
  val baseClientId: String,
  val clientType: ClientType?,
  val teamName: String?,
  val grantTypes: String,
  val roles: String,
  val lastAccessed: LocalDateTime?,
  val secretUpdated: LocalDateTime?,
  val count: Int,
)

enum class SortBy {
  client, type, team, lastAccessed, secretUpdated, count
}

data class ClientFilter(
  val grantType: String? = null,
  val role: String? = null,
  val clientType: ClientType? = null,
)

open class DuplicateClientsException(baseClientId: String, errorCode: String) :
  Exception("Duplicate clientId failed for baseClientId: $baseClientId with reason: $errorCode")
