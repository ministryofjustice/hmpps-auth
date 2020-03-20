package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest


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
        .expectBody(object : ParameterizedTypeReference<Map<String, String>>() {})
        .consumeWith<Nothing?> {
          assertThat(it!!.responseBody!!.filter { it.key != "userId" }).containsExactlyEntriesOf(
              mapOf("username" to username.toUpperCase(), "active" to "true", "name" to "Bob Smith", "authSource" to "auth"))
        }
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
        .expectBody(object : ParameterizedTypeReference<Map<String, String>>() {})
        .consumeWith<Nothing?> {
          assertThat(it!!.responseBody!!).containsExactlyEntriesOf(
              mapOf("error" to "access_denied", "error_description" to "Access is denied"))
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

  private fun String.readFile(): String = this@AuthUserIntTest::class.java.getResource(this).readText()
}

