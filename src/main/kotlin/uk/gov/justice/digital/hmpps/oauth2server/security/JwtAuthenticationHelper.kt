@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.security

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.apache.commons.codec.binary.Base64
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory
import org.springframework.stereotype.Component
import java.security.KeyPair
import java.time.Duration
import java.util.Date
import java.util.Optional
import java.util.UUID.randomUUID

@Component
class JwtAuthenticationHelper(
  @Value("\${jwt.signing.key.pair}") privateKeyPair: String?,
  @Value("\${jwt.keystore.password}") keystorePassword: String,
  @Value("\${jwt.keystore.alias:elite2api}") keystoreAlias: String?,
  properties: JwtCookieConfigurationProperties
) {
  private val keyPair: KeyPair
  private val expiryTime: Duration

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  init {
    val keyStoreKeyFactory = KeyStoreKeyFactory(
      ByteArrayResource(Base64.decodeBase64(privateKeyPair)),
      keystorePassword.toCharArray()
    )
    keyPair = keyStoreKeyFactory.getKeyPair(keystoreAlias)
    expiryTime = properties.expiryTime
  }

  fun createJwt(authentication: Authentication): String =
    if (authentication is OAuth2AuthenticationToken) createJwtWithIdFromOidcAuthentication(
      authentication,
      randomUUID().toString()
    ) else createJwtWithId(authentication, randomUUID().toString(), authentication is MfaPassedAuthenticationToken)

  fun createJwtWithIdFromOidcAuthentication(authentication: OAuth2AuthenticationToken, jwtId: String): String {
    val userDetails = authentication.principal as DefaultOidcUser
    val username = userDetails.name.toUpperCase()
    val authoritiesAsString = authentication.authorities?.joinToString(separator = ",") { it.authority } ?: ""
    return Jwts.builder()
      .setId(jwtId)
      .setSubject(username)
      .addClaims(
        mapOf(
          "authorities" to authoritiesAsString,
          "name" to userDetails.fullName,
          "auth_source" to AuthSource.azuread.source,
          "user_id" to userDetails.preferredUsername.toLowerCase()
        )
      )
      .setExpiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
      .signWith(SignatureAlgorithm.RS256, keyPair.private)
      .compact()
  }

  fun createJwtWithId(authentication: Authentication, jwtId: String, passedMfa: Boolean): String {
    val userDetails = authentication.principal as UserPersonDetails
    val username = userDetails.username
    log.debug("Creating jwt cookie for user {}", username)
    val authoritiesAsString = authentication.authorities?.joinToString(separator = ",") { it.authority } ?: ""

    return Jwts.builder()
      .setId(jwtId)
      .setSubject(username)
      .addClaims(
        mapOf<String, Any>(
          "authorities" to authoritiesAsString,
          "name" to userDetails.name,
          "auth_source" to userDetails.authSource,
          "user_id" to userDetails.userId,
          "passed_mfa" to passedMfa,
        )
      )
      .setExpiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
      .signWith(SignatureAlgorithm.RS256, keyPair.private)
      .compact()
  }

  /**
   * This method takes a user JWT post authentication and uses it to create a token that is then sent to the client
   *
   * @param jwt String
   * @return token for client
   */
  fun readAuthenticationFromJwt(jwt: String): Optional<UsernamePasswordAuthenticationToken> =
    readUserDetailsFromJwt(jwt).map { UsernamePasswordAuthenticationToken(it, null, it.authorities) }

  fun readUserDetailsFromJwt(jwt: String): Optional<UserDetailsImpl> = try {
    val body = Jwts.parser()
      .setSigningKey(keyPair.public)
      .parseClaimsJws(jwt)
      .body
    val username = body.subject
    val authoritiesString = body.get("authorities", String::class.java)
    val name = body.get("name", String::class.java) ?: username
    val userId = body.get("user_id", String::class.java) ?: username
    val authorities: Collection<GrantedAuthority> = authoritiesString.split(",")
      .filterNot { it.isEmpty() }
      .map { SimpleGrantedAuthority(it) }
    val authSource = body.get("auth_source", String::class.java) ?: AuthSource.none.source
    val passedMfa = body.get("passed_mfa", java.lang.Boolean::class.java) ?.booleanValue() ?: false

    log.debug("Set authentication for {} with jwt id of {}", username, body.id)
    Optional.of(UserDetailsImpl(username, name, authorities, authSource, userId, body.id, passedMfa))
  } catch (eje: ExpiredJwtException) {
    // cookie set to expire at same time as JWT so unlikely really get an expired one
    log.info("Expired JWT found for user {}", eje.claims.subject)
    Optional.empty()
  }
}
