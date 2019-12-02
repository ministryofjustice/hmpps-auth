package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserRetries;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRetriesRepository;
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.service.DelegatingUserService;

import java.time.LocalDateTime;

@Service
@Slf4j
@Transactional(transactionManager = "authTransactionManager")
@AllArgsConstructor
public class UserRetriesService {

    private final UserRetriesRepository userRetriesRepository;
    private final UserRepository userRepository;
    private final DelegatingUserService delegatingUserService;

    private void resetRetries(final String username) {
        // reset their retry count
        userRetriesRepository.save(new UserRetries(username, 0));
    }

    public void resetRetriesAndRecordLogin(final UserPersonDetails userPersonDetails) {
        final var username = userPersonDetails.getUsername();
        resetRetries(username);

        // and record last logged in as now too (doing for all users to prevent confusion)
        final var userOptional = userRepository.findByUsername(username);
        final var user = userOptional.orElseGet(userPersonDetails::toUser);
        user.setLastLoggedIn(LocalDateTime.now());

        // copy across email address on each successful login
        if (userPersonDetails instanceof DeliusUserPersonDetails) {
            user.setEmail(((DeliusUserPersonDetails) userPersonDetails).getEmail());
            user.setVerified(true);
        }
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

    public void lockAccount(final UserPersonDetails userPersonDetails) {
        delegatingUserService.lockAccount(userPersonDetails);

        // reset retries otherwise if account is unlocked in c-nomis then user won't be allowed in
        resetRetries(userPersonDetails.getUsername());
    }
}
