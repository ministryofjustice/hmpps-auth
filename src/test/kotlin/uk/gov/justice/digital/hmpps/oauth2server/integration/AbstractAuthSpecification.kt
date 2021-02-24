package uk.gov.justice.digital.hmpps.oauth2server.integration

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.JWTParser
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.client.utils.URLEncodedUtils
import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.adapter.junit.jupiter.FluentTest
import org.fluentlenium.core.FluentPage
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.support.FindBy
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.oauth2server.resource.AzureOIDCExtension
import uk.gov.justice.digital.hmpps.oauth2server.resource.RemoteClientMockServer
import uk.gov.justice.digital.hmpps.oauth2server.resource.TokenVerificationExtension
import java.nio.charset.Charset
import java.util.Base64

@ExtendWith(TokenVerificationExtension::class, AzureOIDCExtension::class)
open class AbstractAuthSpecification : FluentTest() {
  private val webTestClient = WebTestClient.bindToServer().baseUrl(baseUrl).build()

  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  @Page
  internal lateinit var loginPage: LoginPage

  @AfterEach
  fun cleanup() {
    try {
      executeScript("console.log('Test finished')")
      val logEntries = driver.manage().logs().get(LogType.BROWSER).all
      println("START WebDriver ${LogType.BROWSER} logs")
      logEntries.forEach { println(it) }
      println("END WebDriver ${LogType.BROWSER} logs")
    } catch (error: Exception) {
      error.printStackTrace()
    }
  }

  fun clientAccess(doWithinAuth: () -> Unit = {}, clientId: String = "elite2apiclient"): WebTestClient.BodyContentSpec {
    val state = RandomStringUtils.random(6, true, true)
    goTo("/oauth/authorize?client_id=$clientId&redirect_uri=${RemoteClientMockServer.clientBaseUrl}&response_type=code&state=$state")

    doWithinAuth()

    assertThat(driver.currentUrl).startsWith("${RemoteClientMockServer.clientBaseUrl}?code").contains("state=$state")

    val params = URLEncodedUtils.parse(driver.currentUrl.replace("${RemoteClientMockServer.clientBaseUrl}?", ""), Charset.forName("UTF-8"))
    val code = params.find { it.name == "code" }!!.value

    return getAccessToken(code, clientId)
  }

  fun getAccessToken(authCode: String, clientId: String): WebTestClient.BodyContentSpec {
    val auth = Base64.getEncoder().encodeToString("$clientId:clientsecret".toByteArray())
    return webTestClient
      .post().uri("/oauth/token?grant_type=authorization_code&code=$authCode&redirect_uri=${RemoteClientMockServer.clientBaseUrl}")
      .headers { it.set(HttpHeaders.AUTHORIZATION, "Basic $auth") }
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
      .expectBody()
  }

  fun getRefreshToken(refreshToken: String): WebTestClient.BodyContentSpec =
    webTestClient
      .post().uri("/oauth/token?grant_type=refresh_token&refresh_token=$refreshToken")
      .headers { it.set(HttpHeaders.AUTHORIZATION, "Basic ZWxpdGUyYXBpY2xpZW50OmNsaWVudHNlY3JldA==") }
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
      .expectBody()

  fun clientSignIn(
    username: String,
    password: String = "password123456",
    clientId: String = "elite2apiclient",
  ) = clientAccess({ loginPage.isAtPage().submitLogin(username, password) }, clientId)
}

@Suppress("UNCHECKED_CAST")
open class AuthPage<T>(
  protected val title: String,
  protected val heading: String,
  protected val headingStartsWith: Boolean = false,
) : FluentPage() {
  @FindBy(css = "#main-content h1")
  protected lateinit var headingText: FluentWebElement

  @FindBy(css = "#error-detail")
  private lateinit var errorDetail: FluentWebElement

  @FindBy(css = "#logout")
  private lateinit var logOut: FluentWebElement

  @FindBy(css = "#principal-name")
  private lateinit var principalName: FluentWebElement

  internal fun logOut() {
    logOut.click()
  }

  override fun isAt() {
    super.isAt()

    assertThat(window().title()).isEqualTo(title)
    if (headingStartsWith) {
      assertThat(headingText.text()).startsWith(heading)
    } else {
      assertThat(headingText.text()).isEqualTo(heading)
    }
  }

  internal fun isAtPage(): T {
    isAt()
    return this as T
  }

  private fun isAtError() {
    super.isAt()

    assertThat(window().title()).isEqualTo("Error: $title")
    assertThat(headingText.text()).isEqualTo(heading)
  }

  internal fun checkError(error: String): T {
    isAtError()
    assertThat(errorDetail.text()).isEqualTo(error)
    return this as T
  }

  internal fun checkErrorContains(error: String): T {
    isAtError()
    assertThat(errorDetail.text()).contains(error)
    return this as T
  }

  internal fun parseJwt(): JWTClaimsSet {
    val token = driver.manage().getCookieNamed("jwtSession").value
    return JWTParser.parse(token).jwtClaimsSet
  }

  internal fun assertNameDisplayedCorrectly(name: String) {
    assertThat(principalName.text()).isEqualTo(name)
  }

  internal fun getCurrentName(): String = principalName.text()
}
