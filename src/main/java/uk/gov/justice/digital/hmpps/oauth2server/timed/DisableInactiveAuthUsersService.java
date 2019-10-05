package uk.gov.justice.digital.hmpps.oauth2server.timed;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@AllArgsConstructor
@Log4j2
public class DisableInactiveAuthUsersService implements BatchUserService {
    private final UserRepository repository;
    private final TelemetryClient telemetryClient;

    @Transactional(transactionManager = "authTransactionManager")
    public int processInBatches() {
        final var usersToDisable = repository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(LocalDateTime.now().minusDays(90));
        usersToDisable.forEach(user -> {
            user.setEnabled(false);
            log.debug("Disabling auth user {} due to inactivity", user.getUsername());
            telemetryClient.trackEvent("DisableInactiveAuthUsersProcessed", Map.of("username", user.getUsername()), null);
        });
        return usersToDisable.size();
    }
}
