package uk.gov.justice.digital.hmpps.oauth2server.timed

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Delete users that haven't logged into the system for a year.  Will only affect auth users that are disabled,
 * user data for other users will be removed if they haven't logged in for the last year too.
 */
@Component
class DeleteDisabledUsers(service: DeleteDisabledUsersService, telemetryClient: TelemetryClient) :
  BatchUserProcessor(service, telemetryClient, "DeleteDisabledUsers") {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Scheduled(
    fixedDelayString = "\${application.authentication.delete.frequency}",
    initialDelayString = "\${random.int[600000,\${application.authentication.delete.frequency}]}"
  )
  fun findAndDeleteDisabledUsers() {
    log.info("Delete disabled users started")
    val state = findAndProcessInBatches()
    log.info(
      "Delete disabled users finished, processed {} records with {} errors",
      state.totalAsString,
      state.errorCountAsString
    )
  }
}
