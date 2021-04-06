package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest
import java.util.Base64

class DuplicateClientControllerIntTest : IntegrationTest() {

  @Test
  fun `duplicate client endpoint returns new clientId and clientSecret when request has ROLE_OAUTH_ADMIN`() {
    val encodedClientAndSecret = convertToBase64("duplicate-client-client", "clientsecret")
    val token = getClientCredentialsToken(encodedClientAndSecret)

    webTestClient
      .put().uri("/api/client/rotation-test-client-2")
      .header("Authorization", "Bearer $token")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$.clientId").isEqualTo("rotation-test-client-3")
      .jsonPath("$.clientSecret").isNotEmpty
  }

  @Test
  fun `duplicate client endpoint returns error when client not found request has ROLE_OAUTH_ADMIN`() {
    val encodedClientAndSecret = convertToBase64("duplicate-client-client", "clientsecret")
    val token = getClientCredentialsToken(encodedClientAndSecret)

    webTestClient
      .put().uri("/api/client/max-duplicate-client-2")
      .header("Authorization", "Bearer $token")
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json("""{"error":"MaxDuplicateReached","error_description":"Duplicate clientId failed for max-duplicate-client-2 with reason: MaxReached","field":"client"}""")
  }

  @Test
  fun `duplicate client endpoint returns not found when client not found request has ROLE_OAUTH_ADMIN`() {
    val encodedClientAndSecret = convertToBase64("duplicate-client-client", "clientsecret")
    val token = getClientCredentialsToken(encodedClientAndSecret)

    webTestClient
      .put().uri("/api/client/not-a-client")
      .header("Authorization", "Bearer $token")
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json("""{"error":"Not Found","error_description":"No client with requested id: not-a-client","field":"client"}""")
  }

  @Test
  fun `duplicate client endpoint requires role`() {
    val encodedClientAndSecret = convertToBase64("max-duplicate-client", "clientsecret")
    val token = getClientCredentialsToken(encodedClientAndSecret)

    webTestClient
      .put().uri("/api/client/not-a-client")
      .header("Authorization", "Bearer $token")
      .exchange()
      .expectStatus().isForbidden
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json("""{"error":"access_denied","error_description":"Access is denied"}""")
  }

  private fun convertToBase64(client: String, secret: String): String =
    Base64.getEncoder().encodeToString("$client:$secret".toByteArray())

  private fun getClientCredentialsToken(encodedClientAndSecret: String): String {
    val result =
      webTestClient
        .post().uri("/oauth/token?grant_type=client_credentials")
        .header("Authorization", "Basic $encodedClientAndSecret")
        .exchange()
        .expectStatus().isOk
        .expectBody<String>().returnResult()

    return JSONObject(result.responseBody).get("access_token") as String
  }
}
