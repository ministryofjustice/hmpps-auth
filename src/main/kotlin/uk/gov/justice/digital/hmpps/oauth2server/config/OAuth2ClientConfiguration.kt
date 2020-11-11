package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository

@Configuration
class OAuth2ClientConfiguration {
  @Bean
  fun clientRegistrationRepository(registrations: List<ClientRegistration>): ClientRegistrationRepository? {
    return InMemoryClientRegistrationRepository(registrations)
  }
}