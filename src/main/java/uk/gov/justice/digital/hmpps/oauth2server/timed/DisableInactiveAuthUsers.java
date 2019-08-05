package uk.gov.justice.digital.hmpps.oauth2server.timed;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Log4j2
@AllArgsConstructor
public class DisableInactiveAuthUsers {
    private final DisableInactiveAuthUsersService service;
    private final TelemetryClient telemetryClient;

    @Scheduled(
            fixedDelayString = "${application.authentication.disable.frequency}",
            initialDelayString = "${random.int[0,${application.authentication.disable.frequency}]}")
    public void findAndDisableInactiveAuthUsers() {
        int processed = 0, total = 0, errorCount = 0;
        boolean lastRunFailed;

        log.info("Disable inactive auth users started");

        do {
            try {
                processed = service.findAndDisableInactiveAuthUsers();
                total += processed;
                lastRunFailed = false;
            } catch (final Exception e) {
                // have to catch the exception here otherwise scheduling will stop
                log.error("Caught exception {} whilst trying to disable users", e.getClass().getSimpleName(), e);
                errorCount++;
                telemetryClient.trackEvent("DisableInactiveAuthUsersError", null, null);
                lastRunFailed = true;
            }

        } while ((processed > 9 || lastRunFailed) && errorCount < 3);

        log.info("Disable inactive auth users finished, processed {} records with {} errors", total, errorCount);
        if (total > 0 || errorCount > 0) {
            telemetryClient.trackEvent("DisableInactiveAuthUsersFinished", Map.of("total", String.valueOf(total), "errors", String.valueOf(errorCount)), null);
        }
    }
}
