package uk.gov.justice.digital.hmpps.oauth2server.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.auth
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.azuread
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.delius
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.nomis
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.none
import uk.gov.justice.digital.hmpps.oauth2server.security.NomisUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails

class UserMappingException(message: String) : Exception(message)

@Service
class UserContextService(
  private val deliusUserService: DeliusUserService,
  private val authUserService: AuthUserService,
  private val nomisUserService: NomisUserService,
) {

  fun discoverUsers(loginUser: UserPersonDetails, scopes: Set<String>): List<UserPersonDetails> =
    discoverUsers(AuthSource.fromNullableString(loginUser.authSource), loginUser.userId, scopes)

  fun discoverUsers(authSource: AuthSource, email: String, scopes: Set<String>): List<UserPersonDetails> {

    if (authSource != azuread) return emptyList()

    val sourcesFromScopes = scopes.map {
      try {
        AuthSource.fromNullableString(it)
      } catch (iae: IllegalArgumentException) {
        none
      }
    }.filter { it != none }

    val desiredSources = if (sourcesFromScopes.isEmpty()) listOf(nomis, auth, delius) else sourcesFromScopes

    return desiredSources
      .map { mapFromAzureAD(email, it).filter { it.isEnabled } }
      .filter { it.isNotEmpty() }
      .flatten()
  }

  private fun mapFromAzureAD(email: String, to: AuthSource): List<UserPersonDetails> = when (to) {
    delius -> deliusUserService.getDeliusUsersByEmail(email)
    auth -> authUserService.findAuthUsersByEmail(email).filter { it.isVerified }
    nomis -> nomisUserService.getNomisUsersByEmail(email)
    else -> emptyList()
  }
}
