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
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group;
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
public class GroupRepositoryTest {
    @Autowired
    private GroupRepository repository;

    @Test
    public void givenATransientEntityItCanBePersisted() {

        final var transientEntity = transientEntity();

        final var entity = new Group(transientEntity.getGroupCode(), transientEntity.getGroupName());

        final var persistedEntity = repository.save(entity);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getGroupCode()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findByGroupCode(entity.getGroupCode()).orElseThrow();

        // equals only compares the business key columns
        assertThat(retrievedEntity).isEqualTo(transientEntity);

        assertThat(retrievedEntity.getGroupCode()).isEqualTo(transientEntity.getGroupCode());
        assertThat(retrievedEntity.getGroupName()).isEqualTo(transientEntity.getGroupName());
    }

    @Test
    public void givenAnExistingUserTheyCanBeRetrieved() {
        final var retrievedEntity = repository.findByGroupCode("SITE_1_GROUP_1").orElseThrow();
        assertThat(retrievedEntity.getGroupCode()).isEqualTo("SITE_1_GROUP_1");
        assertThat(retrievedEntity.getGroupName()).isEqualTo("Site 1 - Group 1");
    }

    @Test
    public void findByEmail_NoRecords() {
        assertThat(repository.findAllByOrderByGroupName()).extracting(Group::getGroupCode).containsExactly("SITE_1_GROUP_1", "SITE_1_GROUP_2", "SITE_2_GROUP_1", "SITE_3_GROUP_1");
    }

    private Group transientEntity() {
        return new Group("hdc", "Licences");
    }
}
