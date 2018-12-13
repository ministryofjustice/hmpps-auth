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
@ActiveProfiles("dev")
@Transactional
public class ReferenceCodesServiceIntTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ReferenceCodesService referenceCodesService;

    @Before
    public void setUp() {
        referenceCodesService = new ReferenceCodesService(jdbcTemplate);
    }

    @Test
    public void getValidEmailDomains() {
        final var domains = referenceCodesService.getValidEmailDomains();
        assertThat(domains).contains("%justice.gov.uk", "HMIProbation.gov.uk").hasSize(9);
    }
}
