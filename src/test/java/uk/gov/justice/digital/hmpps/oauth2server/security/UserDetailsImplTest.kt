package uk.gov.justice.digital.hmpps.oauth2server.security

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test


class UserDetailsImplTest {
  @Test
  fun toUser() {
    assertThatThrownBy { UserDetailsImpl("user", "name", listOf(), "none", "userId").toUser() }
        .isInstanceOf(IllegalStateException::class.java)
  }
}
