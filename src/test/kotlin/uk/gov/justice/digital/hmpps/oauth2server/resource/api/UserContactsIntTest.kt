package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest

class UserContactsIntTest : IntegrationTest() {
  @Test
  fun `User contacts endpoint returns contact information`() {
    webTestClient
      .get().uri("/auth/api/user/AUTH_MFA_PREF_EMAIL/contacts")
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_RETRIEVE_OAUTH_CONTACTS")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json("user_contacts.json".readFile())
  }

  @Test
  fun `User contacts endpoint returns forbidden if missing role`() {
    webTestClient
      .get().uri("/auth/api/user/AUTH_MFA_PREF_EMAIL/contacts")
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
  fun `User contacts endpoint returns not found response if user not found`() {
    webTestClient
      .get().uri("/auth/api/user/USER_DOESNT_EXIST/contacts")
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_RETRIEVE_OAUTH_CONTACTS")))
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "error" to "Not Found",
            "error_description" to "User with username USER_DOESNT_EXIST not found",
            "field" to "username"
          )
        )
      }
  }
}
