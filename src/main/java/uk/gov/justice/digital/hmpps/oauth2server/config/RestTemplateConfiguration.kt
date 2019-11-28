package uk.gov.justice.digital.hmpps.oauth2server.config

import org.hibernate.validator.constraints.URL
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.client.RootUriTemplateHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Suppress("DEPRECATION")
@Configuration
open class RestTemplateConfiguration(private val apiDetails: ClientCredentialsResourceDetails,
                                     @param:Value("\${delius.endpoint.url}") private val deliusEndpointUrl: @URL String,
                                     @param:Value("\${delius.health.timeout:1s}") private val healthTimeout: Duration) {

  @Bean(name = ["deliusApiRestTemplate"])
  open fun deliusRestTemplate(): OAuth2RestTemplate {

    val deliusApiRestTemplate = OAuth2RestTemplate(apiDetails)
    RootUriTemplateHandler.addTo(deliusApiRestTemplate, "${deliusEndpointUrl}/secure")

    return deliusApiRestTemplate
  }

  @Bean(name = ["deliusApiHealthRestTemplate"])
  open fun deliusHealthRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate =
      getHealthRestTemplate(restTemplateBuilder, deliusEndpointUrl)

  private fun getHealthRestTemplate(restTemplateBuilder: RestTemplateBuilder, uri: String): RestTemplate =
      restTemplateBuilder
          .rootUri(uri)
          .setConnectTimeout(healthTimeout)
          .setReadTimeout(healthTimeout)
          .build()
}
