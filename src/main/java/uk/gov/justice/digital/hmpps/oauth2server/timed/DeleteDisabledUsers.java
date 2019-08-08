package uk.gov.justice.digital.hmpps.oauth2server.timed;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Delete users that haven't logged into the system for a year.  Will only affect auth users that are disabled,
 * user data for other users will be removed if they haven't logged in for the last year too.
 */
@Component
@Log4j2
public class DeleteDisabledUsers extends BatchUserProcessor {
    public DeleteDisabledUsers(final DeleteDisabledUsersService service, final TelemetryClient telemetryClient) {
        super(service, telemetryClient, "DeleteDisabledUsers");
    }

    @Scheduled(
            fixedDelayString = "${application.authentication.delete.frequency}",
            initialDelayString = "${random.int[600000,${application.authentication.delete.frequency}]}")
    public void findAndDeleteDisabledUsers() {
        log.info("Delete disabled users started");

        final var state = findAndProcessInBatches();

        log.info("Delete disabled users finished, processed {} records with {} errors",
                state.getTotalAsString(), state.getErrorCountAsString());
    }
}
