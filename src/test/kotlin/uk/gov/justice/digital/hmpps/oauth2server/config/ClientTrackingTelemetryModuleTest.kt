package uk.gov.justice.digital.hmpps.oauth2server.config

import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext
import com.microsoft.applicationinsights.web.internal.ThreadContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.MapEntry.entry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.oauth2server.utils.JwtAuthHelper
import uk.gov.justice.digital.hmpps.oauth2server.utils.JwtAuthHelper.JwtParameters
import java.time.Duration

@ExtendWith(SpringExtension::class)
@Import(JwtAuthHelper::class, ClientTrackingTelemetryModule::class)
@ContextConfiguration(initializers = [ConfigFileApplicationContextInitializer::class])
@ActiveProfiles("test")
class ClientTrackingTelemetryModuleTest {
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private lateinit var clientTrackingTelemetryModule: ClientTrackingTelemetryModule

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private lateinit var jwtAuthHelper: JwtAuthHelper
  private val res = MockHttpServletResponse()
  private val req = MockHttpServletRequest()

  @BeforeEach
  fun setup() {
    ThreadContext.setRequestTelemetryContext(RequestTelemetryContext(1L))
  }

  @AfterEach
  fun tearDown() {
    ThreadContext.remove()
  }

  @Test
  fun shouldAddClientIdAndUserNameToInsightTelemetry() {
    val token = createJwt("bob", 1L)
    req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    clientTrackingTelemetryModule.onBeginRequest(req, res)
    val insightTelemetry = ThreadContext.getRequestTelemetryContext().httpRequestTelemetry.properties
    assertThat(insightTelemetry).contains(entry("username", "bob"), entry("clientId", "elite2apiclient"))
  }

  @Test
  fun shouldAddClientIdAndUserNameToInsightTelemetryEvenIfTokenExpired() {
    val token = createJwt("Fred", -1L)
    req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    clientTrackingTelemetryModule.onBeginRequest(req, res)
    val insightTelemetry = ThreadContext.getRequestTelemetryContext().httpRequestTelemetry.properties
    assertThat(insightTelemetry).contains(entry("username", "Fred"), entry("clientId", "elite2apiclient"))
  }

  private fun createJwt(user: String, duration: Long) =
    jwtAuthHelper.createJwt(
      JwtParameters(
        username = user,
        scope = listOf("read", "write"),
        expiryTime = Duration.ofDays(duration)
      )
    )

  @Test
  fun shouldAddClientIpToInsightTelemetry() {
    val SOME_IP_ADDRESS = "12.13.14.15"
    req.remoteAddr = SOME_IP_ADDRESS
    clientTrackingTelemetryModule.onBeginRequest(req, res)
    val insightTelemetry = ThreadContext.getRequestTelemetryContext().httpRequestTelemetry.properties
    assertThat(insightTelemetry).contains(entry("clientIpAddress", SOME_IP_ADDRESS))
  }
}
