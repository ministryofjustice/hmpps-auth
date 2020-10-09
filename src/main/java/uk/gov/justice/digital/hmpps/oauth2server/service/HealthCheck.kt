package uk.gov.justice.digital.hmpps.oauth2server.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.health.Status
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

abstract class HealthCheck(private val restTemplate: RestTemplate) : HealthIndicator {
  override fun health(): Health {
    return try {
      val responseEntity = restTemplate.getForEntity("/health/ping", String::class.java)
      Health.up().withDetail("HttpStatus", responseEntity.statusCode).build()
    } catch (e: RestClientException) {
      Health.down(e).build()
    }
  }
}

@Component
class TokenVerificationApiHealth(
  @Qualifier("tokenVerificationApiHealthRestTemplate") restTemplate: RestTemplate,
  @Value("\${tokenverification.enabled:false}") private val tokenVerificationEnabled: Boolean,
) :
  HealthCheck(restTemplate) {
  override fun health(): Health {
    return if (tokenVerificationEnabled) super.health() else {
      Health.up().withDetail("VerificationDisabled", "token verification is disabled").build()
    }
  }
}

@Component
class DeliusApiHealth(@Qualifier("deliusApiHealthRestTemplate") private val restTemplate: RestTemplate) :
  HealthCheck(restTemplate) {
  override fun health(): Health {
    val health = super.health()
    if (health.status != Status.DOWN) return health

    // can still run a degraded service with delius down, so mark as healthy with error message
    return Health.up().withDetails(health.details).build()
  }
}
