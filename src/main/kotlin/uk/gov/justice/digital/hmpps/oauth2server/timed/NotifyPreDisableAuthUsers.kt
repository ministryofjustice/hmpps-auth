package uk.gov.justice.digital.hmpps.oauth2server.timed

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Notify users that haven't logged into the system for 83 days their account will be disabled in 7 days
 */
@Component
class NotifyPreDisableAuthUsers(
  service: NotifyPreDisableAuthUsersService,
  telemetryClient: TelemetryClient,
  @Value("\${application.authentication.notify-pre-disable.enabled}") private val notificationEnabled: Boolean
) :
  BatchUserProcessor(service, telemetryClient, "NotifyPreDisableInactiveAuthUsers") {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Scheduled(
    fixedDelayString = "\${application.authentication.notify-pre-disable.frequency}",
    initialDelayString = "\${random.int[3600000,\${application.authentication.notify-pre-disable.frequency}]}"
  )
  fun findAndNotifyPreDisableInactiveAuthUsers() = // disabled in preprod so notification not sent
    if (notificationEnabled) {
      log.info("Notify PreDisable inactive auth users started")
      val state = findAndProcessInBatches()
      log.info(
        "Notify PreDisable inactive auth users finished, processed {} records with {} errors",
        state.totalAsString,
        state.errorCountAsString
      )
    } else {
      log.info("Notify PreDisable inactive auth users disabled in this environment")
    }
}
