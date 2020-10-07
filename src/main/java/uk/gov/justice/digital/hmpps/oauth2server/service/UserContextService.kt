package uk.gov.justice.digital.hmpps.oauth2server.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.auth
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.azuread
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.delius
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.fromNullableString
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.nomis
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.none
import uk.gov.justice.digital.hmpps.oauth2server.security.NomisUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails

class UserMappingException(message: String) : Exception(message)

@Service
class UserContextService(private val deliusUserService: DeliusUserService,
                         private val authUserService: AuthUserService,
                         private val nomisUserService: NomisUserService) {

  @Throws(UserMappingException::class)
  fun resolveUser(loginUser: UserPersonDetails, scopes: Set<String>): UserPersonDetails {
    val users = discoverUsers(loginUser, scopes)
    return when (users.size) {
      1 -> users[0]
      0 -> loginUser
      else -> throw UserMappingException("Multiple users found with scopes $scopes")
    }
  }

  private fun discoverUsers(loginUser: UserPersonDetails, scopes: Set<String>): List<UserPersonDetails> {
    val loginUserAuthSource = fromNullableString(loginUser.authSource)
    if (loginUserAuthSource != azuread) return listOf(loginUser)

    val desiredSources = scopes.map {
      try {
        fromNullableString(it)
      } catch (iae: IllegalArgumentException) {
        none
      }
    }.filter { it != none }
    if (desiredSources.isEmpty()) return listOf(loginUser)

    return desiredSources
        .map { mapFromAzureAD(loginUser.userId, it).filter { it.isEnabled } }
        .filter { it.isNotEmpty() }
        .flatten()
  }

  private fun mapFromAzureAD(email: String, to: AuthSource): List<UserPersonDetails> = when (to) {
    delius -> deliusUserService.getDeliusUsersByEmail(email)
    auth -> authUserService.findAuthUsersByEmail(email)
    nomis -> nomisUserService.getNomisUsersByEmail(email)
    else -> emptyList()
  }
}
