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
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffIdentifierRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("dev")
@Import({AuthDbConfig.class, NomisDbConfig.class, FlywayConfig.class})
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class StaffIdentifierRepositoryTest {

    public static final long STAFF_ID = 3L;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private StaffIdentifierRepository repository;

    @Test
    public void givenATransientEntityItCanBePeristed() {

        final var staff = staffRepository.findById(STAFF_ID).orElseThrow();
        assertThat(staff.getIdentifiers()).hasSize(0);

        final var newStaffId = staff.addIdentifier("WIBBLE", "WOBBLE");

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(staff.getIdentifiers()).hasSize(1);

        TestTransaction.start();

        final var retrievedEntity = staffRepository.findById(STAFF_ID).orElseThrow();

        assertThat(retrievedEntity.findIdentifier("WIBBLE")).isEqualTo(newStaffId);

    }

    @Test
    public void givenAnExistingStaffMemberTheyCanBeRetrievedByIdentification() {

        final var retrievedEntity = repository.findById_TypeAndId_IdentificationNumber("YJAF", "olduser@yjaf.gov.uk");

        final var staff = retrievedEntity.getStaff();
        assertThat(staff).isNotNull();

        assertThat(staff.getFirstName()).isEqualTo("Old");
        assertThat(staff.getIdentifiers()).hasSize(1);
        assertThat(staff.findIdentifier("YJAF").getId().getIdentificationNumber()).isEqualTo("olduser@yjaf.gov.uk");
    }

}
