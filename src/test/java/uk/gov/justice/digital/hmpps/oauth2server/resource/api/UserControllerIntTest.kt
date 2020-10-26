package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.oauth2server.resource.DeliusExtension
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest

@ExtendWith(DeliusExtension::class)
class UserControllerIntTest : IntegrationTest() {

  @Test
  fun `User Me endpoint returns principal user data for client credentials grant`() {
    webTestClient
      .get().uri("/auth/api/user/me")
      .headers(setAuthorisation("ITAG_USER"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "username" to "ITAG_USER",
            "active" to true,
            "name" to "Itag User",
            "staffId" to 1,
            "activeCaseLoadId" to "MDI",
            "authSource" to "nomis",
            "userId" to "1"
          )
        )
      }
  }

  @Test
  fun `User Me endpoint returns principal user data for auth user`() {
    webTestClient
      .get().uri("/auth/api/user/me")
      .headers(setAuthorisation("AUTH_USER"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "username" to "AUTH_USER",
            "active" to true,
            "name" to "Auth Only",
            "authSource" to "auth",
            "userId" to "608955ae-52ed-44cc-884c-011597a77949",
          )
        )
      }
  }

  @Test
  fun `User Me endpoint returns principal user data for delius user`() {
    webTestClient
      .get().uri("/auth/api/user/me")
      .headers(setAuthorisation("DELIUS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "username" to "DELIUS",
            "active" to true,
            "name" to "Delius Smith",
            "authSource" to "delius",
            "userId" to "2500077027",
          )
        )
      }
  }

  @Test
  fun `User username endpoint returns user data`() {
    webTestClient
      .get().uri("/auth/api/user/RO_USER")
      .headers(setAuthorisation("ITAG_USER"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "username" to "RO_USER",
            "active" to true,
            "name" to "Licence Responsible Officer",
            "authSource" to "nomis",
            "staffId" to 4,
            "activeCaseLoadId" to "BEL",
            "userId" to "4",
          )
        )
      }
  }

  @Test
  fun `User username endpoint returns user data for auth user`() {
    webTestClient
      .get().uri("/auth/api/user/AUTH_USER")
      .headers(setAuthorisation("ITAG_USER"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "username" to "AUTH_USER",
            "active" to true,
            "name" to "Auth Only",
            "authSource" to "auth",
            "userId" to "608955ae-52ed-44cc-884c-011597a77949",
          )
        )
      }
  }

  @Test
  fun `User username endpoint returns user data for delius user`() {
    webTestClient
      .get().uri("/auth/api/user/me")
      .headers(setAuthorisation("delius"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "username" to "DELIUS",
            "active" to true,
            "name" to "Delius Smith",
            "authSource" to "delius",
            "userId" to "2500077027",
          )
        )
      }
  }

  @Test
  fun `User email endpoint returns user data for auth user`() {
    webTestClient
      .get().uri("/auth/api/user/AUTH_USER/email")
      .headers(setAuthorisation("ITAG_USER"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "username" to "AUTH_USER",
            "email" to "auth_user@digital.justice.gov.uk",
          )
        )
      }
  }

  @Test
  fun `User email endpoint returns user data for nomis user`() {
    webTestClient
      .get().uri("/auth/api/user/ITAG_USER/email")
      .headers(setAuthorisation("ITAG_USER"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "username" to "ITAG_USER",
            "email" to "itag_user@digital.justice.gov.uk",
          )
        )
      }
  }

  @Test
  fun `User email endpoint returns user data for delius user`() {
    webTestClient
      .get().uri("/auth/api/user/delius_email/email")
      .headers(setAuthorisation("delius_email"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "username" to "DELIUS_EMAIL",
            "email" to "delius_user@digital.justice.gov.uk",
          )
        )
      }
  }

  @Test
  fun `User email endpoint returns no user data for unverified email address`() {
    webTestClient
      .get().uri("/auth/api/user/DM_USER/email")
      .headers(setAuthorisation("ITAG_USER"))
      .exchange()
      .expectStatus().isNoContent
  }

  @Test
  fun `User Me endpoint not accessible without valid token`() {
    webTestClient
      .get().uri("/auth/api/user/me")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `User Me Roles endpoint not accessible without valid token`() {
    webTestClient
      .get().uri("/auth/api/user/me/roles")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `User username endpoint not accessible without valid token`() {
    webTestClient
      .get().uri("/auth/api/user/bob")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `User email endpoint not accessible without valid token`() {
    webTestClient
      .get().uri("/auth/api/user/bob/email")
      .exchange()
      .expectStatus().isUnauthorized
  }
}
