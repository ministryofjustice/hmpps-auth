package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.oauth2server.resource.DeliusExtension
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest

@ExtendWith(DeliusExtension::class)
class AuthUserRolesIntTest : IntegrationTest() {
  @Test
  fun `Auth User Roles add role endpoint adds a role to a user`() {
    webTestClient
      .put().uri("/api/authuser/AUTH_RO_USER/roles/licence_vary")
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isNoContent

    checkRolesForUser("AUTH_RO_USER", listOf("GLOBAL_SEARCH", "LICENCE_RO", "LICENCE_VARY"))
  }

  @Test
  fun `Auth User Roles add role endpoint adds a role to a user that already exists`() {
    webTestClient
      .put().uri("/api/authuser/AUTH_RO_USER/roles/licence_ro")
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
      .expectBody()
      .json("""{error: "role.exists", error_description: "Modify role failed for field role with reason: role.exists", field: "role"}""")
  }

  @Test
  fun `Auth User Roles add role endpoint adds a role to a user not in their group`() {
    webTestClient
      .put().uri("/api/authuser/AUTH_ADM/roles/licence_vary")
      .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.FORBIDDEN)
      .expectBody()
      .json("""{error: "User not with your groups", error_description: "Unable to maintain user: Auth Adm with reason: User not with your groups", field: "username"}""")
  }

  @Test
  fun `Auth User Roles add role endpoint adds a role that doesn't exist`() {
    webTestClient
      .put().uri("/api/authuser/AUTH_RO_USER_TEST/roles/licence_bob")
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .json("""{error: "role.notfound", error_description: "Modify role failed for field role with reason: role.notfound", field: "role"}""")
  }

  @Test
  fun `Auth User Roles add role endpoint adds a role requires role`() {
    webTestClient
      .put().uri("/api/authuser/AUTH_RO_USER_TEST/roles/licence_bob")
      .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_GLOBAL_SEARCH")))
      .exchange()
      .expectStatus().isForbidden
      .expectBody().json("""{error: "access_denied", error_description: "Access is denied"}""")
  }

  @Test
  fun `Auth User Roles remove role endpoint removes a role from a user`() {
    webTestClient
      .delete().uri("/api/authuser/AUTH_RO_USER_TEST/roles/licence_ro")
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isNoContent

    checkRolesForUser("AUTH_RO_USER_TEST", listOf("GLOBAL_SEARCH"))
  }

  @Test
  fun `Auth User Roles remove role endpoint removes a role from a user that isn't found`() {
    webTestClient
      .delete().uri("/api/authuser/AUTH_RO_USER_TEST/roles/licence_bob")
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .json("""{error: "role.notfound", error_description: "Modify role failed for field role with reason: role.notfound", field: "role"}""")
  }

  @Test
  fun `Auth User Roles remove role endpoint removes a role from a user that isn't on the user`() {
    webTestClient
      .delete().uri("/api/authuser/AUTH_RO_USER_TEST/roles/VIDEO_LINK_COURT_USER")
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .json("""{error: "role.missing", error_description: "Modify role failed for field role with reason: role.missing", field: "role"}""")
  }

  @Test
  fun `Auth User Roles remove role endpoint removes a role from a user not in their group`() {
    webTestClient
      .delete().uri("/api/authuser/AUTH_ADM/roles/licence_ro")
      .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.FORBIDDEN)
      .expectBody()
      .json("""{error: "User not with your groups", error_description: "Unable to maintain user: Auth Adm with reason: User not with your groups", field: "username"}""")
  }

  @Test
  fun `Auth User Roles remove role endpoint requires role`() {
    webTestClient
      .delete().uri("/api/authuser/AUTH_ADM/roles/licence_ro")
      .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_GLOBAL_SEARCH")))
      .exchange()
      .expectStatus().isForbidden
      .expectBody().json("""{error: "access_denied", error_description: "Access is denied"}""")
  }

  @Test
  fun `Auth User Roles endpoint returns user roles`() {
    checkRolesForUser("auth_ro_vary_user", listOf("GLOBAL_SEARCH", "LICENCE_RO", "LICENCE_VARY"))
  }

  @Test
  fun `Auth User Roles endpoint returns user roles not allowed`() {
    webTestClient
      .get().uri("/api/authuser/AUTH_ADM/roles")
      .exchange()
      .expectStatus().isUnauthorized
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json("""{error: "unauthorized", error_description: "Full authentication is required to access this resource"}""")
  }

  @Test
  fun `Auth Roles endpoint returns all assignable auth roles for a group for admin maintainer`() {
    webTestClient
      .get().uri("/api/authuser/auth_ro_vary_user/assignable-roles")
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$.[*].roleCode").value<List<String>> {
        assertThat(it).hasSizeGreaterThan(5)
        assertThat(it).contains("PECS_COURT")
      }
  }

  @Test
  fun `Auth Roles endpoint returns all assignable auth roles for a group for group manager`() {
    webTestClient
      .get().uri("/api/authuser/AUTH_RO_USER_TEST2/assignable-roles")
      .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$.[*].roleCode").value<List<String>> {
        assertThat(it).containsExactlyInAnyOrder("LICENCE_RO", "LICENCE_VARY")
      }
  }

  @Test
  fun `Auth User Roles add role POST endpoint adds a role to a user`() {
    webTestClient
      .post().uri("/api/authuser/AUTH_ADD_ROLE_TEST/roles")
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .body(BodyInserters.fromValue(listOf("GLOBAL_SEARCH", "LICENCE_RO")))
      .exchange()
      .expectStatus().isNoContent

    checkRolesForUser("AUTH_RO_USER", listOf("GLOBAL_SEARCH", "LICENCE_RO"))
  }

  @Test
  fun `Auth User Roles by userId endpoint returns user roles`() {
    checkRolesForUserId("5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8", listOf("GLOBAL_SEARCH", "LICENCE_RO", "LICENCE_VARY"))
  }

  @Test
  fun `Auth User Roles by userId endpoint returns user roles not allowed`() {
    webTestClient
      .get().uri("/api/authuser/id/5105A589-75B3-4CA0-9433-B96228C1C8F3/roles")
      .exchange()
      .expectStatus().isUnauthorized
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json("""{error: "unauthorized", error_description: "Full authentication is required to access this resource"}""")
  }

  private fun checkRolesForUser(user: String, roles: List<String>) {
    webTestClient
      .get().uri("/api/authuser/$user/roles")
      .headers(setAuthorisation("ITAG_USER_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$.[*].roleCode").value<List<String>> {
        assertThat(it).containsExactlyInAnyOrderElementsOf(roles)
      }
  }

  private fun checkRolesForUserId(userId: String, roles: List<String>) {
    webTestClient
      .get().uri("/api/authuser/id/$userId/roles")
      .headers(setAuthorisation("ITAG_USER_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$.[*].roleCode").value<List<String>> {
        assertThat(it).containsExactlyInAnyOrderElementsOf(roles)
      }
  }
}
