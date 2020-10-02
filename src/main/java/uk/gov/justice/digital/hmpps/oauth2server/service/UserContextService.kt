package uk.gov.justice.digital.hmpps.oauth2server.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.azuread
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.delius
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.fromNullableString
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails

class UserMappingException(message: String): Exception(message)

@Service
class UserContextService(
    private val deliusUserService: DeliusUserService
) {
  companion object {
    private val log = LoggerFactory.getLogger(UserContextService::class.java)
  }

  private val deliusScope = "delius"

  @Throws(UserMappingException::class)
  fun resolveUser(loginUser: UserPersonDetails, scopes: Set<String>): UserPersonDetails {
    if (scopes.contains(deliusScope)) {
      return map(loginUser, fromNullableString(loginUser.authSource), delius)
    }

    return loginUser
  }

  private fun map(user: UserPersonDetails, from: AuthSource, to: AuthSource): UserPersonDetails = when (from) {
    to -> user
    azuread -> mapFromAzureAD(user, to)
    else -> throw UserMappingException("auth source '${from}' not supported")
  }

  private fun mapFromAzureAD(user: UserPersonDetails, to: AuthSource): UserPersonDetails = when (to) {
    delius -> {
      log.debug("mapping user context from azure -> delius")
      deliusUserService.getDeliusUserByEmail(user.userId) ?: user
    }
    else -> throw UserMappingException("auth -> '${to}' mapping not supported")
  }
}
