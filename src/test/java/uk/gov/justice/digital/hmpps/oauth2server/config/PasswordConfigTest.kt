package uk.gov.justice.digital.hmpps.oauth2server.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

internal class PasswordConfigTest {
  private val passwordEncoder = PasswordConfig().passwordEncoder()

  @Test
  fun encodePassword() {
    val encoded = passwordEncoder.encode("some_password_123456")
    assertThat(encoded).hasSize(68).startsWith("{bcrypt}")
  }

  @Test
  fun testMatchesWithNoDefaultPasswordEncoding() {
    val encodedPassword = BCryptPasswordEncoder().encode("some_pass")
    assertThat(passwordEncoder.matches("some_pass", encodedPassword)).isTrue()
  }
}
