package uk.gov.justice.digital.hmpps.oauth2server.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm.RS256
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import org.springframework.test.util.ReflectionTestUtils
import java.security.KeyPair
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

class JwtAuthenticationHelperTest {
  private val helper: JwtAuthenticationHelper

  init {
    val properties = JwtCookieConfigurationProperties(name = "Bob", expiryTime = Duration.ofHours(1))
    helper = JwtAuthenticationHelper(PAIR, PASSWORD, ALIAS, properties)
  }

  @Test
  fun testReadAndWriteWithoutAuthorities() {
    val user = UserDetailsImpl("user", "name", emptyList(), "none", "user id", "jwtId")
    val token = UsernamePasswordAuthenticationToken(user, "pass")
    val jwt = helper.createJwt(token)
    val auth = helper.readAuthenticationFromJwt(jwt)
    assertThat(auth).isPresent
    assertThat(auth.get().principal).extracting("username", "name", "authSource", "userId")
      .containsExactly("user", "name", "none", "user id")
    assertThat(auth.get().authorities).isEmpty()
  }

  @Test
  fun `test read and write oidc token`() {
    val token = createOAuth2AuthenticationToken()
    val jwt = helper.createJwt(token)
    val auth = helper.readAuthenticationFromJwt(jwt)
    assertThat(auth).isPresent
    assertThat(auth.get().principal).extracting("username", "name", "authSource", "userId")
      .containsExactly(
        "6C2X9VSOM3FGI9QMEYZGUT4HDYME3DW1566TP8YC1VE",
        "Joe Bloggs",
        "azuread",
        "joe.bloggs@justice.gov.uk"
      )
    assertThat(auth.get().authorities).isEmpty()
  }

  @Test
  fun testReadExpiredCookie() {
    assertThat(helper.readAuthenticationFromJwt(EXPIRED_COOKIE)).isEmpty
  }

  @Test
  fun testReadCookieWithoutNameField() {
    val expiryTime = ReflectionTestUtils.getField(helper, "expiryTime") as Duration
    val keyPair = ReflectionTestUtils.getField(helper, "keyPair") as KeyPair
    val cookie = Jwts.builder()
      .setId(UUID.randomUUID().toString())
      .setSubject("BOB")
      .addClaims(mapOf("authorities" to ""))
      .setExpiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
      .signWith(RS256, keyPair.private)
      .compact()
    val token = helper.readAuthenticationFromJwt(cookie)
    assertThat(token).get().extracting("principal.name").isEqualTo("BOB")
  }

  @Test
  fun testReadCookieWithoutAuthSourceField() {
    val expiryTime = ReflectionTestUtils.getField(helper, "expiryTime") as Duration
    val keyPair = ReflectionTestUtils.getField(helper, "keyPair") as KeyPair
    val cookie = Jwts.builder()
      .setId(UUID.randomUUID().toString())
      .setSubject("BOB")
      .addClaims(mapOf("authorities" to ""))
      .setExpiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
      .signWith(RS256, keyPair.private)
      .compact()
    val token = helper.readAuthenticationFromJwt(cookie)
    assertThat(token).get().extracting("principal.authSource").isEqualTo("none")
  }

  @Test
  fun testReadCookieWithoutUserIdField() {
    val expiryTime = ReflectionTestUtils.getField(helper, "expiryTime") as Duration
    val keyPair = ReflectionTestUtils.getField(helper, "keyPair") as KeyPair
    val cookie = Jwts.builder()
      .setId(UUID.randomUUID().toString())
      .setSubject("BOB")
      .addClaims(mapOf("authorities" to ""))
      .setExpiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
      .signWith(RS256, keyPair.private)
      .compact()
    val token = helper.readAuthenticationFromJwt(cookie)
    assertThat(token).get().extracting("principal.userId").isEqualTo("BOB")
  }

  @Test
  fun `test read cookie`() {
    val expiryTime = ReflectionTestUtils.getField(helper, "expiryTime") as Duration
    val keyPair = ReflectionTestUtils.getField(helper, "keyPair") as KeyPair
    val jwtId = UUID.randomUUID().toString()
    val cookie = Jwts.builder()
      .setId(jwtId)
      .setSubject("BOB")
      .addClaims(mapOf("authorities" to "", "user_id" to "some user", "passed_mfa" to true))
      .setExpiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
      .signWith(RS256, keyPair.private)
      .compact()
    val token = helper.readAuthenticationFromJwt(cookie).orElseThrow()
    assertThat(token).extracting("principal.userId").isEqualTo("some user")
    assertThat(token).extracting("principal.jwtId").isEqualTo(jwtId)
    assertThat(token).extracting("principal.passedMfa").isEqualTo(true)
  }

  companion object {
    private const val PASSWORD = "s3cre3tK3y"
    private const val ALIAS = "elite2api"
    private const val PAIR =
      "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"
    private const val EXPIRED_COOKIE =
      "eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiI1NTgwMzIzOS1kOWVhLTRhYTAtOGQxMC0yMDUxOWQ5M2VhMGEiLCJzdWIiOiJST19VU0VSX01VTFRJIiwiYXV0aG9yaXRpZXMiOiJST0xFX0xJQ0VOQ0VfUk8iLCJleHAiOjE1NDcyMDg5NDl9.D5ro1Wy3a8jQpoWmCn-YrEda4qaNQm2LmpsyQbTl76HmOox0avabTT87zdr1WmsheydrQugyn6PswlReNIlJaEa7b8XsEiwuS1SxwlZLXRdkm9qmMUU5A2avVQqDaN-ryAyMDAtk3Ihd2vzNI249HuGBQxcFt7UpwgCQpZhh6qkG-JnyXbF-HUe5vSLXBtXV1eO6KOYw_pAtoLnYy5O4x3C8fmVfSb6Vb2GluaMbREa9NtwAIYb1o7OOpK9UmqvoSMYk-QqDXZ89hL0sCZ5y0skpQlWvP4ZXvF22iDmiKVLyWfjDde7wfo2ZTTx10nzxwhxnaTxTv97m62YG4NqU4w"
  }

  private fun createOAuth2AuthenticationToken(): OAuth2AuthenticationToken {
    val oidcToken = OidcIdToken(
      "tokenValue",
      Instant.now(),
      Instant.now().plusSeconds(1000),
      // id claims
      mapOf(
        "sub" to "6C2x9vsoM3Fgi9QmeYZGUT4hdYme3Dw1566tp8yc1vE",
        "given_name" to "Joe",
        "family_name" to "Bloggs",
        "oid" to "d6165ad0-aed3-4146-9ef7-222876b57549",
        "preferred_username" to "Joe.Bloggs@justice.gov.uk",
        "name" to "Joe Bloggs",
      )
    )
    return OAuth2AuthenticationToken(DefaultOidcUser(listOf(OidcUserAuthority(oidcToken)), oidcToken), null, "jwtId")
  }
}
