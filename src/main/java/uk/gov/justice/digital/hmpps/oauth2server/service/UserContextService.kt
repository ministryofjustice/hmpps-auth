package uk.gov.justice.digital.hmpps.oauth2server.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.auth
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.azuread
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.delius
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.nomis
import uk.gov.justice.digital.hmpps.oauth2server.security.NomisUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails

class UserMappingException(message: String) : Exception(message)

@Service
class UserContextService(
  private val deliusUserService: DeliusUserService,
  private val authUserService: AuthUserService,
  private val nomisUserService: NomisUserService,
) {

  fun discoverUsers(loginUser: UserPersonDetails, scopes: Set<String>): List<UserPersonDetails> {
    // if specific accounts are requested via scopes, attempt to find just those.
    // otherwise, attempt to find all accounts.
    val requestedSources = AuthSource.values()
      .filter { it.name in scopes }
      .let { if (it.isEmpty()) setOf(auth, nomis, delius) else it }

    return when (AuthSource.fromNullableString(loginUser.authSource)) {
      azuread -> {
        requestedSources
          .map { mapFromAzureAD(loginUser.userId, it).filter { it.isEnabled } }
          .filter { it.isNotEmpty() }
          .flatten()
      }
      else -> emptyList()
    }
  }

  private fun mapFromAzureAD(email: String, to: AuthSource): List<UserPersonDetails> = when (to) {
    delius -> deliusUserService.getDeliusUsersByEmail(email)
    auth -> authUserService.findAuthUsersByEmail(email).filter { it.isVerified }
    nomis -> nomisUserService.getNomisUsersByEmail(email)
    else -> emptyList()
  }
}
