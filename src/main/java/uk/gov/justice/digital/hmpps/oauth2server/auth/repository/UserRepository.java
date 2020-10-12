package uk.gov.justice.digital.hmpps.oauth2server.auth.repository;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends CrudRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndSource(String username, AuthSource source);

    default Optional<User> findByUsernameAndMasterIsTrue(final String username) {
        return findByUsernameAndSource(username, AuthSource.auth);
    }

    List<User> findByEmail(String email);

    List<User> findByEmailAndSourceOrderByUsername(String email, AuthSource source);

    default List<User> findByEmailAndMasterIsTrueOrderByUsername(final String username) {
        return findByEmailAndSourceOrderByUsername(username, AuthSource.auth);
    }

    List<User> findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndSourceOrderByLastLoggedIn(LocalDateTime lastLoggedIn, AuthSource source);

    default List<User> findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(final LocalDateTime lastLoggedIn) {
        return findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndSourceOrderByLastLoggedIn(lastLoggedIn, AuthSource.auth);
    }

    List<User> findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(LocalDateTime lastLoggedIn);

    List<User> findByUsernameIn(@NotNull List<String> usernames);
}
