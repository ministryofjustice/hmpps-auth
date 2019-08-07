package uk.gov.justice.digital.hmpps.oauth2server.timed;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRetriesRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@AllArgsConstructor
@Log4j2
public class DeleteDisabledUsersService implements BatchUserService {
    private final UserEmailRepository repository;
    private final UserRetriesRepository userRetriesRepository;
    private final UserTokenRepository userTokenRepository;
    private final TelemetryClient telemetryClient;

    @Transactional(transactionManager = "authTransactionManager")
    public int processInBatches() {
        final var usersToDelete = repository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(LocalDateTime.now().minusYears(1));
        usersToDelete.forEach(user -> {
            final var username = user.getUsername();
            userRetriesRepository.findById(username).ifPresent(userRetriesRepository::delete);
            userTokenRepository.findByUserEmail(user).forEach(userTokenRepository::delete);
            repository.delete(user);

            log.debug("Deleting auth user {} due to inactivity", username);
            telemetryClient.trackEvent("DeleteDisabledUsersProcessed", Map.of("username", username), null);
        });
        return usersToDelete.size();
    }
}
