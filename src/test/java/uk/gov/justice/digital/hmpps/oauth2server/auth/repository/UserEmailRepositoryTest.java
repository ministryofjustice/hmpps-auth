package uk.gov.justice.digital.hmpps.oauth2server.auth.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail.TokenType;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
public class UserEmailRepositoryTest {
    @Autowired
    private UserEmailRepository repository;

    @Test
    public void givenATransientEntityItCanBePeristed() {

        final var transientEntity = transientEntity();

        final var entity = new UserEmail();
        entity.setUsername(transientEntity.getUsername());
        entity.setEmail(transientEntity.getEmail());
        entity.setToken(TokenType.RESET, transientEntity.getToken());

        final var persistedEntity = repository.save(entity);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getUsername()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findById(entity.getUsername()).orElseThrow();

        // equals only compares the business key columns
        assertThat(retrievedEntity).isEqualTo(transientEntity);

        assertThat(retrievedEntity.getUsername()).isEqualTo(transientEntity.getUsername());
        assertThat(retrievedEntity.getEmail()).isEqualTo(transientEntity.getEmail());
        assertThat(retrievedEntity.getToken()).isEqualTo(transientEntity.getToken());
        assertThat(retrievedEntity.getTokenType()).isEqualTo(transientEntity.getTokenType());
    }

    @Test
    public void givenAnExistingUserTheyCanBeRetrieved() {
        final var retrievedEntity = repository.findById("LOCKED_USER").orElseThrow();
        assertThat(retrievedEntity.getUsername()).isEqualTo("LOCKED_USER");
        assertThat(retrievedEntity.getEmail()).isEqualTo("locked@somewhere.com");
        assertThat(retrievedEntity.getToken()).isEqualTo("reset");
        assertThat(retrievedEntity.getTokenType()).isEqualTo(TokenType.RESET);
        assertThat(retrievedEntity.getTokenExpiry()).isEqualTo(LocalDateTime.of(2018, 12, 10, 8, 55, 45));
        assertThat(retrievedEntity.isVerified()).isTrue();
    }

    @Test
    public void testFindByTokenAndTokenType() {
        final var retrievedEntity = repository.findByTokenTypeAndToken(TokenType.RESET, "reset").orElseThrow();
        assertThat(retrievedEntity.getUsername()).isEqualTo("LOCKED_USER");
        assertThat(retrievedEntity.getEmail()).isEqualTo("locked@somewhere.com");
        assertThat(retrievedEntity.getToken()).isEqualTo("reset");
        assertThat(retrievedEntity.getTokenType()).isEqualTo(TokenType.RESET);
    }

    private UserEmail transientEntity() {
        final var email = new UserEmail();
        email.setUsername("user");
        email.setEmail("a@b.com");
        email.setToken(TokenType.RESET, "reset");
        return email;
    }
}
