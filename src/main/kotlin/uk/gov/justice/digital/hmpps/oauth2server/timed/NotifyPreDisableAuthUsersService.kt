package uk.gov.justice.digital.hmpps.oauth2server.timed

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException
import java.time.LocalDateTime
import java.util.function.Consumer

@Service
class NotifyPreDisableAuthUsersService(
  private val repository: UserRepository,
  private val notificationClient: NotificationClientApi,
  private val telemetryClient: TelemetryClient,
  @Value("\${application.authentication.notify-pre-disable.login-days}") private val loginDays: Int,
  @Value("\${application.notify.pre-disable-warning.template}") private val preDisableTemplateId: String,
  @Value("\${application.signin.url}") private val signinUrl: String,
  @Value("\${application.support.url}") private val support: String,
) : BatchUserService {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(transactionManager = "authTransactionManager")
  override fun processInBatches(): Int {
    val usersToNotifyPreDisable =
      repository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndPreDisableWarningIsFalseAndVerifiedIsTrueAndSourceOrderByUsername(
        LocalDateTime.now().minusDays(loginDays.toLong()).minusHours(1L),
        AuthSource.auth,
      )
    usersToNotifyPreDisable.forEach(
      Consumer { user: User ->
        user.preDisableWarning = true
        log.debug("Notifying auth user {} we will disable account in 7 days due to inactivity", user.username)
        try {
          notificationClient.sendEmail(
            preDisableTemplateId,
            user.email,
            mapOf(
              "firstName" to user.firstName,
              "username" to user.username,
              "signinUrl" to signinUrl,
              "support" to support,
            ),
            null
          )
          telemetryClient.trackEvent(
            "AuthNotifyPreDisableInactiveAuthUsersProcessed",
            mapOf("username" to user.username),
            null
          )
        } catch (e: NotificationClientException) {
          val reason = (e.cause?.let { e.cause } ?: e).javaClass.simpleName
          log.warn("Failed to send pre disable waring for user {}", user.username, e)
          telemetryClient.trackEvent(
            "AuthNotifyPreDisableInactiveAuthUsersFailure",
            mapOf("username" to user.username, "reason" to reason),
            null
          )
        }
      }
    )
    return usersToNotifyPreDisable.size
  }
}
