package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest

class AuthAllGroupsIntTest : IntegrationTest() {
  @Test
  fun `Auth Groups endpoint returns all possible auth groups`() {
    webTestClient
      .get().uri("/api/authgroups")
      .headers(setAuthorisation("AUTH_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath(".[?(@.groupCode == 'SITE_1_GROUP_1')]")
      .isEqualTo(mapOf("groupCode" to "SITE_1_GROUP_1", "groupName" to "Site 1 - Group 1"))
      .jsonPath(".[*].groupCode").value<List<String>> {
        assertThat(it).hasSizeGreaterThan(2)
      }
  }

  @Test
  fun `Auth Groups endpoint not accessible without valid token`() {
    webTestClient.get().uri("/api/authgroups")
      .exchange()
      .expectStatus().isUnauthorized
  }
}
