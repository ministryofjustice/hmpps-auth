package uk.gov.justice.digital.hmpps.oauth2server.config

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties
import io.github.resilience4j.circuitbreaker.internal.InMemoryCircuitBreakerRegistry
import io.github.resilience4j.circuitbreaker.monitoring.health.CircuitBreakersHealthIndicator
import org.springframework.boot.actuate.health.StatusAggregator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Resilience4JConfiguration {
  @Bean
  fun circuitBreakerRegistry(): CircuitBreakerRegistry = InMemoryCircuitBreakerRegistry()

  @Bean
  fun circuitBreakersHealthIndicator(
    circuitBreakerRegistry: CircuitBreakerRegistry,
    circuitBreakerProperties: CircuitBreakerConfigurationProperties,
    statusAggregator: StatusAggregator
  ): CircuitBreakersHealthIndicator {
    return CircuitBreakersHealthIndicator(circuitBreakerRegistry, circuitBreakerProperties, statusAggregator)
  }
}
