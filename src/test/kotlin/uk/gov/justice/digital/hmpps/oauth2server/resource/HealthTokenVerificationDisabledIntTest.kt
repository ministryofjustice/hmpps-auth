package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = ["tokenverification.enabled=false"]
)
class HealthTokenVerificationDisabledIntTest : IntegrationTest() {
  @Test
  fun `Health reports delius info`() {
    webTestClient.get().uri("/health/tokenVerificationApiHealth")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("status").isEqualTo("UP")
      .jsonPath("details.VerificationDisabled").isEqualTo("token verification is disabled")
  }
}
