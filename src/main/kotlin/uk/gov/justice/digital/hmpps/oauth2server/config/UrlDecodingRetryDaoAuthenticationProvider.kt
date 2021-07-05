@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.config

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.provider.ClientDetailsService
import org.springframework.security.oauth2.provider.client.ClientDetailsUserDetailsService
import uk.gov.justice.digital.hmpps.oauth2server.utils.IpAddressHelper
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class UrlDecodingRetryDaoAuthenticationProvider(
  private val telemetryClient: TelemetryClient,
  clientDetailsService: ClientDetailsService,
  passwordEncoder: PasswordEncoder,
) : DaoAuthenticationProvider() {

  init {
    val clientDetailsUserDetailsService = ClientDetailsUserDetailsService(clientDetailsService)
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

  override fun additionalAuthenticationChecks(
    userDetails: UserDetails?,
    authentication: UsernamePasswordAuthenticationToken?
  ) {
    try {
      super.additionalAuthenticationChecks(userDetails, authentication)
    } catch (e: BadCredentialsException) {
      authentication?.credentials?.let {
        val decodedAuthentication = UsernamePasswordAuthenticationToken(
          authentication.principal,
          URLDecoder.decode(authentication.credentials.toString(), StandardCharsets.UTF_8.toString()),
          authentication.authorities,
        )
        decodedAuthentication.details = authentication.details
        super.additionalAuthenticationChecks(userDetails, decodedAuthentication)
      }
    }
  }
}
