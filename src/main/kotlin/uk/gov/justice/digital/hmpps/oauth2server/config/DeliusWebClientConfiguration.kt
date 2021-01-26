package uk.gov.justice.digital.hmpps.oauth2server.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import org.hibernate.validator.constraints.URL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.Connection
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Configuration
class DeliusWebClientConfiguration {

  @Bean
  fun getDeliusClientRegistration(
    @Value("\${delius.client.client-id}") deliusClientId: String,
    @Value("\${delius.client.client-secret}") deliusClientSecret: String,
    @Value("\${delius.client.access-token-uri}") deliusTokenUri: String
  ): ClientRegistration = ClientRegistration.withRegistrationId("delius")
    .clientName("delius")
    .clientId(deliusClientId)
    .clientSecret(deliusClientSecret)
    .tokenUri(deliusTokenUri)
    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
    .build()

  @Bean("deliusWebClient")
  fun deliusWebClient(
    builder: WebClient.Builder,
    @Value("\${delius.endpoint.url}") deliusEndpointUrl: @URL String,
    @Value("\${delius.endpoint.timeout:5s}") apiTimeout: Duration,
    authorizedClientManager: OAuth2AuthorizedClientManager
  ): WebClient {
    val oauth2 = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2.setDefaultClientRegistrationId("delius")

    return builder
      .baseUrl("$deliusEndpointUrl/secure")
      .apply(oauth2.oauth2Configuration())
      .clientConnector(getClientConnectorWithTimeouts(apiTimeout, apiTimeout, deliusEndpointUrl))
      .build()
  }

  @Bean("deliusHealthWebClient")
  fun deliusHealthWebClient(
    builder: WebClient.Builder,
    @Value("\${delius.endpoint.url}") deliusEndpointUrl: @URL String,
    @Value("\${delius.health.timeout:1s}") healthTimeout: Duration
  ): WebClient = builder
    .baseUrl(deliusEndpointUrl)
    .clientConnector(getClientConnectorWithTimeouts(healthTimeout, healthTimeout, deliusEndpointUrl))
    .build()

  private fun getClientConnectorWithTimeouts(
    connectTimeout: Duration,
    readTimeout: Duration,
    url: @URL String
  ): ClientHttpConnector {
    val httpClient = HttpClient.create()
      .warmupWithHealthPing(url)
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout.toMillis().toInt())
      .doOnConnected { connection: Connection ->
        connection
          .addHandlerLast(ReadTimeoutHandler(readTimeout.toSeconds().toInt()))
      }
    return ReactorClientHttpConnector(httpClient)
  }

  private fun HttpClient.warmupWithHealthPing(baseUrl: String): HttpClient {
    log.info("Warming up web client for {}", baseUrl)
    warmup().block()
    log.info("Warming up web client for {} halfway through, now calling health ping", baseUrl)
    try {
      baseUrl("$baseUrl/health/ping").get().response().block(Duration.ofSeconds(30))
    } catch (e: RuntimeException) {
      log.error("Caught exception during warm up, carrying on regardless", e)
    }
    log.info("Warming up web client completed for {}", baseUrl)
    return this
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

@Suppress("ConfigurationProperties", "ConfigurationProperties")
@ConstructorBinding
@ConfigurationProperties("delius.roles")
open class DeliusRoleMappings(val mappings: Map<String, List<String>>)
