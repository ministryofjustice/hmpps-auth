package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest

class PrisonUserControllerIntTest : IntegrationTest() {

  @Test
  fun `prisonuser end-point returns results`() {
    webTestClient
        .get().uri("/auth/api/prisonusers?firstName=ryAn&lastName=OrtoN")
        .headers(setAuthorisation("UOF_REVIEWER_USER", listOf()))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .json("""
          [{
            "username":"RO_USER_TEST",
            "firstName":null,
            "lastName":null,
            "emailAddress":"ro_user_test@digital.justice.gov.uk",
            "verified":true
        }]
        """.trimIndent())
  }

  @Test
  fun `prisonuser end-point rejects invalid query`() {
    webTestClient
        .get().uri("/auth/api/prisonusers")
        .headers(setAuthorisation("UOF_REVIEWER_USER", listOf()))
        .exchange()
        .expectStatus().is4xxClientError
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
  }
}