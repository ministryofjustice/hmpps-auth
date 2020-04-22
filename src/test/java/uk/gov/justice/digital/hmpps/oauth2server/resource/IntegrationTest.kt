@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.oauth2server.config.DeliusClientCredentials
import uk.gov.justice.digital.hmpps.oauth2server.config.TokenVerificationClientCredentials
import uk.gov.justice.digital.hmpps.oauth2server.utils.JwtAuthHelper
import uk.gov.justice.digital.hmpps.oauth2server.utils.JwtAuthHelper.JwtParameters
import java.time.Duration


@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(JwtAuthHelper::class)
@ExtendWith(TokenVerificationExtension::class)
abstract class IntegrationTest {
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  private lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  private lateinit var deliusApiRestTemplate: OAuth2RestTemplate

  @Autowired
  private lateinit var tokenVerificationApiRestTemplate: OAuth2RestTemplate

  @LocalServerPort
  private var localServerPort: Int = 0

  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  @BeforeEach
  internal fun setupPort() {
    // need to override port as random port only assigned on server startup
    (deliusApiRestTemplate.resource as DeliusClientCredentials).accessTokenUri = "http://localhost:${localServerPort}/auth/oauth/token"
    (tokenVerificationApiRestTemplate.resource as TokenVerificationClientCredentials).accessTokenUri = "http://localhost:${localServerPort}/auth/oauth/token"
  }

  internal fun setAuthorisation(user: String, roles: List<String> = listOf()): (org.springframework.http.HttpHeaders) -> Unit {
    val token = createJwt(user, roles)
    return { it.set(HttpHeaders.AUTHORIZATION, "Bearer $token") }
  }

  internal fun setBasicAuthorisation(token: String): (HttpHeaders) -> Unit =
      { it.set(HttpHeaders.AUTHORIZATION, "Basic $token") }

  private fun createJwt(user: String, roles: List<String> = listOf()) =
      jwtAuthHelper.createJwt(
          JwtParameters(username = user,
              scope = listOf("read", "write"),
              expiryTime = Duration.ofHours(1L),
              roles = roles))

  internal fun String.readFile(): String = this@IntegrationTest::class.java.getResource(this).readText()
}
