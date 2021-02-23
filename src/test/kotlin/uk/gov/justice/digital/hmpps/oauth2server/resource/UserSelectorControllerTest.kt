@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class UserSelectorControllerTest {
  @Test
  fun userSelector() {
    val forward = UserSelectorController().approveOrDeny()
    assertThat(forward).isEqualTo("forward:/oauth/authorize")
  }
}
