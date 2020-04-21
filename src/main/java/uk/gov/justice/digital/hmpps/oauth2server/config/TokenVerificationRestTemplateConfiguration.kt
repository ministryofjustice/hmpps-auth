@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.config

import org.hibernate.validator.constraints.URL
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
class TokenVerificationRestTemplateConfiguration(private val apiDetails: TokenVerificationClientCredentials,
                                                 @Value("\${tokenverification.endpoint.url}") private val tokenVerificationEndpointUrl: @URL String,
                                                 @Value("\${tokenverification.health.timeout:1s}") private val healthTimeout: Duration,
                                                 @Value("\${tokenverification.endpoint.timeout:5s}") private val apiTimeout: Duration) {

  @Bean(name = ["tokenVerificationApiRestTemplate"])
  fun tokenVerificationRestTemplate(restTemplateBuilder: RestTemplateBuilder): OAuth2RestTemplate =
      restTemplateBuilder
          .rootUri(tokenVerificationEndpointUrl)
          .setConnectTimeout(apiTimeout)
          .setReadTimeout(apiTimeout)
          .configure(OAuth2RestTemplate(apiDetails))

  @Bean(name = ["tokenVerificationApiHealthRestTemplate"])
  fun tokenVerificationHealthRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate =
      getHealthRestTemplate(restTemplateBuilder, tokenVerificationEndpointUrl)

  private fun getHealthRestTemplate(restTemplateBuilder: RestTemplateBuilder, uri: String): RestTemplate =
      restTemplateBuilder
          .rootUri(uri)
          .setConnectTimeout(healthTimeout)
          .setReadTimeout(healthTimeout)
          .build()

}

@Suppress("ConfigurationProperties")
@ConfigurationProperties("tokenverification.client")
open class TokenVerificationClientCredentials : ClientCredentialsResourceDetails()
