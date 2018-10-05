package uk.gov.justice.digital.hmpps.oauth2server.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(locations = "classpath:test-application-override.properties")
@Transactional
public class StaffRepositoryTest {

    @Autowired
    private StaffRepository repository;

    @Test
    public void givenATransientEntityItCanBePeristed() {

        var transientEntity = transientEntity();

        var entity = transientEntity.toBuilder().build();

        var persistedEntity = repository.save(entity);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getStaffId()).isNotNull();

        TestTransaction.start();

        var retrievedEntity = repository.findById(persistedEntity.getStaffId()).orElseThrow();

        // equals only compares the business key columns
        assertThat(retrievedEntity).isEqualTo(transientEntity);

        assertThat(retrievedEntity.getStatus()).isEqualTo(transientEntity.getStatus());

    }

    @Test
    public void givenAnExistingStaffMemberTheyCanBeRetrieved() {

        var retrievedEntity = repository.findById(1L).orElseThrow();

        assertThat(retrievedEntity.getUsers()).hasSize(2);
        assertThat(retrievedEntity.getIdentifiers()).hasSize(1);

        StaffUserAccount generalUser = retrievedEntity.getAccountByType("GENERAL");
        assertThat(generalUser.getUsername()).isEqualTo("ITAG_USER");
        assertThat(retrievedEntity.getAccountByType("ADMIN").getUsername()).isEqualTo("ITAG_USER_ADM");
        assertThat(generalUser.getRoles()).hasSize(2);
        assertThat(generalUser.getCaseloads()).hasSize(2);
        assertThat(generalUser.filterRolesByCaseload("NWEB")).hasSize(1);

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
