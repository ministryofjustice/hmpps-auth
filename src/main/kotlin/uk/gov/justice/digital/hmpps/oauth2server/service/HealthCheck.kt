package uk.gov.justice.digital.hmpps.oauth2server.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.boot.actuate.health.Status
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

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

abstract class WebClientHealthCheck(private val webClient: WebClient) : ReactiveHealthIndicator {
  override fun health(): Mono<Health> = webClient.get().uri("/health/ping")
    .retrieve()
    .toEntity(String::class.java)
    .map { responseEntity -> Health.Builder().up().withDetail("HttpStatus", responseEntity.statusCode).build() }
    .onErrorResume { ex -> Mono.just(Health.Builder().down(ex).build()) }
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
class DeliusApiHealth(@Qualifier("deliusHealthWebClient") private val webClient: WebClient) :
  WebClientHealthCheck(webClient) {
  override fun health(): Mono<Health> = super.health().map { health ->
    when (health.status) {
      Status.DOWN ->
        Health.up().withDetails(health.details).build()
      else -> health
    }
  }
}
