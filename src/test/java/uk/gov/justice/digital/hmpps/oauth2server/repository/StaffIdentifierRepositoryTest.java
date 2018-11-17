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
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffIdentifier;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffIdentifierRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
public class StaffIdentifierRepositoryTest {

    public static final long STAFF_ID = 3L;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private StaffIdentifierRepository repository;

    @Test
    public void givenATransientEntityItCanBePeristed() {

        Staff staff = staffRepository.findById(STAFF_ID).orElseThrow();
        assertThat(staff.getIdentifiers()).hasSize(0);

        StaffIdentifier newStaffId = staff.addIdentifier("WIBBLE", "WOBBLE");

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(staff.getIdentifiers()).hasSize(1);

        TestTransaction.start();

        var retrievedEntity = staffRepository.findById(STAFF_ID).orElseThrow();

        assertThat(retrievedEntity.findIdentifier("WIBBLE")).isEqualTo(newStaffId);

    }

    @Test
    public void givenAnExistingStaffMemberTheyCanBeRetrievedByIdentification() {

        var retrievedEntity = repository.findById_TypeAndId_IdentificationNumber("YJAF", "olduser@yjaf.gov.uk");

        var staff = retrievedEntity.getStaff();
        assertThat(staff).isNotNull();

        assertThat(staff.getFirstName()).isEqualTo("OLD");
        assertThat(staff.getIdentifiers()).hasSize(1);
        assertThat(staff.findIdentifier("YJAF").getId().getIdentificationNumber()).isEqualTo("olduser@yjaf.gov.uk");
    }

}
