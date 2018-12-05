package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserRetries;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRetriesRepository;

@Service
@Slf4j
@Transactional
public class UserRetriesService {

    private final UserRetriesRepository repository;

    public UserRetriesService(final UserRetriesRepository repository) {
        this.repository = repository;
    }

    public void resetRetries(final String username) {
        repository.save(new UserRetries(username, 0));
    }

    /**
     * @param username           unique identifier of user
     * @param existingRetryCount retry count from Oracle.  If we don't have a row for the user then copy that row over,
     *                           otherwise ignore and use the existing value
     * @return incremented retry value
     */
    public int incrementRetries(final String username, final int existingRetryCount) {
        final var retriesOptional = repository.findById(username);
        final UserRetries userRetries;
        if (retriesOptional.isEmpty()) {
            // no row exists, so create new from other table
            userRetries = new UserRetries(username, existingRetryCount + 1);
        } else {
            userRetries = retriesOptional.get();
            userRetries.incrementRetryCount();
        }
        repository.save(userRetries);
        return userRetries.getRetryCount();
    }
}
