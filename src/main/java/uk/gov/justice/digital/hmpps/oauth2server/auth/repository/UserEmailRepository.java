package uk.gov.justice.digital.hmpps.oauth2server.auth.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;

public interface UserEmailRepository extends CrudRepository<UserEmail, String> {
}
