package uk.gov.justice.digital.hmpps.oauth2server.auth.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
public class UserEmailRepositoryTest {
    @Autowired
    private UserEmailRepository repository;

    @Test
    public void givenATransientEntityItCanBePersisted() {

        final var transientEntity = transientEntity();

        final var entity = new UserEmail();
        entity.setUsername(transientEntity.getUsername());
        entity.setEmail(transientEntity.getEmail());

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
    }

    @Test
    public void givenATransientAuthEntityItCanBePersisted() {

        final var transientEntity = transientEntity();
        transientEntity.setPerson(new Person(transientEntity.getUsername(), "first", "last"));
        transientEntity.setAuthorities(Set.of(new Authority("AUTH_1"), new Authority("AUTH_2")));

        final var persistedEntity = repository.save(transientEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getUsername()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findById(transientEntity.getUsername()).orElseThrow();

        // equals only compares the business key columns
        assertThat(retrievedEntity).isEqualTo(transientEntity);
        assertThat(retrievedEntity.getUsername()).isEqualTo(transientEntity.getUsername());
        assertThat(retrievedEntity.getEmail()).isEqualTo(transientEntity.getEmail());
        assertThat(retrievedEntity.getName()).isEqualTo("first last");
        assertThat(retrievedEntity.getAuthorities()).extracting("authority").containsOnly("AUTH_1", "AUTH_2");
    }

    @Test
    public void persistUserWithoutEmail() {
        final var transientEntity = new UserEmail("userb");
        final var persistedEntity = repository.save(transientEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getUsername()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findById(transientEntity.getUsername()).orElseThrow();

        // equals only compares the business key columns
        assertThat(retrievedEntity).isEqualTo(transientEntity);

        assertThat(retrievedEntity.getUsername()).isEqualTo(transientEntity.getUsername());
        assertThat(retrievedEntity.getEmail()).isNull();
    }

    @Test
    public void givenAnExistingUserTheyCanBeRetrieved() {
        final var retrievedEntity = repository.findById("LOCKED_USER").orElseThrow();
        assertThat(retrievedEntity.getUsername()).isEqualTo("LOCKED_USER");
        assertThat(retrievedEntity.getEmail()).isEqualTo("locked@somewhere.com");
        assertThat(retrievedEntity.isVerified()).isTrue();
    }

    @Test
    public void givenAnExistingAuthUserTheyCanBeRetrieved() {
        final var retrievedEntity = repository.findById("AUTH_ONLY_USER").orElseThrow();
        assertThat(retrievedEntity.getUsername()).isEqualTo("AUTH_ONLY_USER");
        assertThat(retrievedEntity.getPerson().getFirstName()).isEqualTo("Auth");
        assertThat(retrievedEntity.getAuthorities()).extracting("authority").containsOnly("ROLE_AUTH", "ROLE_AUTH_RO");
        assertThat(retrievedEntity.getEmail()).isEqualTo("auth_only_user@digital.justice.gov.uk");
        assertThat(retrievedEntity.isVerified()).isTrue();
    }

    @Test
    public void testAuthorityMapping() {
        final var entity = repository.findById("AUTH_ONLY_TEST").orElseThrow();
        assertThat(entity.getUsername()).isEqualTo("AUTH_ONLY_TEST");
        assertThat(entity.getName()).isEqualTo("Auth Test");
        assertThat(entity.getAuthorities()).isEmpty();

        entity.getAuthorities().add(new Authority("ROLE_AUTH"));
        entity.getAuthorities().add(new Authority("ROLE_AUTH_RO"));

        repository.save(entity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedEntity = repository.findById("AUTH_ONLY_TEST").orElseThrow();
        final var authorities = retrievedEntity.getAuthorities();
        assertThat(authorities).extracting("authority").containsOnly("ROLE_AUTH", "ROLE_AUTH_RO");
        authorities.removeIf(a -> "ROLE_AUTH".equals(a.getAuthority()));
        assertThat(authorities).extracting("authority").containsOnly("ROLE_AUTH_RO");

        repository.save(retrievedEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedEntity2 = repository.findById("AUTH_ONLY_TEST").orElseThrow();
        assertThat(retrievedEntity2.getAuthorities()).extracting("authority").containsOnly("ROLE_AUTH_RO");
    }

    @Test
    public void findByUsernameAndMasterIsTrue_AuthUser() {
        assertThat(repository.findByUsernameAndMasterIsTrue("AUTH_ONLY_TEST")).isPresent();
    }

    @Test
    public void findByUsernameAndMasterIsTrue_NomisUser() {
        assertThat(repository.findByUsernameAndMasterIsTrue("ITAG_USER")).isEmpty();
    }

    private UserEmail transientEntity() {
        final var email = new UserEmail();
        email.setUsername("user");
        email.setEmail("a@b.com");
        return email;
    }
}
