package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import java.util.Optional

@Configuration
class OAuth2ClientConfiguration {

  /**
   * Constructs a ClientRegistrationRepository with registrations from beans (Delius) and Spring configuration (Azure OIDC)
   */
  @Bean
  fun clientRegistrationRepository(properties: Optional<OAuth2ClientProperties>, registrationBeans: Optional<List<ClientRegistration>>): ClientRegistrationRepository? {
    val registrations = ArrayList<ClientRegistration>()

    if (properties.isPresent) {
      registrations.addAll(OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties.get()).values)
    }

    if (registrationBeans.isPresent) {
      registrations.addAll(registrationBeans.get())
    }

    return InMemoryClientRegistrationRepository(registrations)
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?
  ): OAuth2AuthorizedClientManager? {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager =
      AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService)
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }
}
