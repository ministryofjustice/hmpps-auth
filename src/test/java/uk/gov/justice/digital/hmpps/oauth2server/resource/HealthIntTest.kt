package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.junit.jupiter.api.Test

class HealthIntTest : IntegrationTest() {
  @Test
  fun `Health page reports ok`() {
    webTestClient.get().uri("/auth/health")
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health ping reports ok`() {
    webTestClient.get().uri("/auth/health/ping")
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health reports db info`() {
    webTestClient.get().uri("/auth/health")
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("components.db.components.authDataSource.details.database").isEqualTo("H2")
        .jsonPath("components.db.components.authDataSource.details.result").isEqualTo(1)
        .jsonPath("components.db.components.authDataSource.details.validationQuery").isEqualTo("SELECT 1")
  }
}
