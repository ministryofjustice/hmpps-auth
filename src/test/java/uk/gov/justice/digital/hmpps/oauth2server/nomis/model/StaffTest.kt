package uk.gov.justice.digital.hmpps.oauth2server.nomis.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StaffTest {
  @Test
  fun firstName() {
    val staff = Staff(firstName = "boB", lastName = "Smith", staffId = 1, status = "INACTIVE")
    assertThat(staff.getFirstName()).isEqualTo("Bob")
  }

  @Test
  fun name() {
    val staff = Staff(firstName = "boB", lastName = "SMITH", staffId = 1, status = "INACTIVE")
    assertThat(staff.name).isEqualTo("Bob Smith")
  }
}
