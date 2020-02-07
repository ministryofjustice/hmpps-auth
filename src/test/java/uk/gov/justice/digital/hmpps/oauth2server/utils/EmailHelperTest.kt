package uk.gov.justice.digital.hmpps.oauth2server.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class EmailHelperTest {
  @Test
  fun formatToLowercase() {
    assertThat(EmailHelper.format(" JOHN brian")).isEqualTo("john brian")
  }

  @Test
  fun formatTrim() {
    assertThat(EmailHelper.format(" john obrian  ")).isEqualTo("john obrian")
  }

  @Test
  fun formatReplaceMicrosoftQuote() {
    assertThat(EmailHelper.format(" JOHN O’brian")).isEqualTo("john o'brian")
  }
}
