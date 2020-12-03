package uk.gov.justice.digital.hmpps.oauth2server.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OracleSha1PasswordEncoderTest {
  @Test
  fun matches_testMatchesOraclePassword() {
    val encodedPassword = "S:39BA463D55E5C8936A6798CC37B1347BA8BEC37B6407397EB769BC356F0C"
    assertThat(OracleSha1PasswordEncoder().matches("somepass1", encodedPassword)).isTrue()
  }

  @Test
  fun matches_invalidPasswordLength() {
    val encodedPassword = "somepasswordthatshouldbeencoded"
    assertThat(OracleSha1PasswordEncoder().matches("somepass1", encodedPassword)).isFalse()
  }

  @Test
  fun matches_nullInput() {
    assertThat(OracleSha1PasswordEncoder().matches("somepass1", null)).isFalse()
  }
}
