@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class ClientTest {
  @Nested
  inner class authoritiesWithoutPrefix {
    @Test
    fun empty() {
      assertThat(Client("id").authoritiesWithoutPrefix).isEmpty()
    }

    @Test
    fun `removes role`() {
      assertThat(Client("id", authorities = listOf("bob", "ROLE_joe")).authoritiesWithoutPrefix).isEqualTo(
        listOf("bob", "joe")
      )
    }
  }
}
