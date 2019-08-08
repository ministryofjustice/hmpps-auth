package uk.gov.justice.digital.hmpps.oauth2server.timed;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Set users that haven't logged into the system for 90 days to be disabled
 */
@Component
@Log4j2
public class DisableInactiveAuthUsers extends BatchUserProcessor {
    public DisableInactiveAuthUsers(final DisableInactiveAuthUsersService service, final TelemetryClient telemetryClient) {
        super(service, telemetryClient, "DisableInactiveAuthUsers");
    }

    @Scheduled(
            fixedDelayString = "${application.authentication.disable.frequency}",
            initialDelayString = "${random.int[600000,${application.authentication.disable.frequency}]}")
    public void findAndDisableInactiveAuthUsers() {
        log.info("Disable inactive auth users started");

        final var state = findAndProcessInBatches();

        log.info("Disable inactive auth users finished, processed {} records with {} errors",
                state.getTotalAsString(), state.getErrorCountAsString());
    }
}
