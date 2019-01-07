package uk.gov.justice.digital.hmpps.oauth2server.auth.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail.TokenType;

import java.util.Optional;

public interface UserEmailRepository extends CrudRepository<UserEmail, String> {
    Optional<UserEmail> findByTokenTypeAndToken(final TokenType tokenType, final String token);
}
