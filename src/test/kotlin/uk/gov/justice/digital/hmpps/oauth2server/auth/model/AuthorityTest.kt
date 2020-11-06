package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority.Companion.removeRolePrefixIfNecessary

internal class AuthorityTest {
  @Test
  fun testConstructor_addsRole() {
    assertThat(Authority("BOB", "bloggs").authority).isEqualTo("ROLE_BOB")
  }

  @Test
  fun testConstructor_unecessary() {
    assertThat(Authority("ROLE_BOB", "bloggs").authority).isEqualTo("ROLE_BOB")
  }

  @Test
  fun authorityName() {
    assertThat(Authority("ROLE_BOB", "bloggs").roleCode).isEqualTo("BOB")
  }

  @Test
  fun removeRolePrefixIfNecessary_necessary() {
    assertThat(removeRolePrefixIfNecessary("ROLE_BOB")).isEqualTo("BOB")
  }

  @Test
  fun removeRolePrefixIfNecessary_unnecessary() {
    assertThat(removeRolePrefixIfNecessary("BOB")).isEqualTo("BOB")
  }
}
