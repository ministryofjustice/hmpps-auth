package uk.gov.justice.digital.hmpps.oauth2server.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.DomainCodeIdentifier;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.ReferenceCode;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.ReferenceDomain;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.ReferenceCodeRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev-seed")
@Transactional
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
