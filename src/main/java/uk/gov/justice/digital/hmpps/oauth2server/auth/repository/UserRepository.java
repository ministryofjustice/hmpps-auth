package uk.gov.justice.digital.hmpps.oauth2server.auth.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends CrudRepository<User, String>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndMasterIsTrue(String username);

    List<User> findByEmail(String email);

    List<User> findByEmailAndMasterIsTrueOrderByUsername(String email);

    List<User> findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(LocalDateTime lastLoggedIn);

    List<User> findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(LocalDateTime lastLoggedIn);
}
