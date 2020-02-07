package uk.gov.justice.digital.hmpps.oauth2server.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailHelperTest {

    @Test
    void formatToLowercase() {
        assertThat(EmailHelper.format(" JOHN brian")).isEqualTo("john brian");
    }

    @Test
    void formatTrim() {
        assertThat(EmailHelper.format(" john obrian  ")).isEqualTo("john obrian");
    }

    @Test
    void formatReplaceMicrosoftQuote() {
        assertThat(EmailHelper.format(" JOHN O’brian")).isEqualTo("john o'brian");
    }
}
