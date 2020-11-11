package uk.gov.justice.digital.hmpps.oauth2server.config

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties
import io.github.resilience4j.circuitbreaker.internal.InMemoryCircuitBreakerRegistry
import io.github.resilience4j.circuitbreaker.monitoring.health.CircuitBreakersHealthIndicator
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import org.springframework.boot.actuate.health.StatusAggregator
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder
import org.springframework.cloud.client.circuitbreaker.Customizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration


@Configuration
class Resilience4JConfiguration {
  @Bean
  fun circuitBreakerRegistry() : CircuitBreakerRegistry = InMemoryCircuitBreakerRegistry()

  @Bean
  fun globalCustomConfiguration(): Customizer<Resilience4JCircuitBreakerFactory> {

    val circuitBreakerConfig = CircuitBreakerConfig.custom()
//      .failureRateThreshold(50f)
//      .waitDurationInOpenState(Duration.ofSeconds(5))
//      .slidingWindowSize(10)
      .build()
    val timeLimiterConfig = TimeLimiterConfig.custom()
      .timeoutDuration(Duration.ofSeconds(20))
      .build()

    // the circuitBreakerConfig and timeLimiterConfig objects
    return Customizer<Resilience4JCircuitBreakerFactory> { factory ->
      factory.configureCircuitBreakerRegistry(circuitBreakerRegistry())
      factory.configureDefault { id ->
        Resilience4JConfigBuilder(id)
          .timeLimiterConfig(timeLimiterConfig)
          .circuitBreakerConfig(circuitBreakerConfig)
          .build()
      }
    }
  }

  @Bean
//  @ConditionalOnMissingBean(name = ["circuitBreakersHealthIndicator"])
//  @ConditionalOnProperty(prefix = "management.health.circuitbreakers", name = ["enabled"])
  fun circuitBreakersHealthIndicator(
    circuitBreakerRegistry: CircuitBreakerRegistry,
    circuitBreakerProperties: CircuitBreakerConfigurationProperties,
    statusAggregator: StatusAggregator): CircuitBreakersHealthIndicator {
    return CircuitBreakersHealthIndicator(circuitBreakerRegistry, circuitBreakerProperties, statusAggregator)
  }
}