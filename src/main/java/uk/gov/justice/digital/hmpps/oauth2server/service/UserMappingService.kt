package uk.gov.justice.digital.hmpps.oauth2server.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails

class UserMappingException(message: String): Exception(message)

@Service
class UserMappingService {
    companion object {
        private val log = LoggerFactory.getLogger(UserMappingService::class.java)
    }

    @Throws(UserMappingException::class)
    fun map(username: String, from: String, to: String): UserPersonDetails? = when (from) {
        "azure" -> mapFromAzure(username, to)
        else -> throw UserMappingException("auth source '${from}' not supported")
    }

    private fun mapFromAzure(username: String, to: String): UserPersonDetails? = when (to) {
        else -> throw UserMappingException("auth -> '${to}' mapping not supported")

    }
}
