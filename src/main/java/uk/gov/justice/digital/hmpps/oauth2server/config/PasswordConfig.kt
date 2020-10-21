package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class PasswordConfig {
  /**
   * This password encoder is used for client API authentication.
   * <br></br>
   * Authentication provider authentication for user's passwords goes through
   * [uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider] instead
   */
  @Bean
  fun passwordEncoder(): PasswordEncoder {
    val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder() as DelegatingPasswordEncoder
    // if there isn't an encoder set then default to bcrypt (existing records only)
    passwordEncoder.setDefaultPasswordEncoderForMatches(BCryptPasswordEncoder())
    return passwordEncoder
  }
}
