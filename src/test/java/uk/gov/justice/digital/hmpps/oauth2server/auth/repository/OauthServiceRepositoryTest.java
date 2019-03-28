package uk.gov.justice.digital.hmpps.oauth2server.auth.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
public class OauthServiceRepositoryTest {
    @Autowired
    private OauthServiceRepository repository;

    @Test
    public void givenATransientEntityItCanBePersisted() {

        final var transientEntity = transientEntity();

        final var entity = transientEntity();

        final var persistedEntity = repository.save(entity);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getCode()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findById(entity.getCode()).orElseThrow();

        // equals only compares the business key columns
        assertThat(retrievedEntity).isEqualTo(transientEntity);

        assertThat(retrievedEntity.getCode()).isEqualTo(transientEntity.getCode());
        assertThat(retrievedEntity.getName()).isEqualTo(transientEntity.getName());
    }

    @Test
    public void findByUsernameAndMasterIsTrue_AuthUser() {
        assertThat(repository.findAllByEnabledTrueOrderByName()).extracting(Service::getName).containsOnly("New NOMIS", "Categorisation Tool", "Home Detention Curfew", "Allocate a POM");
    }

    private Service transientEntity() {
        return new Service("CODE", "NAME", "Description", "SOME_ROLE", "http://some.url", true);
    }
}
