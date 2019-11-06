package uk.gov.justice.digital.hmpps.oauth2server.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EmailHelperTest {

    @Test
    public void formatToLowercase() {
        assertThat(EmailHelper.format(" JOHN brian")).isEqualTo("john brian");
    }

    @Test
    public void formatTrim() {
        assertThat(EmailHelper.format(" john obrian  ")).isEqualTo("john obrian");
    }

    @Test
    public void formatReplaceMicrosoftQuote() {
        assertThat(EmailHelper.format(" JOHN O’brian")).isEqualTo("john o'brian");
    }
}
