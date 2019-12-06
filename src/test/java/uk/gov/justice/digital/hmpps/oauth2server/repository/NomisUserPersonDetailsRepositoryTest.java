package uk.gov.justice.digital.hmpps.oauth2server.repository;

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
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig;
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig;
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffUserAccountRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("dev")
@Import({AuthDbConfig.class, NomisDbConfig.class, FlywayConfig.class})
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class NomisUserPersonDetailsRepositoryTest {

    private static final Staff DEFAULT_STAFF = Staff.builder().staffId(5L).build();

    @Autowired
    private StaffUserAccountRepository repository;

    @Test
    public void givenATransientEntityItCanBePeristed() {

        final var transientEntity = transientEntity();

        final var entity = transientEntity.toBuilder().build();

        final var persistedEntity = repository.save(entity);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getUsername()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findById(entity.getUsername()).orElseThrow();

        // equals only compares the business key columns
        assertThat(retrievedEntity).isEqualTo(transientEntity);

        assertThat(retrievedEntity.getUsername()).isEqualTo(transientEntity.getUsername());
        assertThat(retrievedEntity.getType()).isEqualTo(transientEntity.getType());
    }

    @Test
    public void givenAnExistingUserTheyCanBeRetrieved() {
        final var retrievedEntity = repository.findById("ITAG_USER").orElseThrow();
        assertThat(retrievedEntity.getUsername()).isEqualTo("ITAG_USER");
        assertThat(retrievedEntity.getRoles().stream().map(r -> r.getRole().getName()))
                .containsExactly("Some Old Role", "Omic Administrator", "KW Migration", "Maintain Access Roles Admin", "Global Search",
                        "Create Category assessments", "Approve Category assessments", "Security Cat tool role");

        assertThat(retrievedEntity.getCaseloads().stream().map(c -> c.getCaseload().getName()))
                .containsExactly("Magic API Caseload", "Moorlands");

        assertThat(retrievedEntity.filterRolesByCaseload("NWEB").stream().map(r -> r.getRole().getName()))
                .containsExactly("Omic Administrator", "KW Migration", "Maintain Access Roles Admin", "Global Search",
                        "Create Category assessments", "Approve Category assessments", "Security Cat tool role");

        assertThat(retrievedEntity.getStaff().getIdentifiers().stream().map(i -> i.getStaff().getFirstName()))
                .containsExactly("Itag");
    }

    @Test
    public void testSpare4MappedAsPasswordFromSysUserTable() {
        final var entity = repository.findById("CA_USER").orElseThrow();
        assertThat(entity.getPassword()).startsWith("{bcrypt}");
    }

    private NomisUserPersonDetails transientEntity() {
        return NomisUserPersonDetails
                .builder()
                .username("TEST_USER")
                .type("ADMIN")
                .staff(DEFAULT_STAFF)
                .build();
    }
}
