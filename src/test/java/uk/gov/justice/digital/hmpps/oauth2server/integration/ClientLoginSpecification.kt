@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.integration

import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.nimbusds.jwt.JWTParser
import net.minidev.json.JSONArray
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.client.utils.URLEncodedUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec
import uk.gov.justice.digital.hmpps.oauth2server.resource.TokenVerificationExtension.Companion.tokenVerificationApi
import java.nio.charset.Charset

class ClientLoginSpecification : AbstractAuthSpecification() {
  private val clientBaseUrl = "http://localhost:8081/login"
  private val webTestClient = WebTestClient.bindToServer().baseUrl(baseUrl).build()

  @Test
  fun `I can sign in from another client`() {
    clientSignIn("ITAG_USER", "password")
        .jsonPath(".user_name").isEqualTo("ITAG_USER")
        .jsonPath(".user_id").isEqualTo("1")
        .jsonPath(".sub").isEqualTo("ITAG_USER")
        .jsonPath(".auth_source").isEqualTo("nomis")
  }

  @Test
  fun `I can sign in from another client and send token to verification service`() {
    val jwt = goTo(loginPage).loginAs("ITAG_USER", "password").parseJwt()

    clientAccess()
        .jsonPath(".sub").isEqualTo("ITAG_USER")
        .jsonPath(".access_token").value<JSONArray> {
          tokenVerificationApi.verify(postRequestedFor(urlPathMatching("/token/${jwt.jwtid}"))
              .withRequestBody(equalTo(it[0].toString())))
        }
  }

  @Test
  fun `I can sign in from another client as delius only user`() {
    clientSignIn("DELIUS_USER", "password")
        .jsonPath(".user_name").isEqualTo("DELIUS_USER")
        .jsonPath(".user_id").isEqualTo("2500077027")
        .jsonPath(".sub").isEqualTo("DELIUS_USER")
        .jsonPath(".auth_source").isEqualTo("delius")
  }

  @Test
  fun `I can sign in from another client as auth only user`() {
    clientSignIn("AUTH_USER")
        .jsonPath(".user_name").isEqualTo("AUTH_USER")
        .jsonPath(".user_id").isEqualTo("608955ae-52ed-44cc-884c-011597a77949")
        .jsonPath(".sub").isEqualTo("AUTH_USER")
        .jsonPath(".auth_source").isEqualTo("auth")
  }

  @Test
  fun `I can redeem the access token for a refresh token`() {
    clientSignIn("AUTH_USER")
        .jsonPath(".refresh_token").value<JSONArray> {
          getRefreshToken(it[0].toString())
              .jsonPath(".user_name").isEqualTo("AUTH_USER")
              .jsonPath(".user_id").isEqualTo("608955ae-52ed-44cc-884c-011597a77949")
              .jsonPath(".sub").isEqualTo("AUTH_USER")
              .jsonPath(".auth_source").isEqualTo("auth")
        }
  }

  @Test
  fun `I can redeem the refresh token for an access token and send token to verification service`() {
    goTo(loginPage).loginAs("AUTH_USER")

    clientAccess()
        .jsonPath(".['refresh_token','access_token']").value<JSONArray> {
          val accessJwtid = JWTParser.parse((it[0] as Map<*, *>)["access_token"].toString()).jwtClaimsSet.jwtid

          getRefreshToken((it[0] as Map<*, *>)["refresh_token"].toString())
              .jsonPath(".sub").isEqualTo("AUTH_USER")
              .jsonPath(".access_token").value<JSONArray> { accessToken ->
                tokenVerificationApi.verify(postRequestedFor(urlPathMatching("/token/refresh/${accessJwtid}"))
                    .withRequestBody(equalTo(accessToken[0].toString())))
              }
        }
  }

  private fun clientSignIn(username: String, password: String = "password123456"): BodyContentSpec {
    return clientAccess { loginPage.isAtPage().submitLogin(username, password) }
  }

  private fun clientAccess(doWithinAuth: () -> Unit = {}): BodyContentSpec {
    val state = RandomStringUtils.random(6, true, true)
    goTo("/oauth/authorize?client_id=elite2apiclient&redirect_uri=$clientBaseUrl&response_type=code&state=$state")

    doWithinAuth()

    assertThat(driver.currentUrl).startsWith("$clientBaseUrl?code").contains("state=$state")

    val params = URLEncodedUtils.parse(driver.currentUrl.replace("$clientBaseUrl?", ""), Charset.forName("UTF-8"))
    val code = params.find { it.name == "code" }!!.value

    return getAccessToken(code)
  }

  private fun getAccessToken(authCode: String): BodyContentSpec =
      webTestClient
          .post().uri("/oauth/token?grant_type=authorization_code&code=$authCode&redirect_uri=$clientBaseUrl")
          .headers { it.set(HttpHeaders.AUTHORIZATION, "Basic ZWxpdGUyYXBpY2xpZW50OmNsaWVudHNlY3JldA==") }
          .exchange()
          .expectStatus().isOk
          .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
          .expectBody()

  private fun getRefreshToken(refreshToken: String): BodyContentSpec =
      webTestClient
          .post().uri("/oauth/token?grant_type=refresh_token&refresh_token=$refreshToken")
          .headers { it.set(HttpHeaders.AUTHORIZATION, "Basic ZWxpdGUyYXBpY2xpZW50OmNsaWVudHNlY3JldA==") }
          .exchange()
          .expectStatus().isOk
          .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
          .expectBody()
}