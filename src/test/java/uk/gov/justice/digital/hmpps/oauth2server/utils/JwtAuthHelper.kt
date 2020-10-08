@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.utils

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.apache.commons.codec.binary.Base64
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory
import org.springframework.stereotype.Component
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.time.Duration
import java.util.Date
import java.util.UUID

@Component
class JwtAuthHelper(
  @Value("\${jwt.signing.key.pair}") privateKeyPair: String? = null,
  @Value("\${jwt.keystore.password}") keystorePassword: String? = null,
  @Value("\${jwt.keystore.alias:elite2api}") keystoreAlias: String? = null
) {
  private val keyPair: KeyPair

  fun createJwt(parameters: JwtParameters): String {
    val claims = mutableMapOf<String, Any>("user_name" to parameters.username, "client_id" to "elite2apiclient")
    parameters.roles?.let { claims["authorities"] = parameters.roles }
    parameters.scope?.let { claims["scope"] = parameters.scope }
    parameters.additionalClaims?.let { claims.putAll(parameters.additionalClaims) }
    return Jwts.builder()
      .setId(UUID.randomUUID().toString())
      .setSubject(parameters.username)
      .addClaims(claims)
      .setExpiration(Date(System.currentTimeMillis() + parameters.expiryTime.toMillis()))
      .signWith(SignatureAlgorithm.RS256, keyPair.private)
      .compact()
  }

  data class JwtParameters(
    val username: String = "someuser",
    val scope: List<String>? = listOf(),
    val roles: List<String>? = listOf(),
    val expiryTime: Duration = Duration.ofHours(1),
    val additionalClaims: Map<String, Any>? = mapOf()
  )

  init {
    keyPair = if (privateKeyPair.isNullOrEmpty()) {
      val gen = KeyPairGenerator.getInstance("RSA")
      gen.initialize(2048)
      gen.generateKeyPair()
    } else {
      val keyStoreKeyFactory = KeyStoreKeyFactory(
        ByteArrayResource(Base64.decodeBase64(privateKeyPair)),
        keystorePassword!!.toCharArray()
      )
      keyStoreKeyFactory.getKeyPair(keystoreAlias)
    }
  }
}
