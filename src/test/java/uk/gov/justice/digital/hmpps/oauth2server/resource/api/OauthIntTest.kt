package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest

@Suppress("DEPRECATION")
class OauthIntTest : IntegrationTest() {
  @Test
  fun `Existing auth code stored in database can be redeemed for access token`() {
    val authCode = "03sn0c" // from database oauth_code table
    val clientUrl = "http://localhost:8081/login" // same as row in oauth_code table
    webTestClient
        .post().uri("/auth/oauth/token?grant_type=authorization_code&code=$authCode&redirect_uri=$clientUrl")
        .headers(setBasicAuthorisation("ZWxpdGUyYXBpY2xpZW50OmNsaWVudHNlY3JldA=="))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
        .expectBody()
        .jsonPath(".user_name").isEqualTo("ITAG_USER_ADM")
        .jsonPath(".user_id").isEqualTo("1")
        .jsonPath(".sub").isEqualTo("ITAG_USER_ADM")
        .jsonPath(".auth_source").isEqualTo("nomis")
  }
}
