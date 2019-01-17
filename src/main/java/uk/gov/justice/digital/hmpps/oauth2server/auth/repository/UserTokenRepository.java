package uk.gov.justice.digital.hmpps.oauth2server.auth.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;

import java.util.Optional;

public interface UserTokenRepository extends CrudRepository<UserToken, String> {
    Optional<UserToken> findByTokenTypeAndUserEmail(TokenType tokenType, UserEmail userEmail);
}
