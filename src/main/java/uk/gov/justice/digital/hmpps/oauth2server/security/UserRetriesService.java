package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserRetries;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRetriesRepository;

import java.time.LocalDateTime;

@Service
@Slf4j
@Transactional(transactionManager = "authTransactionManager")
public class UserRetriesService {

    private final UserRetriesRepository userRetriesRepository;
    private final UserRepository userRepository;
    private final AlterUserService alterUserService;


    public UserRetriesService(final UserRetriesRepository userRetriesRepository, final UserRepository userRepository, final AlterUserService alterUserService) {
        this.userRetriesRepository = userRetriesRepository;
        this.userRepository = userRepository;
        this.alterUserService = alterUserService;
    }

    private void resetRetries(final String username) {
        // reset their retry count
        userRetriesRepository.save(new UserRetries(username, 0));
    }

    public void resetRetriesAndRecordLogin(final String username) {
        resetRetries(username);

        // and record last logged in as now too (doing for all users to prevent confusion)
        final var userOptional = userRepository.findByUsername(username);
        final var user = userOptional.orElseGet(() -> User.of(username));
        user.setLastLoggedIn(LocalDateTime.now());
        userRepository.save(user);
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
        final var userOptional = userRepository.findByUsername(username);
        final var user = userOptional.orElseGet(() -> User.of(username));
        user.setLocked(true);
        userRepository.save(user);

        // if auth isn't the master of the data then call to oracle to lock the user account
        if (!user.isMaster()) {
            alterUserService.lockAccount(username);
        }

        // reset retries otherwise if account is unlocked in c-nomis then user won't be allowed in
        resetRetries(username);
    }
}
