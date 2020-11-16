@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.config

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.OAuth2Request
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService
import org.springframework.test.util.ReflectionTestUtils
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import java.util.UUID

internal class JWTTokenEnhancerTest {
  private val authentication: OAuth2Authentication = mock()
  private val clientDetailsService: JdbcClientDetailsService = mock()
  private val jwtTokenEnhancer = JWTTokenEnhancer()

  @BeforeEach
  internal fun setUp() {
    ReflectionTestUtils.setField(jwtTokenEnhancer, "clientsDetailsService", clientDetailsService)
  }

  @Test
  fun testEnhance_HasUserToken() {
    val token: OAuth2AccessToken = DefaultOAuth2AccessToken("value")
    whenever(authentication.isClientOnly).thenReturn(false)
    val uuid = UUID.randomUUID()
    val user = createSampleUser(id = uuid, username = "user", source = AuthSource.auth)
    whenever(authentication.userAuthentication).thenReturn(UsernamePasswordAuthenticationToken(user, "pass"))
    whenever(authentication.oAuth2Request).thenReturn(
      OAuth2Request(
        mapOf(),
        "client_id",
        listOf(),
        true,
        setOf(),
        setOf(),
        "redirect",
        setOf(),
        mapOf()
      )
    )
    whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(createBaseClientDetails("+user_name,-name"))
    jwtTokenEnhancer.enhance(token, authentication)
    assertThat(token.additionalInformation).containsOnly(
      entry("sub", "user"),
      entry("user_name", "user"),
      entry("auth_source", "auth"),
      entry("user_id", uuid.toString())
    )
  }

  @Test
  fun `testEnhance modify jwt fields`() {
    val token: OAuth2AccessToken = DefaultOAuth2AccessToken("value")
    whenever(authentication.isClientOnly).thenReturn(false)
    val uuid = UUID.randomUUID()
    val user = createSampleUser(id = uuid, username = "user", firstName = "Joe", lastName = "bloggs", source = AuthSource.auth)
    whenever(authentication.userAuthentication).thenReturn(UsernamePasswordAuthenticationToken(user, "pass"))
    whenever(authentication.oAuth2Request).thenReturn(
      OAuth2Request(
        mapOf(),
        "client_id",
        listOf(),
        true,
        setOf(),
        setOf(),
        "redirect",
        setOf(),
        mapOf()
      )
    )
    whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(createBaseClientDetails("-auth_source"))
    jwtTokenEnhancer.enhance(token, authentication)
    assertThat(token.additionalInformation).containsOnly(
      entry("sub", "user"),
      entry("name", "Joe bloggs"),
      entry("user_name", "user"),
      entry("user_id", uuid.toString())
    )
  }

  @Test
  fun `testEnhance blank jwt fields`() {
    val token: OAuth2AccessToken = DefaultOAuth2AccessToken("value")
    whenever(authentication.isClientOnly).thenReturn(false)
    val uuid = UUID.randomUUID()
    val user = createSampleUser(id = uuid, username = "user", firstName = "Joe", lastName = "bloggs", source = AuthSource.auth)
    whenever(authentication.userAuthentication).thenReturn(UsernamePasswordAuthenticationToken(user, "pass"))
    whenever(authentication.oAuth2Request).thenReturn(
      OAuth2Request(
        mapOf(),
        "client_id",
        listOf(),
        true,
        setOf(),
        setOf(),
        "redirect",
        setOf(),
        mapOf()
      )
    )
    whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(BaseClientDetails())
    jwtTokenEnhancer.enhance(token, authentication)
    assertThat(token.additionalInformation).containsOnly(
      entry("sub", "user"),
      entry("name", "Joe bloggs"),
      entry("auth_source", "auth"),
      entry("user_name", "user"),
      entry("user_id", uuid.toString())
    )
  }

  private fun createBaseClientDetails(jwtFields: String = "-name", legacyUsername: String? = null): ClientDetails {
    val details = BaseClientDetails()
    details.addAdditionalInformation("jwtFields", jwtFields)
    if (legacyUsername != null) details.addAdditionalInformation("legacyUsernameField", legacyUsername)
    return details
  }

