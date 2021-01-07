package uk.gov.justice.digital.hmpps.oauth2server.config

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Application insights now controlled by the spring-boot-starter dependency.  However when the key is not specified
 * we don't get a telemetry bean and application won't start.  Therefore need this backup configuration.
 */
@Configuration
class ApplicationInsightsConfiguration {
  @Bean
  @ConditionalOnExpression("T(org.apache.commons.lang3.StringUtils).isNotBlank('\${applicationinsights.connection.string:}')")
  fun insightsPresent(): AppInsightsConfigurationPresent = AppInsightsConfigurationPresent()

  @Bean
  @ConditionalOnMissingBean(AppInsightsConfigurationPresent::class)
  fun telemetryClient(): TelemetryClient = TelemetryClient()

  class AppInsightsConfigurationPresent
}
