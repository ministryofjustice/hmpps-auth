package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest

class AuthAllRolesIntTest : IntegrationTest() {
  @Test
  fun `Auth Roles endpoint returns all possible auth roles`() {
    webTestClient
        .get().uri("/auth/api/authroles")
        .headers(setAuthorisation("AUTH_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath(".[?(@.roleCode == 'GLOBAL_SEARCH')]").isEqualTo(mapOf("roleCode" to "GLOBAL_SEARCH", "roleName" to "Global Search"))
        .jsonPath(".[*].roleCode").value<List<String>> {
          assertThat(it).hasSizeGreaterThan(2)
        }
  }

  @Test
  fun `Auth Roles endpoint not accessible without valid token`() {
    webTestClient.get().uri("/auth/api/authroles")
        .exchange()
        .expectStatus().isUnauthorized
  }
}
