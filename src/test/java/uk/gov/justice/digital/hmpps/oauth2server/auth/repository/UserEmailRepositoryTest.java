package uk.gov.justice.digital.hmpps.oauth2server.auth.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.*;
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig;
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig;
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("dev")
@Import({AuthDbConfig.class, NomisDbConfig.class, FlywayConfig.class})
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Transactional(transactionManager = "authTransactionManager")
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
public class UserEmailRepositoryTest {
    @Autowired
    private UserEmailRepository repository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private RoleRepository roleRepository;

    @Test
    public void givenATransientEntityItCanBePersisted() {

        final var transientEntity = transientEntity();

        final var entity = UserEmail.builder().username(transientEntity.getUsername()).email(transientEntity.getEmail()).build();

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

        final var roleLicenceVary = roleRepository.findByRoleCode("LICENCE_VARY").orElseThrow();
        final var roleGlobalSearch = roleRepository.findByRoleCode("GLOBAL_SEARCH").orElseThrow();
        transientEntity.setAuthorities(Set.of(roleLicenceVary, roleGlobalSearch));

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
        assertThat(retrievedEntity.getAuthorities()).extracting(Authority::getAuthority).containsOnly("ROLE_LICENCE_VARY", "ROLE_GLOBAL_SEARCH");
    }

    @Test
    public void persistUserWithoutEmail() {
        final var transientEntity = UserEmail.of("userb");
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
        final var retrievedEntity = repository.findById("AUTH_ADM").orElseThrow();
        assertThat(retrievedEntity.getUsername()).isEqualTo("AUTH_ADM");
        assertThat(retrievedEntity.getPerson().getFirstName()).isEqualTo("Auth");
        assertThat(retrievedEntity.getAuthorities()).extracting(Authority::getAuthority).containsOnly("ROLE_OAUTH_ADMIN", "ROLE_MAINTAIN_ACCESS_ROLES", "ROLE_MAINTAIN_OAUTH_USERS");
        assertThat(retrievedEntity.getEmail()).isEqualTo("auth_test2@digital.justice.gov.uk");
        assertThat(retrievedEntity.isVerified()).isTrue();
    }

