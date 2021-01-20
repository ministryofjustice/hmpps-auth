package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest

@TestPropertySource(properties = ["jwt.jwk.key.id=some-key-id"])
class JwkSetIntTest : IntegrationTest() {
  @Test
  fun `Jwk set page returns JwkSet`() {
    webTestClient.get().uri("/.well-known/jwks.json")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody().jsonPath("keys[0].kid").isEqualTo("some-key-id")
  }
}
