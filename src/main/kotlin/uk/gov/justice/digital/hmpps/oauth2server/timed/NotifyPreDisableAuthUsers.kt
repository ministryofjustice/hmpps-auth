package uk.gov.justice.digital.hmpps.oauth2server.timed

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Set users that haven't logged into the system for 90 days to be disabled
 */
@Component
class NotifyPreDisableAuthUsers(service: NotifyPreDisableAuthUsersService, telemetryClient: TelemetryClient) :
  BatchUserProcessor(service, telemetryClient, "NotifyPreDisableInactiveAuthUsers") {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Scheduled(
    fixedDelayString = "\${application.authentication.disable.frequency}",
    initialDelayString = "\${random.int[3600000,\${application.authentication.notify-pre-disable.frequency}]}"
  )
  fun findAndNotifyPreDisableInactiveAuthUsers() {
    log.info("Notify PreDisable inactive auth users started")
    val state = findAndProcessInBatches()
    log.info(
      "Notify PreDisable inactive auth users finished, processed {} records with {} errors",
      state.totalAsString,
      state.errorCountAsString
    )
  }
}
