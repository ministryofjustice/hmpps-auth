package uk.gov.justice.digital.hmpps.oauth2server.auth.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;

import java.util.List;
import java.util.Optional;

public interface UserEmailRepository extends CrudRepository<UserEmail, String> {
    Optional<UserEmail> findByUsernameAndMasterIsTrue(String username);

    List<UserEmail> findByEmail(String email);

    List<UserEmail> findByEmailAndMasterIsTrueOrderByUsername(String email);

}