  @Test
  fun testEnhance_MissingAuthSource() {
    val token: OAuth2AccessToken = DefaultOAuth2AccessToken("value")
    whenever(authentication.isClientOnly).thenReturn(false)
    whenever(authentication.userAuthentication).thenReturn(
      UsernamePasswordAuthenticationToken(
        UserDetailsImpl(
          "user",
          "name",
          emptyList(),
          "none",
          "userID",
          "jwtId"
        ),
        "pass"
      )
    )
    whenever(authentication.oAuth2Request).thenReturn(
      OAuth2Request(
        mapOf(),
        "client_id",
        listOf(),
        true,
        setOf(),
        setOf(),
        "redirect",
        setOf(),
        mapOf()
      )
    )
    whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(createBaseClientDetails("-name,+user_name"))
    jwtTokenEnhancer.enhance(token, authentication)
    assertThat(token.additionalInformation).containsOnly(
      entry("sub", "user"),
      entry("user_name", "user"),
      entry("auth_source", "none"),
      entry("user_id", "userID")
    )
  }

  @Test
  fun `enhance client credentials no username`() {
    val token: OAuth2AccessToken = DefaultOAuth2AccessToken("value")
    whenever(authentication.isClientOnly).thenReturn(true)
    whenever(authentication.oAuth2Request).thenReturn(
      OAuth2Request(
        mapOf(),
        "client_id",
        listOf(),
        true,
        setOf(),
        setOf(),
        "redirect",
        setOf(),
        mapOf()
      )
    )
    whenever(authentication.name).thenReturn("principal")
    whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(BaseClientDetails())
    jwtTokenEnhancer.enhance(token, authentication)
    assertThat(token.additionalInformation).containsOnly(
      entry("sub", "principal"),
      entry("auth_source", "none")
    )
  }

  @Test
  fun `enhance client credentials with username`() {
    val token: OAuth2AccessToken = DefaultOAuth2AccessToken("value")
    whenever(authentication.isClientOnly).thenReturn(true)
    whenever(authentication.oAuth2Request).thenReturn(
      OAuth2Request(
        mapOf("username" to "joe"),
        "client_id",
        listOf(),
        true,
        setOf(),
        setOf(),
        "redirect",
        setOf(),
        mapOf()
      )
    )
    whenever(authentication.name).thenReturn("principal")
    whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(BaseClientDetails())
    jwtTokenEnhancer.enhance(token, authentication)
    assertThat(token.additionalInformation).containsOnly(
      entry("sub", "JOE"),
      entry("user_name", "JOE"),
      entry("auth_source", "none")
    )
  }

  @Test
  fun `enhance client credentials with auth source`() {
    val token: OAuth2AccessToken = DefaultOAuth2AccessToken("value")
    whenever(authentication.isClientOnly).thenReturn(true)
    whenever(authentication.oAuth2Request).thenReturn(
      OAuth2Request(
        mapOf(
          "username" to "jOe",
          "auth_source" to "deLius"
        ),
        "client_id", listOf(), true, setOf(), setOf(), "redirect", setOf(), mapOf()
      )
    )
    whenever(authentication.name).thenReturn("principal")
    whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(BaseClientDetails())

    jwtTokenEnhancer.enhance(token, authentication)
    assertThat(token.additionalInformation).containsOnly(
      entry("sub", "JOE"),
      entry("user_name", "JOE"),
      entry("auth_source", "delius")
    )
  }

  @Test
  fun `enhance client credentials with auth source invalid`() {
    val token: OAuth2AccessToken = DefaultOAuth2AccessToken("value")
    whenever(authentication.isClientOnly).thenReturn(true)
    whenever(authentication.oAuth2Request).thenReturn(
      OAuth2Request(
        mapOf(
          "username" to "jOe",
          "auth_source" to "billybob"
        ),
        "client_id", listOf(), true, setOf(), setOf(), "redirect", setOf(), mapOf()
      )
    )
    whenever(authentication.name).thenReturn("principal")
    whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(BaseClientDetails())
    jwtTokenEnhancer.enhance(token, authentication)
    assertThat(token.additionalInformation).containsOnly(
      entry("sub", "JOE"),
      entry("user_name", "JOE"),
      entry("auth_source", "none")
    )
  }

  @Test
  fun `enhance client credentials with legacy username`() {
    val token: OAuth2AccessToken = DefaultOAuth2AccessToken("value")
    whenever(authentication.isClientOnly).thenReturn(true)
    whenever(authentication.oAuth2Request).thenReturn(
      OAuth2Request(
        mapOf(
          "username" to "moic",
          "auth_source" to "none"
        ),
        "client_id", listOf(), true, setOf(), setOf(), "redirect", setOf(), mapOf()
      )
    )
    whenever(authentication.name).thenReturn("principal")
    whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(createBaseClientDetails("-name", "API_USER"))
    jwtTokenEnhancer.enhance(token, authentication)
    assertThat(token.additionalInformation).containsOnly(
      entry("sub", "MOIC"),
      entry("user_name", "MOIC"),
      entry("auth_source", "none"),
      entry("legacy_username", "API_USER")

    )
  }
}
