package uk.gov.justice.digital.hmpps.oauth2server.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev-seed")
@Transactional
public class VerifyEmailServiceIntTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private VerifyEmailService verifyEmailService;

    @Before
    public void setUp() {
        verifyEmailService = new VerifyEmailService(null, null, null, jdbcTemplate, null, null, "templateId");
    }

    @Test
    public void getExistingEmailAddresses() {
        final var emails = verifyEmailService.getExistingEmailAddresses("RO_USER");
        assertThat(emails).containsExactlyInAnyOrder("phillips@bobjustice.gov.uk", "phillips@fredjustice.gov.uk");
    }

    @Test
    public void getExistingEmailAddresses_NotFound() {
        final var emails = verifyEmailService.getExistingEmailAddresses("CA_USER");
        assertThat(emails).isEmpty();
    }
}
