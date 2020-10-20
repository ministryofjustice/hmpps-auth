package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.oauth2server.resource.DeliusExtension
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest

@ExtendWith(DeliusExtension::class)
class AuthUserIntTest : IntegrationTest() {
  data class NewUser(val email: String, val firstName: String, val lastName: String, val groupCode: String? = null)

  @Test
  fun `Create User endpoint succeeds to create user data`() {
    val username = RandomStringUtils.randomAlphanumeric(10)
    val user = NewUser("bob@bobdigital.justice.gov.uk", "Bob", "Smith")

    webTestClient
      .put().uri("/auth/api/authuser/$username").bodyValue(user)
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isOk

    webTestClient
      .get().uri("/auth/api/user/$username")
      .headers(setAuthorisation("ITAG_USER_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it.filter { it.key != "userId" }).containsExactlyInAnyOrderEntriesOf(
          mapOf("username" to username.toUpperCase(), "active" to true, "name" to "Bob Smith", "authSource" to "auth")
        )
      }
  }

  @Test
  fun `Create User endpoint fails if user with email already exists and unique is enabled`() {
    val username = RandomStringUtils.randomAlphanumeric(10)
    val user = NewUser("auth_test@digital.justice.gov.uk", "Bob", "Smith")

    webTestClient
      .put().uri("/auth/api/authuser/$username?enforceUniqueEmail=true").bodyValue(user)
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
  }

  @Test
  fun `Create User endpoint succeeds to create user data with group and roles`() {
    val username = RandomStringUtils.randomAlphanumeric(10)
    val user = NewUser("bob@bobdigital.justice.gov.uk", "Bob", "Smith", "SITE_1_GROUP_1")

    webTestClient
      .put().uri("/auth/api/authuser/$username").bodyValue(user)
      .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
      .exchange()
      .expectStatus().isOk

    webTestClient
      .get().uri("/auth/api/authuser/$username/groups")
      .headers(setAuthorisation("ITAG_USER_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.[*].groupCode").value<List<String>> {
        assertThat(it).containsOnly("SITE_1_GROUP_1")
      }
      .jsonPath("$.[*].groupName").value<List<String>> {
        assertThat(it).containsOnly("Site 1 - Group 1")
      }

    webTestClient
      .get().uri("/auth/api/authuser/$username/roles")
      .headers(setAuthorisation("ITAG_USER_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.[*].roleCode").value<List<String>> {
        assertThat(it).isEqualTo(listOf("GLOBAL_SEARCH", "LICENCE_RO"))
      }
  }

  @Test
  fun `Create User endpoint fails if no privilege`() {
    val username = RandomStringUtils.randomAlphanumeric(10)
    val user = NewUser("bob@bobdigital.justice.gov.uk", "Bob", "Smith")

    webTestClient
      .put().uri("/auth/api/authuser/$username").bodyValue(user)
      .headers(setAuthorisation("ITAG_USER_ADM"))
      .exchange()
      .expectStatus().isForbidden
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf("error" to "access_denied", "error_description" to "Access is denied")
        )
      }
  }

  @Test
  fun `Create User endpoint returns failure message if delius server error`() {
    val username = "DELIUS_ERROR_SERVER"
    val user = NewUser("bob@bobdigital.justice.gov.uk", "Bob", "Smith")

    webTestClient
      .put().uri("/auth/api/authuser/$username").bodyValue(user)
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isUnauthorized
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "error" to "unauthorized",
            "error_description" to "Unable to retrieve information for DELIUS_ERROR_SERVER from Delius.  We are unable to connect to Delius or there is an issue with DELIUS_ERROR_SERVER in Delius"
          )
        )
      }
  }

  @Test
  fun `Auth User endpoint returns user data`() {
    webTestClient
      .get().uri("/auth/api/authuser/AUTH_USER_LAST_LOGIN")
      .headers(setAuthorisation("AUTH_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json("auth_user_data.json".readFile())
  }

  @Test
  fun `Auth User endpoint returns no data for nomis user`() {
    webTestClient
      .get().uri("/auth/api/authuser/ITAG_USER")
      .headers(setAuthorisation("AUTH_ADM"))
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json("auth_user_not_found.json".readFile())
  }

  @Test
  fun `Auth User email endpoint returns user data`() {
    webTestClient
      .get().uri("/auth/api/authuser?email=auth_test2@digital.justice.gov.uk")
      .headers(setAuthorisation("AUTH_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json("auth_user_emails.json".readFile())
  }

  @Test
  fun `Auth User email endpoint returns no data if not found`() {
    webTestClient
      .get().uri("/auth/api/authuser?email=nobody@nowhere")
      .headers(setAuthorisation("AUTH_ADM"))
      .exchange()
      .expectStatus().isNoContent
  }

  @Test
  fun `Auth User search endpoint returns user data`() {
    webTestClient
      .get().uri("/auth/api/authuser/search?name=test2")
      .headers(setAuthorisation("AUTH_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json("auth_user_search.json".readFile())
  }
}
