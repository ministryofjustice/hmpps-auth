package uk.gov.justice.digital.hmpps.oauth2server.nomis.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StaffTest {
  @Test
  fun firstName() {
    val staff = Staff()
    staff.firstName = "boB"
    assertThat(staff.firstName).isEqualTo("Bob")
  }

  @Test
  fun name() {
    val staff = Staff()
    staff.firstName = "boB"
    staff.lastName = "SMITH"
    assertThat(staff.name).isEqualTo("Bob Smith")
  }
}
