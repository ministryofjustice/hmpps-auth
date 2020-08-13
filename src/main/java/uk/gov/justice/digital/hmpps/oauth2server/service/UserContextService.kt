package uk.gov.justice.digital.hmpps.oauth2server.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails

class UserMappingException(message: String): Exception(message)

@Service
class UserContextService {
  companion object {
    private val log = LoggerFactory.getLogger(UserContextService::class.java)
  }

  @Throws(UserMappingException::class)
  fun getUser(loginUser: UserPersonDetails, scopes: Set<String>): UserPersonDetails? {
    if (scopes.contains("delius")) {
      return map(loginUser.username, loginUser.authSource, "delius")
    }

    return loginUser
  }

  private fun map(username: String, from: String, to: String): UserPersonDetails? = when (from) {
    to -> null
    "azure" -> mapFromAzure(username, to)
    else -> throw UserMappingException("auth source '${from}' not supported")
  }

  private fun mapFromAzure(username: String, to: String): UserPersonDetails? = when (to) {
    else -> throw UserMappingException("auth -> '${to}' mapping not supported")
  }
}
