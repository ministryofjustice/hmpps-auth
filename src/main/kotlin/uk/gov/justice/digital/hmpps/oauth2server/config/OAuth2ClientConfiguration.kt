package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.*
import org.springframework.security.oauth2.client.registration.*
import org.springframework.security.oauth2.client.web.server.AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository

@Configuration
class OAuth2ClientConfiguration {
//  @Bean
//  fun clientRegistrationRepository(registrations: List<ClientRegistration>): ClientRegistrationRepository? {
//    return InMemoryClientRegistrationRepository(registrations)
//  }
//
//  @Bean
//  fun reactiveClientRegistrationRepository(registrations: List<ClientRegistration>): ReactiveClientRegistrationRepository {
//    return InMemoryReactiveClientRegistrationRepository(registrations)
//  }
//
//  @Bean
//  fun blach(clientRegistrationRepository: ReactiveClientRegistrationRepository, authorizedClientService: ReactiveOAuth2AuthorizedClientService): AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager {
//    return AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService)
//  }
//
//  @Bean
//  fun test(authorizedClientService: ReactiveOAuth2AuthorizedClientService): ServerOAuth2AuthorizedClientRepository {
//    val repository = AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository(authorizedClientService)
//    repository.setAnonymousAuthorizedClientRepository()
//    return
//  }
//
//  @Bean
//  fun arg( clientRegistrationRepository: ReactiveClientRegistrationRepository): InMemoryReactiveOAuth2AuthorizedClientService {
//    return InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository)
//  }

  @Bean
  fun authorizedClientManager(clientRegistrationRepository: ClientRegistrationRepository?,
                              oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?): OAuth2AuthorizedClientManager? {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService)
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }
}