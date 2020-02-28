package uk.gov.justice.digital.hmpps.oauth2server.auth.repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.*;
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig;
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig;
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig;
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("dev")
@Import({AuthDbConfig.class, NomisDbConfig.class, FlywayConfig.class})
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Transactional(transactionManager = "authTransactionManager")
public class UserRepositoryTest {
    @Autowired
    private UserRepository repository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    @Qualifier("authFlyway")
    private Flyway flyway;

    private static boolean initialized;

    @BeforeEach
    void resetFlyway() {
        if (!initialized) {
            flyway.clean();
            flyway.migrate();
            initialized = true;
        }
    }

    @Test
    void givenATransientEntityItCanBePersisted() {
        final var transientEntity = User.builder().username("transiententity").email("transient@b.com").mobile("07987654321").source(AuthSource.delius).build();

        final var persistedEntity = repository.save(transientEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getUsername()).isNotNull();
        assertThat(persistedEntity.getId()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findByUsername(transientEntity.getUsername()).orElseThrow();

        // equals only compares the business key columns
        assertThat(retrievedEntity).isEqualTo(transientEntity);

        assertThat(retrievedEntity.getUsername()).isEqualTo(transientEntity.getUsername());
        assertThat(retrievedEntity.getEmail()).isEqualTo(transientEntity.getEmail());
        assertThat(retrievedEntity.getMobile()).isEqualTo(transientEntity.getMobile());
    }

    @Test
    void givenATransientAuthEntityItCanBePersisted() {

        final var transientEntity = transientEntity();
        transientEntity.setPerson(new Person("first", "last"));

        final var roleLicenceVary = roleRepository.findByRoleCode("LICENCE_VARY").orElseThrow();
        final var roleGlobalSearch = roleRepository.findByRoleCode("GLOBAL_SEARCH").orElseThrow();
        transientEntity.setAuthorities(Set.of(roleLicenceVary, roleGlobalSearch));

        final var persistedEntity = repository.save(transientEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getUsername()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findByUsername(transientEntity.getUsername()).orElseThrow();

        // equals only compares the business key columns
        assertThat(retrievedEntity).isEqualTo(transientEntity);
        assertThat(retrievedEntity.getUsername()).isEqualTo(transientEntity.getUsername());
        assertThat(retrievedEntity.getEmail()).isEqualTo(transientEntity.getEmail());
        assertThat(retrievedEntity.getName()).isEqualTo("first last");
        assertThat(retrievedEntity.getAuthorities()).extracting(Authority::getAuthority).containsOnly("ROLE_LICENCE_VARY", "ROLE_GLOBAL_SEARCH");
    }

    @Test
    void persistUserWithoutEmail() {
        final var transientEntity = User.builder().username("userb").source(AuthSource.nomis).build();
        final var persistedEntity = repository.save(transientEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getUsername()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findByUsername(transientEntity.getUsername()).orElseThrow();

        // equals only compares the business key columns
        assertThat(retrievedEntity).isEqualTo(transientEntity);

        assertThat(retrievedEntity.getUsername()).isEqualTo(transientEntity.getUsername());
        assertThat(retrievedEntity.getEmail()).isNull();
    }

    @Test
    void givenAnExistingUserTheyCanBeRetrieved() {
        final var retrievedEntity = repository.findByUsername("LOCKED_USER").orElseThrow();
        assertThat(retrievedEntity.getUsername()).isEqualTo("LOCKED_USER");
        assertThat(retrievedEntity.getEmail()).isEqualTo("locked@somewhere.com");
        assertThat(retrievedEntity.isVerified()).isTrue();
    }

    @Test
    void givenAnExistingAuthUserTheyCanBeRetrieved() {
        final var retrievedEntity = repository.findByUsername("AUTH_ADM").orElseThrow();
        assertThat(retrievedEntity.getUsername()).isEqualTo("AUTH_ADM");
        assertThat(retrievedEntity.getPerson().getFirstName()).isEqualTo("Auth");
        assertThat(retrievedEntity.getAuthorities()).extracting(Authority::getAuthority).containsOnly("ROLE_OAUTH_ADMIN", "ROLE_MAINTAIN_ACCESS_ROLES", "ROLE_MAINTAIN_OAUTH_USERS");
        assertThat(retrievedEntity.getEmail()).isEqualTo("auth_test2@digital.justice.gov.uk");
        assertThat(retrievedEntity.isVerified()).isTrue();
    }

    @Test
    void testAuthorityMapping() {
        final var entity = repository.findByUsername("AUTH_TEST").orElseThrow();
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

        final var retrievedEntity = repository.findByUsername("AUTH_TEST").orElseThrow();
        final var authorities = retrievedEntity.getAuthorities();
        assertThat(authorities).extracting(Authority::getAuthority).containsOnly("ROLE_LICENCE_VARY", "ROLE_GLOBAL_SEARCH");
        authorities.removeIf(a -> "ROLE_LICENCE_VARY".equals(a.getAuthority()));
        assertThat(authorities).extracting(Authority::getAuthority).containsOnly("ROLE_GLOBAL_SEARCH");

        repository.save(retrievedEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedEntity2 = repository.findByUsername("AUTH_TEST").orElseThrow();
        assertThat(retrievedEntity2.getAuthorities()).extracting(Authority::getAuthority).containsOnly("ROLE_GLOBAL_SEARCH");
    }

    @Test
    void testGroupMapping() {
        final var entity = repository.findByUsername("AUTH_TEST").orElseThrow();
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

        final var retrievedEntity = repository.findByUsername("AUTH_TEST").orElseThrow();
        final var groups = retrievedEntity.getGroups();
        assertThat(groups).extracting(Group::getGroupCode).containsOnly("SITE_1_GROUP_1", "SITE_3_GROUP_1");
        groups.removeIf(a -> "SITE_3_GROUP_1".equals(a.getGroupCode()));
        assertThat(groups).extracting(Group::getGroupCode).containsOnly("SITE_1_GROUP_1");

        repository.save(retrievedEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedEntity2 = repository.findByUsername("AUTH_TEST").orElseThrow();
        assertThat(retrievedEntity2.getGroups()).extracting(Group::getGroupCode).containsOnly("SITE_1_GROUP_1");
    }

    @Test
    void findByUsernameAndMasterIsTrue_AuthUser() {
        assertThat(repository.findByUsernameAndMasterIsTrue("AUTH_TEST")).isPresent();
    }

    @Test
    void findByUsernameAndMasterIsTrue_NomisUser() {
        assertThat(repository.findByUsernameAndMasterIsTrue("ITAG_USER")).isEmpty();
    }

    @Test
    void findByEmail() {
        assertThat(repository.findByEmail("auth_test2@digital.justice.gov.uk")).extracting(User::getUsername).contains("AUTH_ADM", "AUTH_EXPIRED");
    }

    @Test
    void findByEmail_NoRecords() {
        assertThat(repository.findByEmail("noone@digital.justice.gov.uk")).isEmpty();
    }

    @Test
    void findByEmailAndMasterIsTrue() {
        assertThat(repository.findByEmailAndMasterIsTrueOrderByUsername("auth_test2@digital.justice.gov.uk"))
                .extracting(User::getUsername)
                .contains("AUTH_ADM", "AUTH_EXPIRED");
    }

    @Test
    void findByEmailAndMasterIsTrue_NomisUser() {
        assertThat(repository.findByEmailAndMasterIsTrueOrderByUsername("ca_user@digital.justice.gov.uk")).isEmpty();
    }

    @Test
    void findAll_UserFilter_ByRole() {
        assertThat(repository.findAll(UserFilter.builder().roleCode("LICENCE_VARY").build()))
                .extracting(User::getUsername)
                .containsExactly("AUTH_RO_VARY_USER");
    }

    @Test
    void findAll_UserFilter_ByGroup() {
        assertThat(repository.findAll(UserFilter.builder().groupCode("SITE_1_GROUP_2").build()))
                .extracting(User::getUsername)
                .containsExactly("AUTH_RO_VARY_USER", "AUTH_GROUP_MANAGER");
    }

    @Test
    void findAll_UserFilter_ByUsername() {
        assertThat(repository.findAll(UserFilter.builder().name("_expired").build()))
                .extracting(User::getUsername)
                .contains("AUTH_EXPIRED", "AUTH_MFA_EXPIRED_USER");
    }

    @Test
    void findAll_UserFilter_ByEmail() {
        assertThat(repository.findAll(UserFilter.builder().name("test@digital").build()))
                .extracting(User::getUsername)
                .containsOnly("AUTH_TEST", "AUTH_RO_USER_TEST", "AUTH_CHANGE_TEST", "AUTH_CHANGE2_TEST", "AUTH_CHANGE_EMAIL", "AUTH_CHANGE_MOBILE",
                        "AUTH_CHANGE_MOBILE_VERIFIED", "AUTH_CHANGE_MOBILE2");
    }

    @Test
    void findAll_UserFilter_ByFirstNameLastName() {
        assertThat(repository.findAll(UserFilter.builder().name("a no").build()))
                .extracting(User::getUsername)
                .containsExactly("AUTH_NO_EMAIL", "AUTH_MFA_NOEMAIL_USER");
    }

    @Test
    void findAll_UserFilter_ByLastNameFirstName() {
        assertThat(repository.findAll(UserFilter.builder().name("orton, r").build()))
                .extracting(User::getUsername)
                .containsOnly("AUTH_RO_USER", "AUTH_RO_VARY_USER", "AUTH_RO_USER_TEST");
    }

    @Test
    void findAll_UserFilter_all() {
        assertThat(repository.findAll(UserFilter.builder().roleCode("LICENCE_VARY").groupCode("SITE_1_GROUP_2").name("vary").build()))
                .extracting(User::getUsername)
                .containsExactly("AUTH_RO_VARY_USER");
    }

    @Test
    void findInactiveUsers_First10() {
        final var inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(LocalDateTime.now().plusMinutes(1));
        assertThat(inactive).extracting(User::getUsername)
                .contains("AUTH_INACTIVE")
                .doesNotContain("AUTH_DISABLED", "ITAG_USER");
        assertThat(inactive).hasSize(10);
    }

    @Test
    void findInactiveUsers_OrderByLastLoggedInOldestFirst() {
        final var inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(LocalDateTime.now().plusMinutes(1));
        assertThat(inactive).extracting(User::getUsername).first().isEqualTo("AUTH_USER_LAST_LOGIN");
    }

    @Test
    void findInactiveUsers_NoRows() {
        final var inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(LocalDateTime.parse("2019-01-01T12:00:00").minusSeconds(1));
        assertThat(inactive).isEmpty();
    }

    @Test
    void findInactiveUsers_SingleRow() {
        final var inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(LocalDateTime.parse("2019-02-03T13:23:19").plusSeconds(1));
        assertThat(inactive).extracting(User::getUsername).containsExactly("AUTH_USER_LAST_LOGIN", "AUTH_INACTIVE");
    }

    @Test
    void findDisabledUsers_First10() {
        final var inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(LocalDateTime.now().plusMinutes(1));
        assertThat(inactive).extracting(User::getUsername)
                .contains("AUTH_DELETE", "AUTH_DELETEALL", "NOMIS_DELETE")
                .doesNotContain("AUTH_DISABLED", "AUTH_USER");
        assertThat(inactive).hasSize(10);
    }

    @Test
    void findDisabledUsers_OrderByLastLoggedInOldestFirst() {
        final var inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(LocalDateTime.now().plusMinutes(1));
        assertThat(inactive).extracting(User::getUsername).first().isEqualTo("AUTH_DELETE");
    }

    @Test
    void findDisabledUsers_NoRows() {
        final var inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(LocalDateTime.parse("2018-01-02T13:23:19").minusSeconds(1));
        assertThat(inactive).isEmpty();
    }

    @Test
    void findDisabledUsers_SingleRow() {
        final var inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(LocalDateTime.parse("2018-01-02T13:23:19").plusSeconds(1));
        assertThat(inactive).extracting(User::getUsername).containsExactly("AUTH_DELETE");
    }

    private User transientEntity() {
        return User.builder().username("user").source(AuthSource.nomis).email("a@b.com").build();
    }
}
