package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserRetries;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRetriesRepository;

@Service
@Slf4j
@Transactional
public class UserRetriesService {

    private final UserRetriesRepository userRetriesRepository;
    private final UserEmailRepository userEmailRepository;
    private final AlterUserService alterUserService;


    public UserRetriesService(final UserRetriesRepository userRetriesRepository, final UserEmailRepository userEmailRepository, final AlterUserService alterUserService) {
        this.userRetriesRepository = userRetriesRepository;
        this.userEmailRepository = userEmailRepository;
        this.alterUserService = alterUserService;
    }

    public void resetRetries(final String username) {
        userRetriesRepository.save(new UserRetries(username, 0));
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
        final var userEmail = userEmailOptional.orElseGet(() -> new UserEmail(username));
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
