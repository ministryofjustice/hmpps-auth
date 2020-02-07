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
import java.time.Duration
import java.util.*

@Component
class JwtAuthHelper(@Value("\${jwt.signing.key.pair}") privateKeyPair: String?,
                    @Value("\${jwt.keystore.password}") keystorePassword: String,
                    @Value("\${jwt.keystore.alias:elite2api}") keystoreAlias: String?) {
  private val keyPair: KeyPair
  fun createJwt(parameters: JwtParameters): String {
    val claims = HashMap<String, Any>()
    claims["user_name"] = parameters.username
    claims["client_id"] = "elite2apiclient"
    if (!parameters.roles.isNullOrEmpty()) claims["authorities"] = parameters.roles
    if (!parameters.scope.isNullOrEmpty()) claims["scope"] = parameters.scope
    return Jwts.builder()
        .setId(UUID.randomUUID().toString())
        .setSubject(parameters.username)
        .addClaims(claims)
        .setExpiration(Date(System.currentTimeMillis() + parameters.expiryTime.toMillis()))
        .signWith(SignatureAlgorithm.RS256, keyPair.private)
        .compact()
  }

  data class JwtParameters(val username: String,
                           val scope: List<String>? = listOf(),
                           val roles: List<String>? = listOf(),
                           val expiryTime: Duration)

  init {
    val keyStoreKeyFactory = KeyStoreKeyFactory(ByteArrayResource(Base64.decodeBase64(privateKeyPair)),
        keystorePassword.toCharArray())
    keyPair = keyStoreKeyFactory.getKeyPair(keystoreAlias)
  }
}
