package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_DATE
import java.util.function.Consumer

class InfoIntTest : IntegrationTest() {
  @Test
  fun `Info page contains git information`() {
    webTestClient.get().uri("/auth/info")
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("git.commit.id").isNotEmpty
  }

  @Test
  fun `Info page reports build gradle variable`() {
    val ciSet = !System.getenv("CI").isNullOrBlank()
    webTestClient.get().uri("/auth/info")
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("build.continuousIntegration").isEqualTo(ciSet.toString())
  }

  @Test
  fun `Info page reports version`() {
    webTestClient.get().uri("/auth/info")
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("build.version").value(Consumer<String> {
          assertThat(it).startsWith(LocalDateTime.now().format(ISO_DATE))
        })
  }
}
