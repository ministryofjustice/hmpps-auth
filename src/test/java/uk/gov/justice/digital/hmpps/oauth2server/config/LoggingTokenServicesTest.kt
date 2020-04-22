@file:Suppress("DEPRECATION", "ClassName")

package uk.gov.justice.digital.hmpps.oauth2server.config

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.check
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.isNull
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.common.DefaultOAuth2RefreshToken
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.OAuth2Request
import org.springframework.security.oauth2.provider.TokenRequest
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.security.ExternalIdAuthenticationHelper
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl

@ExtendWith(MockitoExtension::class)
internal class LoggingTokenServicesTest {
  private val telemetryClient: TelemetryClient = mock()
  private val tokenStore: TokenStore = mock()
  private val externalIdAuthenticationHelper: ExternalIdAuthenticationHelper = mock()
  private val restTemplate: RestTemplate = mock()
  private var tokenServices = LoggingTokenServices(telemetryClient, restTemplate, true)
  private var tokenServicesVerificationDisabled = LoggingTokenServices(telemetryClient, restTemplate, false)

  @BeforeEach
  fun setUp() {
    tokenServices.setSupportRefreshToken(true)
    tokenServices.setTokenStore(tokenStore)
    tokenServicesVerificationDisabled.setSupportRefreshToken(true)
    tokenServicesVerificationDisabled.setTokenStore(tokenStore)
    val tokenEnhancer = JWTTokenEnhancer()
    ReflectionTestUtils.setField(tokenEnhancer, "externalIdAuthenticationHelper", externalIdAuthenticationHelper)
    tokenServices.setTokenEnhancer(tokenEnhancer)
    tokenServicesVerificationDisabled.setTokenEnhancer(tokenEnhancer)
  }

  @Nested
  inner class `create access token`() {
    @Test
    fun createAccessToken() {
      val userAuthentication = UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials")
      tokenServices.createAccessToken(OAuth2Authentication(OAUTH_2_REQUEST, userAuthentication))
      verify(telemetryClient).trackEvent("CreateAccessToken", mapOf("username" to "authenticateduser", "clientId" to "client"), null)
    }

    @Test
    fun `create access token calls token verification service`() {
      val userAuthentication = UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials")
      tokenServices.createAccessToken(OAuth2Authentication(OAUTH_2_REQUEST, userAuthentication))
      verify(restTemplate).postForLocation(eq("/token/{authJwtId}"), check {
        assertThat(it).isInstanceOf(String::class.java).asString().hasSize(36)
      }, eq("jwtId"))
    }

    @Test
    fun `create access token ignores token verification service if disabled`() {
      val userAuthentication = UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials")
      tokenServicesVerificationDisabled.createAccessToken(OAuth2Authentication(OAUTH_2_REQUEST, userAuthentication))
      verifyZeroInteractions(restTemplate)
    }

    @Test
    fun createAccessToken_ClientOnly() {
      whenever(externalIdAuthenticationHelper.getUserDetails(anyMap())).thenReturn(USER_DETAILS)
      tokenServices.createAccessToken(OAuth2Authentication(OAUTH_2_REQUEST, null))
      verify(telemetryClient, never()).trackEvent(any(), anyMap(), isNull())
    }

    @Test
    fun createAccessToken_ClientOnlyProxyUser() {
      whenever(externalIdAuthenticationHelper.getUserDetails(anyMap())).thenReturn(UNCHECKED_USER_DETAILS)
      tokenServices.createAccessToken(OAuth2Authentication(OAUTH_2_SCOPE_REQUEST, null))
      verify(telemetryClient, never()).trackEvent(any(), anyMap(), isNull())
    }
  }

  @Nested
  inner class `refresh access token`() {
    @Test
    fun refreshAccessToken() {
      whenever(tokenStore.readRefreshToken(anyString())).thenReturn(DefaultOAuth2RefreshToken("newValue"))
      whenever(tokenStore.readAuthenticationForRefreshToken(any())).thenReturn(OAuth2Authentication(OAUTH_2_REQUEST, UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials")))
      tokenServices.refreshAccessToken("tokenValue", TokenRequest(emptyMap(), "client", emptySet(), "refresh"))
      verify(telemetryClient).trackEvent("RefreshAccessToken", mapOf("username" to "authenticateduser", "clientId" to "client"), null)
    }

    @Test
    fun `refresh access token ignores token verification service if disabled`() {
      whenever(tokenStore.readRefreshToken(anyString())).thenReturn(DefaultOAuth2RefreshToken("newValue"))
      whenever(tokenStore.readAuthenticationForRefreshToken(any())).thenReturn(OAuth2Authentication(OAUTH_2_REQUEST, UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials")))
      tokenServicesVerificationDisabled.refreshAccessToken("tokenValue", TokenRequest(emptyMap(), "client", emptySet(), "refresh"))
      verifyZeroInteractions(restTemplate)
    }
  }

  companion object {
    private val OAUTH_2_REQUEST = OAuth2Request(emptyMap(), "client", emptySet(), true, emptySet(), emptySet(), "redirect", null, null)
    private val USER_DETAILS = UserDetailsImpl("authenticateduser", "name", emptySet(), "none", "userid", "jwtId")
    private val OAUTH_2_SCOPE_REQUEST = OAuth2Request(emptyMap(), "community-api-client", listOf(GrantedAuthority { "ROLE_COMMUNITY" }), true, setOf("proxy-user"), emptySet(), "redirect", null, null)
    private val UNCHECKED_USER_DETAILS = UserDetailsImpl("notcheckeduser", "name", emptySet(), "none", "userid", "jwtId")
  }
}
