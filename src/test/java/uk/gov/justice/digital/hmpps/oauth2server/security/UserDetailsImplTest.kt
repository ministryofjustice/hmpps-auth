package uk.gov.justice.digital.hmpps.oauth2server.security

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test


class UserDetailsImplTest {
  @Test
  fun toUser() {
    assertThatThrownBy { UserDetailsImpl("user", "name", listOf(), "none", "userId", "jwtId").toUser() }
        .isInstanceOf(IllegalStateException::class.java)
  }
}
