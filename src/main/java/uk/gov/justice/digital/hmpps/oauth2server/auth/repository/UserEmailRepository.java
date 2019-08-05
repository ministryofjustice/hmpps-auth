package uk.gov.justice.digital.hmpps.oauth2server.auth.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserEmailRepository extends CrudRepository<UserEmail, String>, JpaSpecificationExecutor<UserEmail> {
    Optional<UserEmail> findByUsernameAndMasterIsTrue(String username);

    List<UserEmail> findByEmail(String email);

    List<UserEmail> findByEmailAndMasterIsTrueOrderByUsername(String email);

    List<UserEmail> findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(LocalDateTime lastLoggedIn);
}
