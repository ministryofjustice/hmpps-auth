package uk.gov.justice.digital.hmpps.oauth2server.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.security.StaffUserAccountRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(locations = "classpath:test-application-override.properties")
@Transactional
public class StaffUserAccountRepositoryTest {

    private static final Staff DEFAULT_STAFF = Staff.builder().staffId(1L).build();

    @Autowired
    private StaffUserAccountRepository repository;

    @Test
    public void givenATransientEntityItCanBePeristed() {

        var transientEntity = transientEntity();

        var entity = transientEntity.toBuilder().build();

        var persistedEntity = repository.save(entity);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getUsername()).isNotNull();

        TestTransaction.start();

        var retrievedEntity = repository.findById(entity.getUsername()).orElseThrow();

        // equals only compares the business key columns
        assertThat(retrievedEntity).isEqualTo(transientEntity);

        assertThat(retrievedEntity.getUsername()).isEqualTo(transientEntity.getUsername());
        assertThat(retrievedEntity.getType()).isEqualTo(transientEntity.getType());

    }

    @Test
    public void givenAnExistingUserTheyCanBeRetrieved() {
        var retrievedEntity = repository.findById("ITAG_USER").orElseThrow();
        assertThat(retrievedEntity.getUsername()).isEqualTo("ITAG_USER");
        assertThat(retrievedEntity.getRoles()).hasSize(2);
        assertThat(retrievedEntity.getCaseloads()).hasSize(2);
        assertThat(retrievedEntity.filterRolesByCaseload("NWEB")).hasSize(1);
        assertThat(retrievedEntity.getStaff().getIdentifiers()).hasSize(1);
    }


    private StaffUserAccount transientEntity() {
        return StaffUserAccount
                .builder()
                .username("TEST_USER")
                .type("GENERAL")
                .staff(DEFAULT_STAFF)
                .build();
    }
}
