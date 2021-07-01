package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest
import uk.gov.justice.digital.hmpps.oauth2server.resource.RemoteClientExtension
import uk.gov.justice.digital.hmpps.oauth2server.resource.RemoteClientMockServer

@Suppress("DEPRECATION")
@ExtendWith(RemoteClientExtension::class)
class OauthWebClientIntTest(
  @Qualifier("authTestWebClient")
  val authTestWebClient: WebClient
) : IntegrationTest() {
  @Test
  fun `Client Credentials Login`() {
    val responseEntity = authTestWebClient.get()
      .retrieve()
      .toBodilessEntity()
      .block()
    assertThat(responseEntity?.statusCode).isEqualTo(HttpStatus.OK)
  }
}

@Configuration
class TestWebClientConfiguration(val webClientBuilder: WebClient.Builder, val environment: Environment) {
  @Lazy
  @Bean
  fun authTestWebClient(): WebClient {
    val port = environment.getProperty("local.server.port")
    val testRegistration = ClientRegistration.withRegistrationId("auth-test")
      .clientName("auth-test")
      .clientId("special-chars-test-client")
      .clientSecret("""'fI-P<%Fe9k18"#ESRcqggdsthuGwOu#:nK19E9ldq3(-hNDa&2g0,/3Zy84""")
      .tokenUri("http://localhost:$port/auth/oauth/token")
      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
      .build()

    val repository = InMemoryClientRegistrationRepository(testRegistration)
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
      repository, InMemoryOAuth2AuthorizedClientService(repository)
    )
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)

    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("auth-test")

    return webClientBuilder
      .apply(oauth2Client.oauth2Configuration())
      .baseUrl(RemoteClientMockServer.clientBaseUrl)
      .build()
  }
}
