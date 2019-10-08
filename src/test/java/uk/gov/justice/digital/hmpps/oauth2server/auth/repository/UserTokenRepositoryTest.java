package uk.gov.justice.digital.hmpps.oauth2server.auth.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig;
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig;
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("dev")
@Import({AuthDbConfig.class, NomisDbConfig.class, FlywayConfig.class})
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Transactional(transactionManager = "authTransactionManager")
public class UserTokenRepositoryTest {
    @Autowired
    private UserTokenRepository repository;
    @Autowired
    private UserRepository userRepository;

    @Test
    public void givenATransientEntityItCanBePersisted() {

        final var user = transientUser();
        final var entity = user.createToken(TokenType.RESET);
        userRepository.save(user);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(entity.getToken()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findById(entity.getToken()).orElseThrow();

        // equals only compares the business key columns
        assertThat(retrievedEntity).isEqualTo(entity);

        assertThat(retrievedEntity.getToken()).isEqualTo(entity.getToken());
        assertThat(retrievedEntity.getTokenType()).isEqualTo(entity.getTokenType());
        assertThat(retrievedEntity.getTokenExpiry()).isEqualTo(entity.getTokenExpiry());
        assertThat(retrievedEntity.getUser()).isEqualTo(user);
    }

    @Test
    public void givenAnExistingUserTheyCanBeRetrieved() {
        final var retrievedEntity = repository.findById("reset").orElseThrow();
        assertThat(retrievedEntity.getTokenType()).isEqualTo(TokenType.RESET);
        assertThat(retrievedEntity.getTokenExpiry()).isEqualTo(LocalDateTime.of(2018, 12, 10, 8, 55, 45));
        assertThat(retrievedEntity.getUser().getUsername()).isEqualTo("LOCKED_USER");
    }

    private User transientUser() {
        return User.builder().username("userTokenRepository").email("a@b.com").build();
    }
}
