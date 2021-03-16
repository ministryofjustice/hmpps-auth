package uk.gov.justice.digital.hmpps.oauth2server.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class StringSanitiserTest {
  @Test
  fun `carriage return is sanitised`() {
    assertThat(("SomeUser\rWithHiddenCR").sanitise()).isEqualTo("SomeUserWithHiddenCR")
  }

  @Test
  fun `linefeed is sanitised`() {
    assertThat(("SomeUser\nWithHiddenCR").sanitise()).isEqualTo("SomeUserWithHiddenCR")
  }

  @Test
  fun `newline is sanitised`() {
    assertThat(("SomeUser\r\nWithHiddenCR").sanitise()).isEqualTo("SomeUserWithHiddenCR")
  }
}
