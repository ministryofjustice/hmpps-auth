@file:Suppress("DEPRECATION", "ClassName")

package uk.gov.justice.digital.hmpps.oauth2server.config

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.common.DefaultOAuth2RefreshToken
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.OAuth2Request
import org.springframework.security.oauth2.provider.TokenRequest
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.utils.JwtAuthHelper
import uk.gov.justice.digital.hmpps.oauth2server.utils.JwtAuthHelper.JwtParameters

internal class TrackingTokenServicesTest {
  private val telemetryClient: TelemetryClient = mock()
  private val tokenStore: TokenStore = mock()
  private val clientDetailsService: JdbcClientDetailsService = mock()
  private val restTemplate: RestTemplate = mock()
  private val tokenVerificationClientCredentials = TokenVerificationClientCredentials()
  private val tokenServices =
    TrackingTokenServices(telemetryClient, restTemplate, tokenVerificationClientCredentials, true)
  private val tokenServicesVerificationDisabled =
    TrackingTokenServices(telemetryClient, restTemplate, tokenVerificationClientCredentials, false)

  @BeforeEach
  fun setUp() {
    tokenVerificationClientCredentials.clientId = "token-verification-client-id"
    tokenServices.setSupportRefreshToken(true)
    tokenServices.setTokenStore(tokenStore)
    tokenServicesVerificationDisabled.setSupportRefreshToken(true)
    tokenServicesVerificationDisabled.setTokenStore(tokenStore)
    val tokenEnhancer = JWTTokenEnhancer()
    ReflectionTestUtils.setField(tokenEnhancer, "clientsDetailsService", clientDetailsService)
    tokenServices.setTokenEnhancer(tokenEnhancer)
    tokenServicesVerificationDisabled.setTokenEnhancer(tokenEnhancer)
    whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(BaseClientDetails())
  }

  @Nested
  inner class `create access token` {
    @Test
    fun createAccessToken() {
      val userAuthentication = UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials")
      tokenServices.createAccessToken(OAuth2Authentication(OAUTH_2_REQUEST, userAuthentication))
      verify(telemetryClient).trackEvent(
        "CreateAccessToken",
        mapOf("username" to "authenticateduser", "clientId" to "client"),
        null
      )
    }

    @Test
    fun `create access token calls token verification service`() {
      val userAuthentication = UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials")
      tokenServices.createAccessToken(OAuth2Authentication(OAUTH_2_REQUEST, userAuthentication))
      verify(restTemplate).postForLocation(
        eq("/token?authJwtId={authJwtId}"),
        check {
          assertThat(it).isInstanceOf(String::class.java).asString().hasSize(27)
        },
        eq("jwtId")
      )
    }

    @Test
    fun `create access token ignores token verification service if disabled`() {
      val userAuthentication = UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials")
      tokenServicesVerificationDisabled.createAccessToken(OAuth2Authentication(OAUTH_2_REQUEST, userAuthentication))
      verifyZeroInteractions(restTemplate)
    }

    @Test
    fun createAccessToken_ClientOnly() {
      tokenServices.createAccessToken(OAuth2Authentication(OAUTH_2_REQUEST, null))
      verify(telemetryClient, never()).trackEvent(any(), anyMap(), isNull())
    }

    @Test
    fun createAccessToken_ClientOnlyProxyUser() {
      tokenServices.createAccessToken(OAuth2Authentication(OAUTH_2_SCOPE_REQUEST, null))
      verify(telemetryClient, never()).trackEvent(any(), anyMap(), isNull())
    }
  }

  @Nested
  inner class `refresh access token` {
    private val refreshToken =
      JwtAuthHelper().createJwt(JwtParameters(additionalClaims = mapOf("ati" to "accessTokenId")))

    @Test
    fun refreshAccessToken() {
      whenever(tokenStore.readRefreshToken(anyString())).thenReturn(DefaultOAuth2RefreshToken("newValue"))
      whenever(tokenStore.readAuthenticationForRefreshToken(any())).thenReturn(
        OAuth2Authentication(
          OAUTH_2_REQUEST,
          UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials")
        )
      )
      tokenServices.refreshAccessToken(refreshToken, TokenRequest(emptyMap(), "client", emptySet(), "refresh"))
      verify(telemetryClient).trackEvent(
        "RefreshAccessToken",
        mapOf("username" to "authenticateduser", "clientId" to "client"),
        null
      )
    }

    @Test
    fun `refresh access token calls token verification service`() {
      whenever(tokenStore.readRefreshToken(anyString())).thenReturn(DefaultOAuth2RefreshToken("newValue"))
      whenever(tokenStore.readAuthenticationForRefreshToken(any())).thenReturn(
        OAuth2Authentication(
          OAUTH_2_REQUEST,
          UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials")
        )
      )
      tokenServices.refreshAccessToken(refreshToken, TokenRequest(emptyMap(), "client", emptySet(), "refresh"))
      verify(restTemplate).postForLocation(
        eq("/token/refresh?accessJwtId={accessJwtId}"),
        check {
          assertThat(it).isInstanceOf(String::class.java).asString().hasSize(27)
        },
        eq("accessTokenId")
      )
    }

    @Test
    fun `refresh access token ignores token verification service if disabled`() {
      whenever(tokenStore.readRefreshToken(anyString())).thenReturn(DefaultOAuth2RefreshToken("newValue"))
      whenever(tokenStore.readAuthenticationForRefreshToken(any())).thenReturn(
        OAuth2Authentication(
          OAUTH_2_REQUEST,
          UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials")
        )
      )
      tokenServicesVerificationDisabled.refreshAccessToken(
        refreshToken,
        TokenRequest(emptyMap(), "client", emptySet(), "refresh")
      )
      verifyZeroInteractions(restTemplate)
    }

    @Test
    fun `refresh access token ignores token verification service if client is token verification`() {
      val tokenVerificationAuthRequest = OAuth2Request(
        emptyMap(),
        "token-verification-client-id",
        emptySet(),
        true,
        emptySet(),
        emptySet(),
        "redirect",
        null,
        null
      )

      whenever(tokenStore.readRefreshToken(anyString())).thenReturn(DefaultOAuth2RefreshToken("newValue"))
      whenever(tokenStore.readAuthenticationForRefreshToken(any())).thenReturn(
        OAuth2Authentication(
          tokenVerificationAuthRequest,
          UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials")
        )
      )
      tokenServicesVerificationDisabled.refreshAccessToken(
        refreshToken,
        TokenRequest(emptyMap(), "token-verification-client-id", emptySet(), "refresh")
      )
      verifyZeroInteractions(restTemplate)
    }
  }

  companion object {
    private val OAUTH_2_REQUEST =
      OAuth2Request(emptyMap(), "client", emptySet(), true, emptySet(), emptySet(), "redirect", null, null)
    private val USER_DETAILS = UserDetailsImpl("authenticateduser", "name", emptySet(), "none", "userid", "jwtId")
    private val OAUTH_2_SCOPE_REQUEST = OAuth2Request(
      emptyMap(),
      "community-api-client",
      listOf(GrantedAuthority { "ROLE_COMMUNITY" }),
      true,
      setOf("proxy-user"),
      emptySet(),
      "redirect",
      null,
      null
    )
  }
}
