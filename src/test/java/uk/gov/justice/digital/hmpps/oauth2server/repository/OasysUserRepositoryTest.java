package uk.gov.justice.digital.hmpps.oauth2server.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.oasys.repository.OasysUserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
public class OasysUserRepositoryTest {

    @Autowired
    private OasysUserRepository oasysUserRepository;

    @Test
    public void givenAnExistingOasysUserTheyCanBeRetrievedByIdentification() {

        final var retrievedEntity = oasysUserRepository.findById("BOB");

        assertThat(retrievedEntity.isPresent()).isTrue();

        assertThat(retrievedEntity.get().getUserForename1()).isEqualTo("Bob");
        assertThat(retrievedEntity.get().getEmailAddress()).isEqualTo("bob.bobson@bb.com");
    }

}
