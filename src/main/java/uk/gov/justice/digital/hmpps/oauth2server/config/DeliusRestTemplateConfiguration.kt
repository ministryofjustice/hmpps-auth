@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.config

import org.hibernate.validator.constraints.URL
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
class DeliusRestTemplateConfiguration(
  private val apiDetails: DeliusClientCredentials,
  @Value("\${delius.endpoint.url}") private val deliusEndpointUrl: @URL String,
  @Value("\${delius.health.timeout:1s}") private val healthTimeout: Duration,
  @Value("\${delius.endpoint.timeout:5s}") private val apiTimeout: Duration
) {

  @Bean(name = ["deliusApiRestTemplate"])
  fun deliusRestTemplate(restTemplateBuilder: RestTemplateBuilder): OAuth2RestTemplate =
    restTemplateBuilder
      .rootUri("$deliusEndpointUrl/secure")
      .setConnectTimeout(apiTimeout)
      .setReadTimeout(apiTimeout)
      .configure(OAuth2RestTemplate(apiDetails))

  @Bean(name = ["deliusApiHealthRestTemplate"])
  fun deliusHealthRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate =
    getHealthRestTemplate(restTemplateBuilder, deliusEndpointUrl)

  private fun getHealthRestTemplate(restTemplateBuilder: RestTemplateBuilder, uri: String): RestTemplate =
    restTemplateBuilder
      .rootUri(uri)
      .setConnectTimeout(healthTimeout)
      .setReadTimeout(healthTimeout)
      .build()
}

@Suppress("ConfigurationProperties")
@ConfigurationProperties("delius.client")
open class DeliusClientCredentials : ClientCredentialsResourceDetails()

@Suppress("ConfigurationProperties", "ConfigurationProperties")
@ConstructorBinding
@ConfigurationProperties("delius.roles")
open class DeliusRoleMappings(val mappings: Map<String, List<String>>)
