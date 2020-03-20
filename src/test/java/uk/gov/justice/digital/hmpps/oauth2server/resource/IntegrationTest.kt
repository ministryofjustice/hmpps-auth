package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.oauth2server.utils.JwtAuthHelper
import uk.gov.justice.digital.hmpps.oauth2server.utils.JwtAuthHelper.JwtParameters
import java.time.Duration


@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@Import(JwtAuthHelper::class)
abstract class IntegrationTest {
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  private lateinit var jwtAuthHelper: JwtAuthHelper

  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  internal fun setAuthorisation(user: String, roles: List<String> = listOf()): (org.springframework.http.HttpHeaders) -> Unit {
    val token = createJwt(user, roles)
    return { it.set(org.springframework.http.HttpHeaders.AUTHORIZATION, "Bearer $token") }
  }

  private fun createJwt(user: String, roles: List<String> = listOf()) =
      jwtAuthHelper.createJwt(
          JwtParameters(username = user,
              scope = listOf("read", "write"),
              expiryTime = Duration.ofHours(1L),
              roles = roles))
}
