package uk.gov.justice.digital.hmpps.oauth2server.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService

@Service
class UserMappingService(
        private val authUserService: AuthUserService,
        private val deliusUserService: DeliusUserService
    ) {

    companion object {
        private val log = LoggerFactory.getLogger(UserMappingService::class.java)
    }

    fun map(username: String, from: String, to: String) = when (from) {
        "auth" -> mapFromAuth(username, to)
        "azure" -> mapFromAzure(username, to)
        else -> username
    }

    private fun mapFromAuth(username: String, to: String) = when (to) {
        "delius" -> {
            val email = authUserService.getAuthUserByUsername(username)
            log.debug("email for authuser {username}: {email}")

            // fixme:
            val deliusUsername = "TOMMYERSDEV"
            // val deliusUsername = deliusUserService.getDeliusUserByEmail(email)

            log.debug("mapped username from 'auth' -> 'delius': {username} -> {deliusUsername}")
            deliusUsername
        }
        else -> username
    }

    private fun mapFromAzure(username: String, to: String) = when (to) {
        "delius" -> {
            // fixme:
            val deliusUsername = "TOMMYERSDEV"
            // deliusUserService.getDeliusUserByEmail(username)

            log.debug("mapped username from 'azure' -> 'delius': {username} -> {deliusUsername}")
            username
        }
        else -> username
    }
}
