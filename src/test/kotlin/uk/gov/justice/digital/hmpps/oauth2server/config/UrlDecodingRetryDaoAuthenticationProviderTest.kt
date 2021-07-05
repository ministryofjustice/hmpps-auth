@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.config

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.ClientDetailsService
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

internal class UrlDecodingRetryDaoAuthenticationProviderTest {
  private val telemetryClient: TelemetryClient = mock()
  private val passwordEncoder: PasswordEncoder = mock()
  private val clientDetailsService: ClientDetailsService = mock()
  private val clientDetails: ClientDetails = mock()
  private val daoAuthenticationProvider: UrlDecodingRetryDaoAuthenticationProvider =
    UrlDecodingRetryDaoAuthenticationProvider(telemetryClient, clientDetailsService, passwordEncoder)

  @Test
  fun `successful authenticate`() {
    whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(clientDetails)
    whenever(clientDetails.clientSecret).thenReturn("some secret")
    whenever(passwordEncoder.matches(any(), any())).thenReturn(true)
    val token = UsernamePasswordAuthenticationToken("princ", "cred", listOf(SimpleGrantedAuthority("BOB")))
    token.details = "some details"
    val authenticatedToken = daoAuthenticationProvider.authenticate(token)
    assertThat(authenticatedToken.isAuthenticated).isTrue()
    assertThat(authenticatedToken.details).isEqualTo(token.details)

    verify(passwordEncoder).matches(eq("cred"), any())
  }

  @Test
  fun `urldecoded authenticate`() {
    whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(clientDetails)
    whenever(clientDetails.clientSecret).thenReturn("some secret")
    whenever(passwordEncoder.matches(any(), any())).thenReturn(false).thenReturn(true)
    val token = UsernamePasswordAuthenticationToken("princ", "a%20cred", listOf(SimpleGrantedAuthority("BOB")))
    token.details = "some details"
    val authenticatedToken = daoAuthenticationProvider.authenticate(token)
    assertThat(authenticatedToken.isAuthenticated).isTrue()
    assertThat(authenticatedToken.details).isEqualTo(token.details)

    verify(passwordEncoder).matches(eq("a%20cred"), any())
    // second try with client secret decoded
    verify(passwordEncoder).matches(eq("a cred"), any())
  }

  @Test
  fun `failure throws bad credentials exception`() {
    RequestContextHolder.setRequestAttributes(ServletRequestAttributes(MockHttpServletRequest(), null))

    whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(clientDetails)
    whenever(clientDetails.clientSecret).thenReturn("some secret")
    whenever(passwordEncoder.matches(any(), any())).thenReturn(false).thenReturn(false)
    val token = UsernamePasswordAuthenticationToken("princ", "a%20cred", listOf(SimpleGrantedAuthority("BOB")))
    token.details = "some details"

    assertThatThrownBy { daoAuthenticationProvider.authenticate(token) }.isInstanceOf(BadCredentialsException::class.java)

    verify(passwordEncoder, times(2)).matches(any(), any())
  }
}
