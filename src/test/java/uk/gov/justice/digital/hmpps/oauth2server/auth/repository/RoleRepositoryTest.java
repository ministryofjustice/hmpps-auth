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
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority;
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig;
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig;
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("dev")
@Import({AuthDbConfig.class, NomisDbConfig.class, FlywayConfig.class})
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Transactional(transactionManager = "authTransactionManager")
public class RoleRepositoryTest {
    @Autowired
    private RoleRepository repository;

    @Test
    public void givenATransientEntityItCanBePersisted() {

        final var transientEntity = transientEntity();

        final var entity = new Authority(transientEntity.getAuthority(), transientEntity.getRoleName());

        final var persistedEntity = repository.save(entity);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getAuthority()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findByRoleCode(entity.getRoleCode()).orElseThrow();

        // equals only compares the business key columns
        assertThat(retrievedEntity).isEqualTo(transientEntity);

        assertThat(retrievedEntity.getAuthority()).isEqualTo(transientEntity.getAuthority());
        assertThat(retrievedEntity.getRoleName()).isEqualTo(transientEntity.getRoleName());
    }

    @Test
    public void givenAnExistingRoleTheyCanBeRetrieved() {
        final var retrievedEntity = repository.findByRoleCode("PECS_POLICE").orElseThrow();
        assertThat(retrievedEntity.getAuthority()).isEqualTo("ROLE_PECS_POLICE");
        assertThat(retrievedEntity.getRoleName()).isEqualTo("PECS Police");
    }

    @Test
    public void findAllByOrderByRoleName() {
        assertThat(repository.findAllByOrderByRoleName()).extracting(Authority::getAuthority).contains("ROLE_GLOBAL_SEARCH", "ROLE_PECS_POLICE");
    }

    @Test
    public void findByGroupAssignableRolesForUsername() {
        assertThat(repository.findByGroupAssignableRolesForUsername("AUTH_RO_VARY_USER")).extracting(Authority::getRoleCode).containsExactly("GLOBAL_SEARCH", "LICENCE_RO", "LICENCE_VARY");
    }

    private Authority transientEntity() {
        return new Authority("hdc", "Licences");
    }
}
