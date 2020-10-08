package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest

class AuthorisationIssuerIntTest : IntegrationTest() {
  @Test
  fun `Metadata page responds and can be parsed`() {
    webTestClient.get().uri("/auth/issuer/.well-known/openid-configuration")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody().jsonPath("issuer").value<String> {
        assertThat(it).contains("http://localhost")
      }
  }
}
