package uk.gov.justice.digital.hmpps.oauth2server.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
public class ReferenceCodesServiceIntTest {
    @Autowired
    private ReferenceCodesService referenceCodesService;

    @Test
    void isValidEmailDomain_probation() {
        assertThat(referenceCodesService.isValidEmailDomain("hmiprobation.gov.uk")).isTrue();
    }

    @Test
    void isValidEmailDomain_invalid() {
        assertThat(referenceCodesService.isValidEmailDomain("george.gov.uk")).isFalse();
    }

    @Test
    void isValidEmailDomain_wildcard() {
        assertThat(referenceCodesService.isValidEmailDomain("digital.justice.gov.uk")).isTrue();
    }

    @Test
    void isValidEmailDomain_blank() {
        assertThat(referenceCodesService.isValidEmailDomain("  ")).isFalse();
    }
}
