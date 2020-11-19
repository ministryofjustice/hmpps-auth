package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.auth0.jwt.JWT
import net.minidev.json.JSONArray
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.oauth2server.resource.DeliusExtension
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest
import java.util.Base64

@Suppress("DEPRECATION")
@ExtendWith(DeliusExtension::class)
class OauthIntTest : IntegrationTest() {

  @Test
  fun `Existing auth code stored in database can be redeemed for access token`() {
    // from database oauth_code table.  To regenerate - put a breakpoint in ClientLoginSpecification just before the
    // call to get the access token.  Then go to the /auth/h2-console (blank username or password) and look at the last
    // row in the oauth_code table
    val authCode = "5bDHCW"
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

  @Test
  fun `Client Credentials Login`() {
    val encodedClientAndSecret = convertToBase64("deliusnewtech", "clientsecret")
    webTestClient
      .post().uri("/auth/oauth/token?grant_type=client_credentials")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsKey("expires_in")
        assertThat(it["expires_in"]as Int).isLessThan(3600)
        assertThat(it).doesNotContainKey("refreshToken")
      }
  }

  @Test
  fun `Client Credentials Login hold subject`() {
    val encodedClientAndSecret = convertToBase64("deliusnewtech", "clientsecret")
    webTestClient
      .post().uri("/auth/oauth/token?grant_type=client_credentials")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it["sub"]).isEqualTo("deliusnewtech")
      }
  }

  @Test
  fun `Client Credentials Login With username identifier`() {
    val encodedClientAndSecret = convertToBase64("omicadmin", "clientsecret")
    val token = getClientCredentialsTokenWithUsername(encodedClientAndSecret, "CA_USER")

    webTestClient
      .get().uri("/auth/api/user/me")
      .header("Authorization", "Bearer $token")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsAllEntriesOf(
          mapOf(
            "name" to "Licence Case Admin",
          )
        )
      }
  }

  @Test
  fun `Client Credentials Login access token`() {

    val encodedClientAndSecret = convertToBase64("omicadmin", "clientsecret")
    webTestClient
      .post().uri("/auth/oauth/token?grant_type=client_credentials&username=CA_USER")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsKey("expires_in")
        assertThat(it["expires_in"]as Int).isLessThan(3600)
        assertThat(it).doesNotContainKey("refreshToken")
        assertThat(it["auth_source"]).isEqualTo("none")
      }
  }

  @Test
  fun `Client Credentials Login access token for auth user`() {

    val encodedClientAndSecret = convertToBase64("omicadmin", "clientsecret")
    webTestClient
      .post().uri("/auth/oauth/token?grant_type=client_credentials&username=AUTH_USER")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsKey("expires_in")
        assertThat(it["expires_in"]as Int).isLessThan(3600)
        assertThat(it).doesNotContainKey("refreshToken")
        assertThat(it["auth_source"]).isEqualTo("none")
      }
  }

  @Test
  fun `Client Credentials Login access token with auth source`() {

    val encodedClientAndSecret = convertToBase64("omicadmin", "clientsecret")
    webTestClient
      .post().uri("/auth/oauth/token?grant_type=client_credentials&username=AUTH_USER&auth_source=delius")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsKey("expires_in")
        assertThat(it["expires_in"]as Int).isLessThan(3600)
        assertThat(it).doesNotContainKey("refreshToken")
        assertThat(it["auth_source"]).isEqualTo("delius")
      }
  }

  @Test
  fun `Client Credentials Login access token for proxy user with no username`() {

    val encodedClientAndSecret = convertToBase64("community-api-client", "community-api-client")
    webTestClient
      .post().uri("/auth/oauth/token?grant_type=client_credentials")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsKey("expires_in")
        assertThat(it["expires_in"]as Int).isLessThan(3600)
        assertThat(it).doesNotContainKey("refreshToken")
        assertThat(it["auth_source"]).isEqualTo("none")
      }
  }

  @Test
  fun `Client Credentials Login With username identifier for auth user`() {

    val encodedClientAndSecret = convertToBase64("omicadmin", "clientsecret")
    val token = getClientCredentialsTokenWithUsername(encodedClientAndSecret, "AUTH_USER")

    webTestClient
      .get().uri("/auth/api/user/me")
      .header("Authorization", "Bearer $token")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsAllEntriesOf(
          mapOf(
            "name" to "Auth Only",
          )
        )
      }
  }

  @Test
  fun `Password Credentials Login`() {

    val encodedClientAndSecret = convertToBase64("elite2apiclient", "clientsecret")
    webTestClient
      .post().uri("/auth/oauth/token?grant_type=password&username=ITAG_USER&password=password")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsKey("expires_in")
        assertThat(it["expires_in"]as Int).isLessThan(28800)
        assertThat(it["refresh_token"]).isNotNull
        assertThat(it["auth_source"]).isEqualTo("nomis")
        assertThat(it["sub"]).isEqualTo("ITAG_USER")
      }
  }

  @Test
  fun `Password Credentials Login for auth user`() {

    val encodedClientAndSecret = convertToBase64("elite2apiclient", "clientsecret")
    webTestClient
      .post().uri("/auth/oauth/token?grant_type=password&username=AUTH_USER&password=password123456")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsKey("expires_in")
        assertThat(it["expires_in"]as Int).isLessThan(28800)
        assertThat(it["refresh_token"]).isNotNull
        assertThat(it["auth_source"]).isEqualTo("auth")
        assertThat(it["sub"]).isEqualTo("AUTH_USER")
      }
  }

  @Test
  fun `Password Credentials Login for delius user`() {

    val encodedClientAndSecret = convertToBase64("elite2apiclient", "clientsecret")
    webTestClient
      .post().uri("/auth/oauth/token?grant_type=password&username=delius&password=password")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsKey("expires_in")
        assertThat(it["expires_in"]as Int).isLessThan(28800)
        assertThat(it["refresh_token"]).isNotNull
        assertThat(it["auth_source"]).isEqualTo("delius")
        assertThat(it["sub"]).isEqualTo("DELIUS")
      }
  }

  @Test
  fun `Refresh token can be obtained`() {
    val encodedClientAndSecret = convertToBase64("elite2apiclient", "clientsecret")
    val (accessToken, refreshToken) = getAccessAndRefreshTokens(encodedClientAndSecret, "ITAG_USER", "password")

    webTestClient
      .post().uri("/auth/oauth/token?grant_type=refresh_token&refresh_token=$refreshToken")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath(".['refresh_token','access_token']").value<JSONArray> {
        val newAccessToken = (it[0] as Map<*, *>)["access_token"].toString()
        val newRefreshToken = (it[0] as Map<*, *>)["refresh_token"].toString()
        assertThat(newAccessToken).isNotNull().isNotEqualTo(accessToken)
        assertThat(newRefreshToken).isNotNull().isNotEqualTo(refreshToken)
      }
  }

  @Test
  fun `Refresh token can be obtained for auth user`() {
    val encodedClientAndSecret = convertToBase64("elite2apiclient", "clientsecret")
    val (accessToken, refreshToken) = getAccessAndRefreshTokens(encodedClientAndSecret, "AUTH_USER", "password123456")

    webTestClient
      .post().uri("/auth/oauth/token?grant_type=refresh_token&refresh_token=$refreshToken")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath(".['refresh_token','access_token']").value<JSONArray> {
        val newAccessToken = (it[0] as Map<*, *>)["access_token"].toString()
        val newRefreshToken = (it[0] as Map<*, *>)["refresh_token"].toString()
        assertThat(newAccessToken).isNotNull().isNotEqualTo(accessToken)
        assertThat(newRefreshToken).isNotNull().isNotEqualTo(refreshToken)
      }
  }

  @Test
  fun `Refresh token can be obtained for delius user`() {
    val encodedClientAndSecret = convertToBase64("elite2apiclient", "clientsecret")
    val (accessToken, refreshToken) = getAccessAndRefreshTokens(encodedClientAndSecret, "delius", "password")

    webTestClient
      .post().uri("/auth/oauth/token?grant_type=refresh_token&refresh_token=$refreshToken")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath(".['refresh_token','access_token']").value<JSONArray> {
        val newAccessToken = (it[0] as Map<*, *>)["access_token"].toString()
        val newRefreshToken = (it[0] as Map<*, *>)["refresh_token"].toString()
        assertThat(newAccessToken).isNotNull().isNotEqualTo(accessToken)
        assertThat(newRefreshToken).isNotNull().isNotEqualTo(refreshToken)
      }
  }

  @Test
  fun `Password Credentials Login with Bad password credentials`() {

    val encodedClientAndSecret = convertToBase64("elite2apiclient", "clientsecret")
    webTestClient
      .post().uri("/auth/oauth/token?grant_type=password&username=ITAG_USER&password=password2")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "error" to "invalid_grant",
            "error_description" to "Bad credentials",
          )
        )
      }
  }

  @Test
  fun `Password Credentials Login with Bad client credentials`() {

    val encodedClientAndSecret = convertToBase64("elite2apiclient", "clientsecretBAD")
    webTestClient
      .post().uri("/auth/oauth/token?grant_type=password&username=ITAG_USER&password=password")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `Password Credentials Login with Wrong client Id`() {

    val encodedClientAndSecret = convertToBase64("elite2apiclientBAD", "clientsecret")
    webTestClient
      .post().uri("/auth/oauth/token?grant_type=password&username=ITAG_USER&password=password")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `Password Credentials Login with Expired Login`() {

    val encodedClientAndSecret = convertToBase64("elite2apiclient", "clientsecret")
    webTestClient
      .post().uri("/auth/oauth/token?grant_type=password&username=EXPIRED_USER&password=password123456")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "error" to "invalid_grant",
            "error_description" to "User credentials have expired",
          )
        )
      }
  }

  @Test
  fun `Password Credentials Login with Locked Login`() {

    val encodedClientAndSecret = convertToBase64("elite2apiclient", "clientsecret")
    webTestClient
      .post().uri("/auth/oauth/token?grant_type=password&username=LOCKED_USER&password=password123456")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "error" to "invalid_grant",
            "error_description" to "User account is locked",
          )
        )
      }
  }

  @Test
  fun `Password Credentials Login with Locked Login for Delius User`() {

    val encodedClientAndSecret = convertToBase64("elite2apiclient", "clientsecret")
    webTestClient
      .post().uri("/auth/oauth/token?grant_type=password&username=DELIUS_ERROR_LOCKED&password=password123456")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isUnauthorized
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            "error" to "unauthorized",
            "error_description" to "User is disabled",
          )
        )
      }
  }

  @Test
  fun `Password Credentials Login can get api data`() {

    val encodedClientAndSecret = convertToBase64("elite2apiclient", "clientsecret")
    val token = getPasswordCredentialsToken(encodedClientAndSecret)

    webTestClient
      .get().uri("/auth/api/user/me")
      .header("Authorization", "Bearer $token")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsAllEntriesOf(
          mapOf(
            "name" to "Itag User",
          )
        )
      }
  }

  @Test
  fun `Client Credentials Login can get api data`() {

    val encodedClientAndSecret = convertToBase64("omicadmin", "clientsecret")
    val token = getClientCredentialsToken(encodedClientAndSecret)

    webTestClient
      .get().uri("/auth/api/user/me")
      .header("Authorization", "Bearer $token")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").value<Map<String, Any>> {
        assertThat(it).containsAllEntriesOf(
          mapOf(
            "username" to "omicadmin",
          )
        )
      }
  }

  @Test
  fun `Kid header is returned`() {
    val encodedClientAndSecret = convertToBase64("elite2apiclient", "clientsecret")
    val result = webTestClient
      .post().uri("/auth/oauth/token?grant_type=password&username=ITAG_USER&password=password")
      .header("Authorization", "Basic $encodedClientAndSecret")
      .exchange()
      .expectStatus().isOk
      .expectBody<String>().returnResult()

    val token = JSONObject(result.responseBody).get("access_token")
    assertThat(JWT.decode(token as String?).getHeaderClaim("kid").asString()).isEqualTo("dps-client-key")
  }

  private fun convertToBase64(client: String, secret: String): String =
    Base64.getEncoder().encodeToString("$client:$secret".toByteArray())

  private fun getClientCredentialsTokenWithUsername(encodedClientAndSecret: String, username: String): String {
    val result =
      webTestClient
        .post().uri("/auth/oauth/token?grant_type=client_credentials&username=$username")
        .header("Authorization", "Basic $encodedClientAndSecret")
        .exchange()
        .expectStatus().isOk
        .expectBody<String>().returnResult()

    return JSONObject(result.responseBody).get("access_token") as String
  }

  private fun getClientCredentialsToken(encodedClientAndSecret: String): String {
    val result =
      webTestClient
        .post().uri("/auth/oauth/token?grant_type=client_credentials")
        .header("Authorization", "Basic $encodedClientAndSecret")
        .exchange()
        .expectStatus().isOk
        .expectBody<String>().returnResult()

    return JSONObject(result.responseBody).get("access_token") as String
  }

  private fun getPasswordCredentialsToken(encodedClientAndSecret: String): String {
    val result =
      webTestClient
        .post().uri("/auth/oauth/token?grant_type=password&username=ITAG_USER&password=password")
        .header("Authorization", "Basic $encodedClientAndSecret")
        .exchange()
        .expectStatus().isOk
        .expectBody<String>().returnResult()

    return JSONObject(result.responseBody).get("access_token") as String
  }

  private fun getAccessAndRefreshTokens(encodedClientAndSecret: String, username: String, password: String): Pair<String, String> {
    val result =
      webTestClient
        .post().uri("/auth/oauth/token?grant_type=password&username=$username&password=$password")
        .header("Authorization", "Basic $encodedClientAndSecret")
        .exchange()
        .expectStatus().isOk
        .expectBody<String>().returnResult()

    val accessToken = JSONObject(result.responseBody).get("access_token") as String
    val refreshToken = JSONObject(result.responseBody).get("refresh_token")as String
    return Pair(accessToken, refreshToken)
  }
}
