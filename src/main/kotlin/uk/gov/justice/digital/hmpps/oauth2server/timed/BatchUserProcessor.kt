package uk.gov.justice.digital.hmpps.oauth2server.timed

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory

/**
 * Delete users that haven't logged into the system for a year.  Will only affect auth users that are disabled,
 * user data for other users will be removed if they haven't logged in for the last year too.
 */
open class BatchUserProcessor(
  private val service: BatchUserService,
  private val telemetryClient: TelemetryClient,
  private val telemetryPrefix: String,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun findAndProcessInBatches(): BatchState {
    val batch = BatchState()
    do {
      try {
        batch.resetProcessedAndIncrementTotal(service.processInBatches())
      } catch (e: Exception) {
        // have to catch the exception here otherwise scheduling will stop
        log.error("Caught exception {} for {}", e.javaClass.simpleName, telemetryPrefix, e)
        batch.recordFailure()
        telemetryClient.trackEvent(String.format("%sError", telemetryPrefix), null, null)
      }
    } while (batch.shouldContinueProcessing())
    if (batch.hasInformationToReport()) {
      telemetryClient.trackEvent(
        String.format("%sFinished", telemetryPrefix),
        mapOf("total" to batch.totalAsString, "errors" to batch.errorCountAsString),
        null
      )
    }
    return batch
  }

  class BatchState(
    private var total: Int = 0,
    var errorCount: Int = 0,
    var processed: Int = 0,
    private var lastRunFailed: Boolean = false,
  ) {

    fun resetProcessedAndIncrementTotal(newValue: Int) {
      processed = newValue
      total += processed
      lastRunFailed = false
    }

    fun recordFailure() {
      errorCount++
      lastRunFailed = true
    }

    fun shouldContinueProcessing(): Boolean = // repositories return data in batches of 10
      (processed >= 10 || lastRunFailed) && errorCount < 3

    fun hasInformationToReport(): Boolean = total > 0 || errorCount > 0

    val totalAsString: String
      get() = total.toString()
    val errorCountAsString: String
      get() = errorCount.toString()
  }
}
