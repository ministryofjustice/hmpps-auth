package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(DeliusExtension::class, TokenVerificationExtension::class)
class HealthIntTest : IntegrationTest() {
  @Test
  fun `Health page reports ok`() {
    webTestClient.get().uri("/auth/health")
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health reports delius info`() {
    webTestClient.get().uri("/auth/health/deliusApiHealth")
        .exchange()
        .expectBody().jsonPath("status").isEqualTo("UP")
        .jsonPath("details.HttpStatus").isEqualTo("OK")
  }

  @Test
  fun `Health reports token verification info`() {
    webTestClient.get().uri("/auth/health/tokenVerificationApiHealth")
        .exchange()
        .expectBody().jsonPath("status").isEqualTo("UP")
        .jsonPath("details.HttpStatus").isEqualTo("OK")
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
    webTestClient.get().uri("/auth/health/db")
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("components.authDataSource.details.database").isEqualTo("H2")
        .jsonPath("components.authDataSource.details.validationQuery").isEqualTo("isValid()")
  }
}
