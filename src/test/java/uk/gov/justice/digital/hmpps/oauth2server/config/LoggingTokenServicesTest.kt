@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.config

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.common.DefaultOAuth2RefreshToken
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.OAuth2Request
import org.springframework.security.oauth2.provider.TokenRequest
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.util.ReflectionTestUtils
import uk.gov.justice.digital.hmpps.oauth2server.security.ExternalIdAuthenticationHelper
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl

@ExtendWith(SpringExtension::class)
internal class LoggingTokenServicesTest {
  private val telemetryClient: TelemetryClient = mock()
  private val tokenStore: TokenStore = mock()
  private val externalIdAuthenticationHelper: ExternalIdAuthenticationHelper = mock()
  private var loggingTokenServices = LoggingTokenServices(telemetryClient)
  @BeforeEach
  fun setUp() {
    loggingTokenServices.setSupportRefreshToken(true)
    loggingTokenServices.setTokenStore(tokenStore)
    val tokenEnhancer = JWTTokenEnhancer()
    ReflectionTestUtils.setField(tokenEnhancer, "externalIdAuthenticationHelper", externalIdAuthenticationHelper)
    loggingTokenServices.setTokenEnhancer(tokenEnhancer)
  }

  @Test
  fun createAccessToken() {
    val userAuthentication = UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials")
    loggingTokenServices.createAccessToken(OAuth2Authentication(OAUTH_2_REQUEST, userAuthentication))
    verify(telemetryClient).trackEvent("CreateAccessToken", mapOf("username" to "authenticateduser", "clientId" to "client"), null)
  }

  @Test
  fun createAccessToken_ClientOnly() {
    whenever(externalIdAuthenticationHelper.getUserDetails(anyMap())).thenReturn(USER_DETAILS)
    loggingTokenServices.createAccessToken(OAuth2Authentication(OAUTH_2_REQUEST, null))
    verify(telemetryClient, never()).trackEvent(any(), anyMap(), isNull())
  }

  @Test
  fun createAccessToken_ClientOnlyProxyUser() {
    whenever(externalIdAuthenticationHelper.getUserDetails(anyMap())).thenReturn(UNCHECKED_USER_DETAILS)
    loggingTokenServices.createAccessToken(OAuth2Authentication(OAUTH_2_SCOPE_REQUEST, null))
    verify(telemetryClient, never()).trackEvent(any(), anyMap(), isNull())
  }

  @Test
  fun refreshAccessToken() {
    whenever(tokenStore.readRefreshToken(anyString())).thenReturn(DefaultOAuth2RefreshToken("newValue"))
    whenever(tokenStore.readAuthenticationForRefreshToken(any())).thenReturn(OAuth2Authentication(OAUTH_2_REQUEST, UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials")))
    loggingTokenServices.refreshAccessToken("tokenValue", TokenRequest(emptyMap(), "client", emptySet(), "refresh"))
    verify(telemetryClient).trackEvent("RefreshAccessToken", mapOf("username" to "authenticateduser", "clientId" to "client"), null)
  }

  companion object {
    private val OAUTH_2_REQUEST = OAuth2Request(emptyMap(), "client", emptySet(), true, emptySet(), emptySet(), "redirect", null, null)
    private val USER_DETAILS = UserDetailsImpl("authenticateduser", "name", emptySet(), null, null)
    private val OAUTH_2_SCOPE_REQUEST = OAuth2Request(emptyMap(), "community-api-client", listOf(GrantedAuthority { "ROLE_COMMUNITY" }), true, setOf("proxy-user"), emptySet(), "redirect", null, null)
    private val UNCHECKED_USER_DETAILS = UserDetailsImpl("notcheckeduser", null, emptySet(), "none", null)
  }
}
