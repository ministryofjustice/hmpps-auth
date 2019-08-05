package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserRetries;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRetriesRepository;

import java.time.LocalDateTime;

@Service
@Slf4j
@Transactional(transactionManager = "authTransactionManager")
public class UserRetriesService {

    private final UserRetriesRepository userRetriesRepository;
    private final UserEmailRepository userEmailRepository;
    private final AlterUserService alterUserService;


    public UserRetriesService(final UserRetriesRepository userRetriesRepository, final UserEmailRepository userEmailRepository, final AlterUserService alterUserService) {
        this.userRetriesRepository = userRetriesRepository;
        this.userEmailRepository = userEmailRepository;
        this.alterUserService = alterUserService;
    }

    private void resetRetries(final String username) {
        // reset their retry count
        userRetriesRepository.save(new UserRetries(username, 0));
    }

    public void resetRetriesAndRecordLogin(final String username) {
        resetRetries(username);

        // and record last logged in as now too (doing for all users to prevent confusion)
        final var userEmailOptional = userEmailRepository.findById(username);
        final var userEmail = userEmailOptional.orElseGet(() -> UserEmail.of(username));
        userEmail.setLastLoggedIn(LocalDateTime.now());
        userEmailRepository.save(userEmail);
    }

    /**
     * @param username unique identifier of user
     * @return incremented retry value
     */
    public int incrementRetries(final String username) {
        final var retriesOptional = userRetriesRepository.findById(username);
        final var userRetries = retriesOptional.orElse(new UserRetries(username, 0));
        userRetries.incrementRetryCount();
        userRetriesRepository.save(userRetries);
        return userRetries.getRetryCount();
    }

    public void lockAccount(final String username) {
        final var userEmailOptional = userEmailRepository.findById(username);
        final var userEmail = userEmailOptional.orElseGet(() -> UserEmail.of(username));
        userEmail.setLocked(true);
        userEmailRepository.save(userEmail);

        // if auth isn't the master of the data then call to oracle to lock the user account
        if (!userEmail.isMaster()) {
            alterUserService.lockAccount(username);
        }

        // reset retries otherwise if account is unlocked in c-nomis then user won't be allowed in
        resetRetries(username);
    }
}
