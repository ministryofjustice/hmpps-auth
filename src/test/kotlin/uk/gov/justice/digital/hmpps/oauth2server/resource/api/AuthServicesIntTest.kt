package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest

class AuthServicesIntTest : IntegrationTest() {
  @Test
  fun `Auth Services endpoint returns all possible enabled services`() {
    webTestClient
      .get().uri("/api/services")
      .headers(setAuthorisation("AUTH_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath(".[?(@.code == 'NOMIS')]")
      .isEqualTo(
        mapOf(
          "code" to "NOMIS",
          "name" to "Digital Prison Service",
          "description" to "View and Manage Offenders in Prison (Old name was NEW NOMIS)",
          "contact" to "feedback@digital.justice.gov.uk",
          "url" to "http://localhost:3000"
        )
      )
      .jsonPath(".[*].code").value<List<String>> {
        assertThat(it).hasSizeGreaterThan(5)
      }
  }

  @Test
  fun `Auth Services endpoint returns my possible enabled services`() {
    webTestClient
      .get().uri("/api/services/me")
      .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_PRISON")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath(".[?(@.code == 'NOMIS')]")
      .isEqualTo(
        mapOf(
          "code" to "NOMIS",
          "name" to "Digital Prison Service",
          "description" to "View and Manage Offenders in Prison (Old name was NEW NOMIS)",
          "contact" to "feedback@digital.justice.gov.uk",
          "url" to "http://localhost:3000"
        )
      )
      .jsonPath(".[*].code").value<List<String>> {
        assertThat(it).containsExactlyInAnyOrder("NOMIS", "DETAILS")
      }
  }

  @Test
  fun `Auth Roles endpoint accessible without valid token`() {
    webTestClient.get().uri("/api/services")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath(".[*].code").value<List<String>> {
        assertThat(it).hasSizeGreaterThan(5)
      }
  }

  @Test
  fun `Auth Roles Me endpoint inaccessible without valid token`() {
    webTestClient.get().uri("/api/services/me")
      .exchange()
      .expectStatus().isUnauthorized
  }
}
