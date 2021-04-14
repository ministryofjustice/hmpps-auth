package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest

class GroupsControllerIntTest : IntegrationTest() {

  @Nested
  inner class GroupDetails {
    @Test
    fun `Group details endpoint returns details of group when user has ROLE_MAINTAIN_OAUTH_USERS`() {
      webTestClient
        .get().uri("/api/groups/SITE_1_GROUP_2")
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
        .get().uri("/api/groups/SITE_1_GROUP_2")
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
        .get().uri("/api/groups/SITE_1_GROUP_2")
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
        .get().uri("/api/groups/SITE_1_GROUP_2")
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
        .get().uri("/api/groups/bob")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "error" to "Not Found",
              "error_description" to "Unable to get group: bob with reason: notfound",
              "field" to "group"
            )
          )
        }
    }

    @Test
    fun `Group details endpoint returns error when group not found`() {
      webTestClient
        .get().uri("/api/groups/bob")
        .headers(setAuthorisation("AUTH_USER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "error" to "Not Found",
              "error_description" to "Unable to get group: bob with reason: notfound",
              "field" to "group"
            )
          )
        }
    }

    @Test
    fun `Group details endpoint not accessible without valid token`() {
      webTestClient.get().uri("/api/groups/GLOBAL_SEARCH")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Delete Group - no child groups and no members`() {
      webTestClient.delete().uri("/api/groups/GC_DEL_1")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Delete Group - no child groups and but has members`() {
      webTestClient.delete().uri("/api/groups/GC_DEL_2")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Delete Group - has child groups`() {
      webTestClient.delete().uri("/api/groups/GC_DEL_3")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "error" to "child group exist",
              "error_description" to "Unable to delete group: GC_DEL_3 with reason: child group exist",
              "field" to "group"
            )
          )
        }
    }

    @Test
    fun `Delete Child Group endpoint returns forbidden when dose not have admin role`() {
      webTestClient.delete().uri("/api/groups/GC_DEL_1")
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
    fun `Delete Child Group details endpoint not accessible without valid token`() {
      webTestClient.delete().uri("/api/groups/GC_DEL_1")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class ChangeGroupName {
    @Test
    fun `Change group name`() {
      webTestClient
        .put().uri("/api/groups/SITE_9_GROUP_1")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(BodyInserters.fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change group name endpoint returns forbidden when dose not have admin role `() {
      webTestClient
        .put().uri("/api/groups/SITE_9_GROUP_1")
        .headers(setAuthorisation("bob"))
        .body(BodyInserters.fromValue(mapOf("groupName" to "new group name")))
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
    fun `Change group name returns error when group not found`() {
      webTestClient
        .put().uri("/api/groups/Not_A_Group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(BodyInserters.fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "error" to "Not Found",
              "error_description" to "Unable to maintain group: Not_A_Group with reason: notfound",
              "field" to "group"
            )
          )
        }
    }

    @Test
    fun `Group details endpoint not accessible without valid token`() {
      webTestClient.put().uri("/api/groups/SITE_9_GROUP_1")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class CreateChildGroup {
    @Test
    fun `Create child group`() {
      webTestClient
        .post().uri("/api/groups/child")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "parentGroupCode" to "SITE_9_GROUP_1",
              "groupCode" to "CG",
              "groupName" to "Child groupie"
            )
          )
        )
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Create child group error`() {
      webTestClient
        .post().uri("/api/groups/child")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "parentGroupCode" to "",
              "groupCode" to "",
              "groupName" to ""
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `Create child group endpoint returns forbidden when dose not have admin role `() {
      webTestClient
        .post().uri("/api/groups/child")
        .headers(setAuthorisation("bob"))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "parentGroupCode" to "SITE_9_GROUP_1",
              "groupCode" to "CG3",
              "groupName" to "Child groupie 3"
            )
          )
        )
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
    fun `Create child group - group already exists`() {
      webTestClient
        .post().uri("/api/groups/child")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "parentGroupCode" to "SITE_9_GROUP_1",
              "groupCode" to "CG1",
              "groupName" to "Child groupie 1"
            )
          )
        )
        .exchange()
        .expectStatus().isOk

      webTestClient
        .post().uri("/api/groups/child")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "parentGroupCode" to "SITE_9_GROUP_1",
              "groupCode" to "CG1",
              "groupName" to "Child groupie 1"
            )
          )
        )
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "error" to "group code already exists",
              "error_description" to "Unable to create child group: CG1 with reason: group code already exists",
              "field" to "group"
            )
          )
        }
    }

    @Test
    fun `Create child group - parent group doesnt exist`() {

      webTestClient
        .post().uri("/api/groups/child")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "parentGroupCode" to "pg",
              "groupCode" to "CG1",
              "groupName" to "Child groupie 1"
            )
          )
        )
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "error" to "Not Found",
              "error_description" to "Unable to create group: pg with reason: ParentGroupNotFound",
              "field" to "group"
            )
          )
        }
    }

    @Test
    fun `Create Child Group endpoint not accessible without valid token`() {
      webTestClient.post().uri("/api/groups/child")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class createGroup {
    @Test
    fun `Create group`() {
      webTestClient
        .post().uri("/api/groups")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "groupCode" to "CG",
              "groupName" to " groupie"
            )
          )
        )
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Create group error`() {
      webTestClient
        .post().uri("/api/groups")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "groupCode" to "",
              "groupName" to ""
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `Create group endpoint returns forbidden when dose not have admin role `() {
      webTestClient
        .post().uri("/api/groups")
        .headers(setAuthorisation("bob"))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "groupCode" to "CG3",
              "groupName" to " groupie 3"
            )
          )
        )
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
    fun `Create group - group already exists`() {
      webTestClient
        .post().uri("/api/groups")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "groupCode" to "CG1",
              "groupName" to " groupie 1"
            )
          )
        )
        .exchange()
        .expectStatus().isOk

      webTestClient
        .post().uri("/api/groups")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "groupCode" to "CG1",
              "groupName" to " groupie 1"
            )
          )
        )
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "error" to "group code already exists",
              "error_description" to "Unable to create group: CG1 with reason: group code already exists",
              "field" to "group"
            )
          )
        }
    }
    @Test
    fun `Create group endpoint not accessible without valid token`() {
      webTestClient.post().uri("/api/groups")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class ChildGroupDetails {
    @Test
    fun `Child Group details endpoint returns details of child group when user has ROLE_MAINTAIN_OAUTH_USERS`() {
      webTestClient
        .get().uri("/api/groups/child/CHILD_2")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .json("child_group_details_data.json".readFile())
    }

    @Test
    fun `Child Group details endpoint returns forbidden when dose not have admin role `() {
      webTestClient
        .get().uri("/api/groups/child/SITE_1_GROUP_2")
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
    fun `Child Group details endpoint returns error when group not found user has ROLE_MAINTAIN_OAUTH_USERS`() {
      webTestClient
        .get().uri("/api/groups/child/bob")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "error" to "Not Found",
              "error_description" to "Unable to get group: bob with reason: notfound",
              "field" to "group"
            )
          )
        }
    }

    @Test
    fun `Child Group details endpoint not accessible without valid token`() {
      webTestClient.get().uri("/api/groups/child/GLOBAL_SEARCH")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class ChangeChildGroupName {
    @Test
    fun `Change group name`() {
      webTestClient
        .put().uri("/api/groups/child/CHILD_9")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(BodyInserters.fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change group name endpoint returns forbidden when dose not have admin role `() {
      webTestClient
        .put().uri("/api/groups/child/CHILD_9")
        .headers(setAuthorisation("bob"))
        .body(BodyInserters.fromValue(mapOf("groupName" to "new group name")))
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
    fun `Change group name returns error when group not found`() {
      webTestClient
        .put().uri("/api/groups/child/Not_A_Group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(BodyInserters.fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "error" to "Not Found",
              "error_description" to "Unable to maintain group: Not_A_Group with reason: notfound",
              "field" to "group"
            )
          )
        }
    }

    @Test
    fun `Group details endpoint not accessible without valid token`() {
      webTestClient.put().uri("/api/groups/child/CHILD_9")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class DeleteChildGroup {
    @Test
    fun `Delete Child Group`() {
      webTestClient.delete().uri("/api/groups/child/bob")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Delete Child Group endpoint returns forbidden when dose not have admin role`() {
      webTestClient.delete().uri("/api/groups/child/bob")
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
    fun `Delete Child Group details endpoint not accessible without valid token`() {
      webTestClient.delete().uri("/api/groups/child/bob")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }
}
