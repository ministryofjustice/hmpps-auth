@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.config

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.provider.client.ClientDetailsUserDetailsService
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService
import uk.gov.justice.digital.hmpps.oauth2server.utils.IpAddressHelper

class LoggingDaoAuthenticationProvider(
  private val telemetryClient: TelemetryClient,
  jdbcClientDetailsService: JdbcClientDetailsService,
  passwordEncoder: PasswordEncoder,
) : DaoAuthenticationProvider() {

  init {
    val clientDetailsUserDetailsService = ClientDetailsUserDetailsService(jdbcClientDetailsService)
    clientDetailsUserDetailsService.setPasswordEncoder(passwordEncoder)
    this.passwordEncoder = passwordEncoder
    this.userDetailsService = clientDetailsUserDetailsService
  }

  override fun authenticate(authentication: Authentication?): Authentication = try {
    super.authenticate(authentication)
  } catch (e: BadCredentialsException) {
    authentication?.let {
      telemetryClient.trackEvent(
        "CreateAccessTokenFailure",
        mapOf("clientId" to authentication.name, "clientIpAddress" to IpAddressHelper.retrieveIpFromRequest()),
        null
      )
    }
    throw e
  }
}
