@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.config

import com.microsoft.applicationinsights.TelemetryClient
import com.nimbusds.jwt.JWTParser
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.TokenRequest
import org.springframework.security.oauth2.provider.token.DefaultTokenServices
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.utils.IpAddressHelper

open class TrackingTokenServices(
  private val telemetryClient: TelemetryClient,
  private val restTemplate: RestTemplate,
  private val tokenVerificationClientCredentials: TokenVerificationClientCredentials,
  private val tokenVerificationEnabled: Boolean,
) : DefaultTokenServices() {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun createAccessToken(authentication: OAuth2Authentication): OAuth2AccessToken {
    val token = super.createAccessToken(authentication)
    val username = retrieveUsernameFromToken(token)
    val clientId = authentication.oAuth2Request.clientId

    val name = if (authentication.isClientOnly) "CreateSystemAccessToken" else "CreateAccessToken"
    telemetryClient.trackEvent(
      name,
      mapOf(
        "username" to username, "clientId" to clientId, "clientIpAddress" to IpAddressHelper.retrieveIpFromRequest()
      ),
      null
    )
    if (tokenVerificationClientCredentials.clientId != clientId) {
      val jwtId = sendAuthJwtIdToTokenVerification(authentication, token)
      log.info("Created access token for {} and client {} with jwt id of {}", username, clientId, jwtId)
    }
    return token
  }

  override fun refreshAccessToken(refreshTokenValue: String, tokenRequest: TokenRequest): OAuth2AccessToken {
    val token = super.refreshAccessToken(refreshTokenValue, tokenRequest)
    val username = retrieveUsernameFromToken(token)
    val clientId = tokenRequest.clientId
    if (tokenVerificationClientCredentials.clientId != clientId) {
      val jwtId = sendRefreshToTokenVerification(refreshTokenValue, token)
      log.info("Created refresh token for {} and client {} with jwt id of {}", username, clientId, jwtId)
    }
    telemetryClient.trackEvent(
      "RefreshAccessToken",
      mapOf(
        "username" to username, "clientId" to clientId, "clientIpAddress" to IpAddressHelper.retrieveIpFromRequest()
      ),
      null
    )
    return token
  }

  private fun sendRefreshToTokenVerification(refreshTokenValue: String, token: OAuth2AccessToken): String? {
    // refresh tokens have an ati field which links back to the original access token
    val accessTokenId = JWTParser.parse(refreshTokenValue).jwtClaimsSet.getStringClaim("ati")
    if (tokenVerificationEnabled) {
      // now send token to token verification service so can validate them
      restTemplate.postForLocation("/token/refresh?accessJwtId={accessJwtId}", token.value, accessTokenId)
    }
    return accessTokenId
  }

  private fun sendAuthJwtIdToTokenVerification(
    authentication: OAuth2Authentication,
    token: OAuth2AccessToken,
  ): String? {
    val jwtId = if (authentication.principal is UserDetailsImpl) {
      (authentication.principal as UserDetailsImpl).jwtId
    } else {
      // if we're using a password grant then there won't be any authentication, so just use the jti
      token.additionalInformation["jti"] as String?
    }
    if (tokenVerificationEnabled && !jwtId.isNullOrEmpty()) {
      // now send token to token verification service so can validate them
      restTemplate.postForLocation("/token?authJwtId={authJwtId}", token.value, jwtId)
    }
    return jwtId
  }

  private fun retrieveUsernameFromToken(token: OAuth2AccessToken): String {
    val username = token.additionalInformation[JWTTokenEnhancer.SUBJECT] as String?
    return if (username.isNullOrEmpty()) "none" else username
  }
}
