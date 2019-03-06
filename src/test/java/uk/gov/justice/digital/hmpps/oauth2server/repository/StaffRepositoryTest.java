package uk.gov.justice.digital.hmpps.oauth2server.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
public class StaffRepositoryTest {

    @Autowired
    private StaffRepository repository;

    @Test
    public void givenATransientEntityItCanBePeristed() {

        final var transientEntity = transientEntity();

        final var entity = transientEntity.toBuilder().build();

        final var persistedEntity = repository.save(entity);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getStaffId()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findById(persistedEntity.getStaffId()).orElseThrow();

        // equals only compares the business key columns
        assertThat(retrievedEntity).isEqualTo(transientEntity);

        assertThat(retrievedEntity.getStatus()).isEqualTo(transientEntity.getStatus());

    }

    @Test
    public void givenAnExistingStaffMemberTheyCanBeRetrieved() {

        final var retrievedEntity = repository.findById(1L).orElseThrow();

        assertThat(retrievedEntity.getUsers()).hasSize(2);
        assertThat(retrievedEntity.getIdentifiers()).hasSize(1);

        final var generalUser = retrievedEntity.getAccountByType("GENERAL");
        assertThat(generalUser.getUsername()).isEqualTo("ITAG_USER");
        assertThat(retrievedEntity.getAccountByType("ADMIN").getUsername()).isEqualTo("ITAG_USER_ADM");

        assertThat(generalUser.getRoles().stream().map(r -> r.getRole().getName()))
                .containsExactly("Some Old Role", "Omic Administrator", "Maintain Access Roles Admin", "Global Search",
                        "Create Category assessments", "Approve Category assessments");

        assertThat(generalUser.getCaseloads().stream().map(c -> c.getCaseload().getName()))
                .containsExactly("Magic API Caseload", "Moorlands");

        assertThat(generalUser.filterRolesByCaseload("NWEB").stream().map(r -> r.getRole().getName()))
                .containsExactly("Omic Administrator", "Maintain Access Roles Admin", "Global Search",
                        "Create Category assessments", "Approve Category assessments");
    }

    private Staff transientEntity() {
        return Staff
                .builder()
                .firstName("TEST-FIRSTNAME")
                .lastName("TEST-LASTNAME")
                .status("ACTIVE")
                .staffId(-2L)
                .build();
    }
}
