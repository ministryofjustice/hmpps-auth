package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest

class PingIntTest : IntegrationTest() {
  @Test
  fun `Ping returns pong`() {
    webTestClient.get().uri("/auth/ping")
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType("text/plain;charset=UTF-8")
        .expectBody(String::class.java).isEqualTo<Nothing>("pong")
  }
}
