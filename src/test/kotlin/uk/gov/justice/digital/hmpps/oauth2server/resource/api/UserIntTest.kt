package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest

class UserIntTest : IntegrationTest() {
  @Test
  fun `User Me endpoint returns principal user data`() {
    webTestClient
      .get().uri("/api/user/ITAG_USER")
      .headers(setAuthorisation("ITAG_USER_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json("user_data.json".readFile())
  }
}
