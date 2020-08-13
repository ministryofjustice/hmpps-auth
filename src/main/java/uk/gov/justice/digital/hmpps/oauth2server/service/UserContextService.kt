package uk.gov.justice.digital.hmpps.oauth2server.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.azure
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.delius
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.fromNullableString
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails

class UserMappingException(message: String): Exception(message)

@Service
class UserContextService {
  companion object {
    private val log = LoggerFactory.getLogger(UserContextService::class.java)
  }

  private val deliusScope = "delius"

  @Throws(UserMappingException::class)
  fun resolveUser(loginUser: UserPersonDetails, scopes: Set<String>): UserPersonDetails {
    if (scopes.contains(deliusScope)) {
      return map(loginUser.username, fromNullableString(loginUser.authSource), delius) ?: loginUser
    }

    return loginUser
  }

  private fun map(username: String, from: AuthSource, to: AuthSource): UserPersonDetails? = when (from) {
    to -> null
    azure -> mapFromAzure(username, to)
    else -> throw UserMappingException("auth source '${from}' not supported")
  }

  private fun mapFromAzure(username: String, to: AuthSource): UserPersonDetails? = when (to) {
    else -> throw UserMappingException("auth -> '${to}' mapping not supported")
  }
}
