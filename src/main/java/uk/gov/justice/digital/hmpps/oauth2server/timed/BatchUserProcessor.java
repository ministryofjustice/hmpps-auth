package uk.gov.justice.digital.hmpps.oauth2server.timed;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

/**
 * Delete users that haven't logged into the system for a year.  Will only affect auth users that are disabled,
 * user data for other users will be removed if they haven't logged in for the last year too.
 */
@Log4j2
@AllArgsConstructor
class BatchUserProcessor {
    private final BatchUserService service;
    private final TelemetryClient telemetryClient;
    private final String telemetryPrefix;

    BatchState findAndProcessInBatches() {
        final var batch = new BatchState();

        do {
            try {
                batch.resetProcessedAndIncrementTotal(service.processInBatches());
            } catch (final Exception e) {
                // have to catch the exception here otherwise scheduling will stop
                log.error("Caught exception {} for {}", e.getClass().getSimpleName(), telemetryClient, e);
                batch.recordFailure();
                telemetryClient.trackEvent(String.format("%sError", telemetryPrefix), null, null);
            }

        } while (batch.shouldContinueProcessing());

        if (batch.hasInformationToReport()) {
            telemetryClient.trackEvent(String.format("%sFinished", telemetryPrefix),
                    Map.of("total", batch.getTotalAsString(), "errors", batch.getErrorCountAsString()), null);
        }
        return batch;
    }

    public static class BatchState {
        private int total;
        private int errorCount;
        private int processed;
        private boolean lastRunFailed;

        void resetProcessedAndIncrementTotal(final int newValue) {
            processed = newValue;
            total += processed;
            lastRunFailed = false;
        }

        void recordFailure() {
            errorCount++;
            lastRunFailed = true;
        }

        boolean shouldContinueProcessing() {
            // repositories return data in batches of 10
            return (processed >= 10 || lastRunFailed) && errorCount < 3;
        }

        boolean hasInformationToReport() {
            return total > 0 || errorCount > 0;
        }

        String getTotalAsString() {
            return String.valueOf(total);
        }

        String getErrorCountAsString() {
            return String.valueOf(errorCount);
        }
    }
}
