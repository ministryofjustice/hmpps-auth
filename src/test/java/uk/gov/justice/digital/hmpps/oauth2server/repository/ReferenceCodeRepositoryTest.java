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
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.DomainCodeIdentifier;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.ReferenceCode;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.ReferenceDomain;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.ReferenceCodeRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("dev")
@Import({AuthDbConfig.class, NomisDbConfig.class, FlywayConfig.class})
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class ReferenceCodeRepositoryTest {

    @Autowired
    private ReferenceCodeRepository repository;

    @Test
    public void givenATransientEntityItCanBePeristed() {

        final var transientEntity = transientEntity();

        final var entity = transientEntity.toBuilder().build();

        final var persistedEntity = repository.save(entity);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getDomainCodeIdentifier()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findById(entity.getDomainCodeIdentifier()).orElseThrow();

        // equals only compares the business key columns
        assertThat(retrievedEntity).isEqualTo(transientEntity);

        assertThat(retrievedEntity.getDescription()).isEqualTo(transientEntity.getDescription());
        assertThat(retrievedEntity.isActive()).isEqualTo(transientEntity.isActive());

    }

    @Test
    public void givenAnExistingUserTheyCanBeRetrieved() {
        final var retrievedEntity = repository.findById(new DomainCodeIdentifier(ReferenceDomain.EMAIL_DOMAIN, "PROBATION")).orElseThrow();
        assertThat(retrievedEntity.getDescription()).isEqualTo("HMIProbation.gov.uk");
        assertThat(retrievedEntity.isActive()).isTrue();
    }

    public void testFind() {
        final var codes = repository.findByDomainCodeIdentifierDomainAndActiveIsTrueAndExpiredDateIsNull(ReferenceDomain.EMAIL_DOMAIN);
        assertThat(codes).extracting("description").contains("%justice.gov.uk", "HMIProbation.gov.uk").hasSize(9);
    }

    private ReferenceCode transientEntity() {
        return ReferenceCode
                .builder()
                .domainCodeIdentifier(new DomainCodeIdentifier(ReferenceDomain.EMAIL_DOMAIN, "JOE"))
                .active(true)
                .description("some description")
                .build();
    }
}
