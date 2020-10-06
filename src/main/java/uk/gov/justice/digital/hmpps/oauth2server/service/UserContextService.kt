package uk.gov.justice.digital.hmpps.oauth2server.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.auth
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.azuread
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.delius
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.fromNullableString
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails

class UserMappingException(message: String) : Exception(message)

@Service
class UserContextService(
    private val deliusUserService: DeliusUserService,
    private val authUserService: AuthUserService) {

  @Throws(UserMappingException::class)
  fun resolveUser(loginUser: UserPersonDetails, scopes: Set<String>): UserPersonDetails {
    val users = discoverUsers(loginUser, scopes)
    return when (users.size) {
      1 -> users[0]
      0 -> loginUser
      else -> throw UserMappingException("Multiple users found with scopes $scopes")
    }
  }

  fun discoverUsers(loginUser: UserPersonDetails, scopes: Set<String>): List<UserPersonDetails> {
    val loginUserAuthSource = fromNullableString(loginUser.authSource)
    if (loginUserAuthSource != azuread) return listOf(loginUser)

    val desiredSources = scopes.map { fromNullableString(it) }.filter { it != AuthSource.none }
    if (desiredSources.isEmpty()) return listOf(loginUser)

    return desiredSources
        .map { mapFromAzureAD(loginUser, it) }
        .filter { it.isNotEmpty() }
        .flatten()
  }

  private fun mapFromAzureAD(user: UserPersonDetails, to: AuthSource): List<UserPersonDetails> = when (to) {
    delius -> deliusUserService.getDeliusUsersByEmail(user.userId)
    auth -> authUserService.findAuthUsersByEmail(user.userId)
    else -> emptyList()
  }
}
