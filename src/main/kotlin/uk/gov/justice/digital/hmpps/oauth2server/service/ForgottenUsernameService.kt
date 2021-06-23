package uk.gov.justice.digital.hmpps.oauth2server.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.NomisUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException

@Service
class ForgottenUsernameService(
  private val deliusUserService: DeliusUserService,
  private val authUserService: AuthUserService,
  private val nomisUserService: NomisUserService,
  private val notificationClient: NotificationClientApi,
  @Value("\${application.notify.forgotten-username.template}") private val forgotTemplateId: String,

) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Throws(NotificationClientRuntimeException::class)
  fun forgottenUsername(email: String, url: String): List<String> {
    var firstname = ""
    val username = mutableListOf<String>()
    setOf(AuthSource.auth, AuthSource.nomis, AuthSource.delius)
      .map { it -> findUsernamesForEmail(email, it).filter { it.isEnabled } }
      .filter { it.isNotEmpty() }
      .flatten()
      .forEach {
        username.plusAssign(it.username)
        firstname = it.firstName
      }

    val signinUrl = url.replace("/forgotten-username", "/")
    // create map for email
    val parameters = mapOf(
      "firstName" to firstname,
      "username" to username,
      "signinUrl" to signinUrl,
      "single" to if (username.count() == 1) "yes" else "no",
      "multiple" to if (username.count() > 1) "yes" else "no"
    )

    if (username.isNotEmpty()) {
      // send email
      sendUsernameEmail(forgotTemplateId, parameters, email)
    }

    return username
  }

  @Throws(NotificationClientRuntimeException::class)
  private fun sendUsernameEmail(template: String, parameters: Map<String, Any?>, email: String?) {
    try {
      log.info("Sending forgotten username to notify for user {}", email)
      notificationClient.sendEmail(template, email, parameters, null)
    } catch (e: NotificationClientException) {
      log.warn("Failed to send forgotten username to notify for user {}", email, e)
      if (e.httpResult >= 500) {
        // second time lucky
        try {
          notificationClient.sendEmail(template, email, parameters, null)
        } catch (e1: NotificationClientException) {
          throw NotificationClientRuntimeException(e1)
        }
      }
      throw NotificationClientRuntimeException(e)
    }
  }

  private fun findUsernamesForEmail(email: String, to: AuthSource): List<UserPersonDetails> = when (to) {
    AuthSource.delius -> deliusUserService.getDeliusUsersByEmail(email)
    AuthSource.auth -> authUserService.findAuthUsersByEmail(email).filter { it.verified }
    AuthSource.nomis -> nomisUserService.getNomisUsersByEmail(email)
    else -> emptyList()
  }
}
