@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.security.jwt.JwtHelper
import org.springframework.security.jwt.crypto.sign.RsaSigner
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.common.util.JsonParserFactory
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.security.KeyPair
import java.security.interfaces.RSAPrivateKey

class JwtKeyIdHeaderAccessTokenConverter(private val keyId: String, keyPair: KeyPair) : JwtAccessTokenConverter() {
  private val jsonParser = JsonParserFactory.create()
  private val signer: RsaSigner

  init {
    super.setKeyPair(keyPair)
    this.signer = RsaSigner(keyPair.private as RSAPrivateKey)
  }

  override fun encode(accessToken: OAuth2AccessToken, authentication: OAuth2Authentication): String {
    accessToken.additionalInformation["iss"] = issuer()
    val content = try {
      jsonParser.formatMap(accessTokenConverter.convertAccessToken(accessToken, authentication))
    } catch (ex: Exception) {
      throw IllegalStateException("Cannot convert access token to JSON", ex)
    }
    return JwtHelper.encode(content, this.signer, mapOf("kid" to keyId)).encoded
  }

  private fun issuer() = "${ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()}/issuer"
}
