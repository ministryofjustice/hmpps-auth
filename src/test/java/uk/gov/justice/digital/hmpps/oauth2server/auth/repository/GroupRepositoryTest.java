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
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.GroupAssignableRole;
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig;
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig;
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("dev")
@Import({AuthDbConfig.class, NomisDbConfig.class, FlywayConfig.class})
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Transactional("authTransactionManager")
public class GroupRepositoryTest {
    @Autowired
    private GroupRepository repository;
    @Autowired
    private RoleRepository roleRepository;

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
    public void testRoleMapping() {
        final var entity = repository.findByGroupCode("SITE_3_GROUP_1").orElseThrow();
        assertThat(entity.getGroupCode()).isEqualTo("SITE_3_GROUP_1");
        assertThat(entity.getAssignableRoles()).isEmpty();

        final var role1 = roleRepository.findByRoleCode("GLOBAL_SEARCH").orElseThrow();
        final var role2 = roleRepository.findByRoleCode("LICENCE_RO").orElseThrow();
        final var gar1 = new GroupAssignableRole(role1, entity, false);
        entity.getAssignableRoles().add(gar1);
        final var gar2 = new GroupAssignableRole(role2, entity, true);
        entity.getAssignableRoles().add(gar2);

        repository.save(entity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedEntity = repository.findByGroupCode("SITE_3_GROUP_1").orElseThrow();
        final var assignableRoles = retrievedEntity.getAssignableRoles();
        assertThat(assignableRoles).extracting(GroupAssignableRole::getRole).extracting(Authority::getRoleCode).containsOnly("GLOBAL_SEARCH", "LICENCE_RO");
        assignableRoles.remove(gar1);
        assertThat(assignableRoles).containsExactly(gar2);

        repository.save(retrievedEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedEntity2 = repository.findByGroupCode("SITE_3_GROUP_1").orElseThrow();
        assertThat(retrievedEntity2.getAssignableRoles()).containsOnly(gar2);
    }

    @Test
    public void givenAnExistingGroupTheyCanBeRetrieved() {
        final var group = repository.findByGroupCode("SITE_1_GROUP_2").orElseThrow();
        assertThat(group.getGroupCode()).isEqualTo("SITE_1_GROUP_2");
        assertThat(group.getGroupName()).isEqualTo("Site 1 - Group 2");
        assertThat(group.getAssignableRoles()).extracting(GroupAssignableRole::getRole).extracting(Authority::getRoleCode).containsOnly("GLOBAL_SEARCH", "LICENCE_RO");
    }

    @Test
    public void findAllByOrderByGroupName() {
        assertThat(repository.findAllByOrderByGroupName()).extracting(Group::getGroupCode).containsSequence("SITE_1_GROUP_1", "SITE_1_GROUP_2", "SITE_2_GROUP_1", "SITE_3_GROUP_1");
    }

    private Group transientEntity() {
        return new Group("hdc", "Licences");
    }
}
