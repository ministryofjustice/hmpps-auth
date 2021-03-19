package uk.gov.justice.digital.hmpps.oauth2server.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class StringSanitiserTest {
  @Test
  fun `carriage return is sanitised`() {
    assertThat(("SomeText\rWithHiddenCR").removeAllCrLf()).isEqualTo("SomeTextWithHiddenCR")
  }

  @Test
  fun `linefeed is sanitised`() {
    assertThat(("SomeText\nWithHiddenLF").removeAllCrLf()).isEqualTo("SomeTextWithHiddenLF")
  }

  @Test
  fun `newline is sanitised`() {
    assertThat(("SomeText\r\nWithHiddenCRLF").removeAllCrLf()).isEqualTo("SomeTextWithHiddenCRLF")
  }

  @Test
  fun `multiple carriage returns and linefeeds are sanitised`() {
    assertThat(("Some\nText\r\nWith\r\nMultipleH\ridden\nCRAnd\rLF").removeAllCrLf()).isEqualTo("SomeTextWithMultipleHiddenCRAndLF")
  }
}
