package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.junit.jupiter.api.Test
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest
import java.util.concurrent.TimeUnit

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
  @Test
  fun `Jwk set page is cacheable`() {
    webTestClient.get().uri("/.well-known/jwks.json")
      .exchange()
      .expectHeader().cacheControl(CacheControl.maxAge(12, TimeUnit.HOURS).cachePublic())
  }
}
