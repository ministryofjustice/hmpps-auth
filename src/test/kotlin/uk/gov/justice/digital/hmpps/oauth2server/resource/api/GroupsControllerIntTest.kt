package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest

class GroupsControllerIntTest : IntegrationTest() {

  @Test
  fun `Group details endpoint returns details of group when user has ROLE_MAINTAIN_OAUTH_USERS`() {
    webTestClient
      .get().uri("/auth/api/groups/SITE_1_GROUP_2")
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
      .json("group_details_data.json".readFile())
  }

  @Test
  fun `Group details endpoint returns details of group when user is able to maintain group`() {
    webTestClient
      .get().uri("/auth/api/groups/SITE_1_GROUP_2")
      .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
      .json("group_details_data.json".readFile())
  }

  @Test
  fun `Group details endpoint returns error when user is not allowed to maintain group`() {
    webTestClient
      .get().uri("/auth/api/groups/SITE_1_GROUP_2")
      .headers(setAuthorisation("AUTH_USER", listOf("ROLE_AUTH_GROUP_MANAGER")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "error" to "Group not with your groups",
            "error_description" to "Unable to maintain group: SITE_1_GROUP_2 with reason: Group not with your groups",
            "field" to "group"
          )
        )
      }
  }
  @Test
  fun `Group details endpoint returns forbidden when dose not have admin role `() {
    webTestClient
      .get().uri("/auth/api/groups/SITE_1_GROUP_2")
      .headers(setAuthorisation("bob"))
      .exchange()
      .expectStatus().isForbidden
      .expectBody()
      .json(
        """
      {"error":"access_denied","error_description":"Access is denied"}
        """.trimIndent()
      )
  }

  @Test
  fun `Group details endpoint returns error when group not found user has ROLE_MAINTAIN_OAUTH_USERS`() {
    webTestClient
      .get().uri("/auth/api/groups/bob")
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "error" to "Not Found",
            "error_description" to "Unable to maintain group: bob with reason: notfound",
            "field" to "group"
          )
        )
      }
  }

  @Test
  fun `Group details endpoint returns error when group not found`() {
    webTestClient
      .get().uri("/auth/api/groups/bob")
      .headers(setAuthorisation("AUTH_USER", listOf("ROLE_AUTH_GROUP_MANAGER")))
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "error" to "Not Found",
            "error_description" to "Unable to maintain group: bob with reason: notfound",
            "field" to "group"
          )
        )
      }
  }

  @Test
  fun `Group details endpoint not accessible without valid token`() {
    webTestClient.get().uri("/auth/api/groups/GLOBAL_SEARCH")
      .exchange()
      .expectStatus().isUnauthorized
  }
}
