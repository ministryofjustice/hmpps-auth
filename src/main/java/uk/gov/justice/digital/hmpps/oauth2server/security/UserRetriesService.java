package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserRetries;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRetriesRepository;

import java.util.Optional;

@Service
@Slf4j
@Transactional
public class UserRetriesService {

    private final UserRetriesRepository userRetriesRepository;
    private final UserEmailRepository userEmailRepository;


    public UserRetriesService(final UserRetriesRepository userRetriesRepository, final UserEmailRepository userEmailRepository) {
        this.userRetriesRepository = userRetriesRepository;
        this.userEmailRepository = userEmailRepository;
    }

    public void resetRetries(final String username) {
        userRetriesRepository.save(new UserRetries(username, 0));
    }

    /**
     * @param username           unique identifier of user
     * @param existingRetryCount retry count from Oracle.  If we don't have a row for the user then copy that row over,
     *                           otherwise ignore and use the existing value
     * @return incremented retry value
     */
    public int incrementRetries(final String username, final int existingRetryCount) {
        final var retriesOptional = userRetriesRepository.findById(username);
        final UserRetries userRetries;
        if (retriesOptional.isEmpty()) {
            // no row exists, so create new from other table
            userRetries = new UserRetries(username, existingRetryCount + 1);
        } else {
            userRetries = retriesOptional.get();
            userRetries.incrementRetryCount();
        }
        userRetriesRepository.save(userRetries);
        return userRetries.getRetryCount();
    }

    public void lockAccount(final String username) {
        final Optional<UserEmail> userEmailOptional = userEmailRepository.findById(username);
        final UserEmail userEmail = userEmailOptional.orElseGet(() -> new UserEmail(username));
        userEmail.setLocked(true);
        userEmailRepository.save(userEmail);

        // reset retries otherwise if account is unlocked in c-nomis then user won't be allowed in
        resetRetries(username);
    }
}