    @Test
    public void testAuthorityMapping() {
        final var entity = repository.findById("AUTH_TEST").orElseThrow();
        assertThat(entity.getUsername()).isEqualTo("AUTH_TEST");
        assertThat(entity.getName()).isEqualTo("Auth Test");
        assertThat(entity.getAuthorities()).isEmpty();

        final var roleLicenceVary = roleRepository.findByRoleCode("LICENCE_VARY").orElseThrow();
        final var roleGlobalSearch = roleRepository.findByRoleCode("GLOBAL_SEARCH").orElseThrow();
        entity.getAuthorities().add(roleLicenceVary);
        entity.getAuthorities().add(roleGlobalSearch);

        repository.save(entity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedEntity = repository.findById("AUTH_TEST").orElseThrow();
        final var authorities = retrievedEntity.getAuthorities();
        assertThat(authorities).extracting(Authority::getAuthority).containsOnly("ROLE_LICENCE_VARY", "ROLE_GLOBAL_SEARCH");
        authorities.removeIf(a -> "ROLE_LICENCE_VARY".equals(a.getAuthority()));
        assertThat(authorities).extracting(Authority::getAuthority).containsOnly("ROLE_GLOBAL_SEARCH");

        repository.save(retrievedEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedEntity2 = repository.findById("AUTH_TEST").orElseThrow();
        assertThat(retrievedEntity2.getAuthorities()).extracting(Authority::getAuthority).containsOnly("ROLE_GLOBAL_SEARCH");
    }

    @Test
    public void testGroupMapping() {
        final var entity = repository.findById("AUTH_TEST").orElseThrow();
        assertThat(entity.getUsername()).isEqualTo("AUTH_TEST");
        assertThat(entity.getName()).isEqualTo("Auth Test");
        assertThat(TestTransaction.isActive()).isTrue();
        assertThat(entity.getGroups()).isEmpty();

        final var group1 = groupRepository.findByGroupCode("SITE_1_GROUP_1").orElseThrow();
        final var group3 = groupRepository.findByGroupCode("SITE_3_GROUP_1").orElseThrow();
        entity.getGroups().add(group1);
        entity.getGroups().add(group3);

        repository.save(entity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedEntity = repository.findById("AUTH_TEST").orElseThrow();
        final var groups = retrievedEntity.getGroups();
        assertThat(groups).extracting(Group::getGroupCode).containsOnly("SITE_1_GROUP_1", "SITE_3_GROUP_1");
        groups.removeIf(a -> "SITE_3_GROUP_1".equals(a.getGroupCode()));
        assertThat(groups).extracting(Group::getGroupCode).containsOnly("SITE_1_GROUP_1");

        repository.save(retrievedEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedEntity2 = repository.findById("AUTH_TEST").orElseThrow();
        assertThat(retrievedEntity2.getGroups()).extracting(Group::getGroupCode).containsOnly("SITE_1_GROUP_1");
    }

    @Test
    public void findByUsernameAndMasterIsTrue_AuthUser() {
        assertThat(repository.findByUsernameAndMasterIsTrue("AUTH_TEST")).isPresent();
    }

    @Test
    public void findByUsernameAndMasterIsTrue_NomisUser() {
        assertThat(repository.findByUsernameAndMasterIsTrue("ITAG_USER")).isEmpty();
    }

    @Test
    public void findByEmail() {
        assertThat(repository.findByEmail("auth_test2@digital.justice.gov.uk")).extracting(UserEmail::getUsername).containsOnly("AUTH_ADM", "AUTH_EXPIRED");
    }

    @Test
    public void findByEmail_NoRecords() {
        assertThat(repository.findByEmail("noone@digital.justice.gov.uk")).isEmpty();
    }

    @Test
    public void findByEmailAndMasterIsTrue() {
        assertThat(repository.findByEmailAndMasterIsTrueOrderByUsername("auth_test2@digital.justice.gov.uk"))
                .extracting(UserEmail::getUsername)
                .containsExactly("AUTH_ADM", "AUTH_EXPIRED");
    }

    @Test
    public void findByEmailAndMasterIsTrue_NomisUser() {
        assertThat(repository.findByEmailAndMasterIsTrueOrderByUsername("ca_user@digital.justice.gov.uk")).isEmpty();
    }

    @Test
    public void findAll_UserEmailFilter_ByRole() {
        assertThat(repository.findAll(UserEmailFilter.builder().roleCode("LICENCE_VARY").build()))
                .extracting(UserEmail::getUsername)
                .containsExactly("AUTH_RO_VARY_USER");
    }

    @Test
    public void findAll_UserEmailFilter_ByGroup() {
        assertThat(repository.findAll(UserEmailFilter.builder().groupCode("SITE_1_GROUP_2").build()))
                .extracting(UserEmail::getUsername)
                .containsExactly("AUTH_RO_VARY_USER", "AUTH_GROUP_MANAGER");
    }

    @Test
    public void findAll_UserEmailFilter_ByUsername() {
        assertThat(repository.findAll(UserEmailFilter.builder().name("_expired").build()))
                .extracting(UserEmail::getUsername)
                .containsExactly("AUTH_EXPIRED");
    }

    @Test
    public void findAll_UserEmailFilter_ByEmail() {
        assertThat(repository.findAll(UserEmailFilter.builder().name("test@digital").build()))
                .extracting(UserEmail::getUsername)
                .containsExactly("AUTH_TEST", "AUTH_RO_USER_TEST");
    }

    @Test
    public void findAll_UserEmailFilter_ByFirstNameLastName() {
        assertThat(repository.findAll(UserEmailFilter.builder().name("a no").build()))
                .extracting(UserEmail::getUsername)
                .containsExactly("AUTH_NO_EMAIL");
    }

    @Test
    public void findAll_UserEmailFilter_ByLastNameFirstName() {
        assertThat(repository.findAll(UserEmailFilter.builder().name("orton, r").build()))
                .extracting(UserEmail::getUsername)
                .containsExactly("AUTH_RO_USER", "AUTH_RO_VARY_USER", "AUTH_RO_USER_TEST");
    }

    @Test
    public void findAll_UserEmailFilter_all() {
        assertThat(repository.findAll(UserEmailFilter.builder().roleCode("LICENCE_VARY").groupCode("SITE_1_GROUP_2").name("vary").build()))
                .extracting(UserEmail::getUsername)
                .containsExactly("AUTH_RO_VARY_USER");
    }

    @Test
    public void findInactiveUsers_First10() {
        final var inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(LocalDateTime.now().plusMinutes(1));
        assertThat(inactive).extracting(UserEmail::getUsername)
                .contains("AUTH_INACTIVE")
                .doesNotContain("AUTH_DISABLED", "ITAG_USER");
        assertThat(inactive).hasSize(10);
    }

    @Test
    public void findInactiveUsers_OrderByLastLoggedInOldestFirst() {
        final var inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(LocalDateTime.now().plusMinutes(1));
        assertThat(inactive).extracting(UserEmail::getUsername).first().isEqualTo("AUTH_INACTIVE");
    }

    @Test
    public void findInactiveUsers_NoRows() {
        final var inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(LocalDateTime.parse("2019-02-03T13:23:19").minusSeconds(1));
        assertThat(inactive).isEmpty();
    }

    @Test
    public void findInactiveUsers_SingleRow() {
        final var inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(LocalDateTime.parse("2019-02-03T13:23:19").plusSeconds(1));
        assertThat(inactive).extracting(UserEmail::getUsername).containsExactly("AUTH_INACTIVE");
    }

    @Test
    public void findDisabledUsers_First10() {
        final var inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(LocalDateTime.now().plusMinutes(1));
        assertThat(inactive).extracting(UserEmail::getUsername)
                .contains("AUTH_DELETE", "AUTH_DELETEALL", "NOMIS_DELETE")
                .doesNotContain("AUTH_DISABLED", "AUTH_USER");
        assertThat(inactive).hasSize(10);
    }

    @Test
    public void findDisabledUsers_OrderByLastLoggedInOldestFirst() {
        final var inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(LocalDateTime.now().plusMinutes(1));
        assertThat(inactive).extracting(UserEmail::getUsername).first().isEqualTo("AUTH_DELETE");
    }

    @Test
    public void findDisabledUsers_NoRows() {
        final var inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(LocalDateTime.parse("2018-01-02T13:23:19").minusSeconds(1));
        assertThat(inactive).isEmpty();
    }

    @Test
    public void findDisabledUsers_SingleRow() {
        final var inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(LocalDateTime.parse("2018-01-02T13:23:19").plusSeconds(1));
        assertThat(inactive).extracting(UserEmail::getUsername).containsExactly("AUTH_DELETE");
    }

    private UserEmail transientEntity() {
        return UserEmail.builder().username("user").email("a@b.com").build();
    }
}
