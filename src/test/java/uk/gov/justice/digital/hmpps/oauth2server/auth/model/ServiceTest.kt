package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@Suppress("UsePropertyAccessSyntax")
class ServiceTest {
  @Test
  fun roles() {
    val service = Service("CODE", "NAME", "Description", "SOME_ROLE, SOME_OTHER_ROLE", "http://some.url", true, "a@b.com")
    assertThat(service.roles).containsExactly("SOME_ROLE", "SOME_OTHER_ROLE")
  }

  @Test
  fun roles_empty() {
    val service = Service("CODE", "NAME", "Description", null, "http://some.url", true, "a@b.com")
    assertThat(service.roles).isEmpty()
  }

  @Test
  fun isUrlInsteadOfEmail() {
    val service = Service("CODE", "NAME", "Description", null, "http://some.url", true, "a@b.com")
    assertThat(service.isUrlInsteadOfEmail).isFalse()
  }

  @Test
  fun isUrlInsteadOfEmail_true() {
    val service = Service("CODE", "NAME", "Description", null, "http://some.url", true, "http://some.url")
    assertThat(service.isUrlInsteadOfEmail).isTrue()
  }
}
