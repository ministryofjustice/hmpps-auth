package uk.gov.justice.digital.hmpps.oauth2server.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import org.hibernate.validator.constraints.URL
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.Connection
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.TcpClient
import java.time.Duration


@Configuration
class DeliusWebClientConfiguration {

  @Bean
  @ConditionalOnProperty(name = ["delius.enabled"], havingValue = "true")
  fun getDeliusClientRegistration(@Value("\${delius.client.client-id}") deliusClientId: String,
                                  @Value("\${delius.client.client-secret}") deliusClientSecret: String,
                                  @Value("\${delius.client.access-token-uri}") deliusTokenUri: String) : ClientRegistration {
    return ClientRegistration.withRegistrationId("delius")
      .clientName("delius")
      .clientId(deliusClientId)
      .clientSecret(deliusClientSecret)
      .tokenUri(deliusTokenUri)
      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
      .build()
  }

  @Bean("deliusWebClient")
  fun deliusWebClientWithAuth(builder: WebClient.Builder,
                              @Value("\${delius.endpoint.url}") deliusEndpointUrl: @URL String,
                              @Value("\${delius.health.timeout:1s}") healthTimeout: Duration,
                              @Value("\${delius.endpoint.timeout:5s}") apiTimeout: Duration,
                              clientRegistrations: ClientRegistrationRepository,
                              authorizedClients: OAuth2AuthorizedClientRepository): WebClient {
    val oauth2 = ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients)
    oauth2.setDefaultClientRegistrationId("delius")

    return builder
      .baseUrl("$deliusEndpointUrl/secure")
      .apply(oauth2.oauth2Configuration())
      .clientConnector(getClientConnectorWithTimeouts(apiTimeout, apiTimeout))
      .build()
  }

  private fun getClientConnectorWithTimeouts(connectTimeout: Duration, readTimeout: Duration): ClientHttpConnector {
    val tcpClient = TcpClient.create()
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, java.lang.Long.valueOf(connectTimeout.toMillis()).toInt())
      .doOnConnected { connection: Connection ->
        connection
          .addHandlerLast(ReadTimeoutHandler(java.lang.Long.valueOf(readTimeout.toSeconds()).toInt()))
      }
    return ReactorClientHttpConnector(HttpClient.from(tcpClient))
  }
}