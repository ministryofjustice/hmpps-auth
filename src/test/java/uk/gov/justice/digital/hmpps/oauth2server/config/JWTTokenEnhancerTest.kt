@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.config

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import java.util.*

@ExtendWith(SpringExtension::class)
internal class JWTTokenEnhancerTest {
  private val authentication: OAuth2Authentication = mock()

  @Test
  fun testEnhance_HasUserToken() {
    val token: OAuth2AccessToken = DefaultOAuth2AccessToken("value")
    whenever(authentication.isClientOnly).thenReturn(false)
    val uuid = UUID.randomUUID()
    val user = User.builder().id(uuid).username("user").source(AuthSource.auth).build()
    whenever(authentication.userAuthentication).thenReturn(UsernamePasswordAuthenticationToken(user, "pass"))
    JWTTokenEnhancer().enhance(token, authentication)
    assertThat(token.additionalInformation).containsOnly(
        entry("user_name", "user"),
        entry("auth_source", "auth"),
        entry("user_id", uuid.toString()))
  }

  @Test
  fun testEnhance_MissingAuthSource() {
    val token: OAuth2AccessToken = DefaultOAuth2AccessToken("value")
    whenever(authentication.isClientOnly).thenReturn(false)
    whenever(authentication.userAuthentication).thenReturn(UsernamePasswordAuthenticationToken(UserDetailsImpl("user", null, emptyList(), null, "userID"), "pass"))
    JWTTokenEnhancer().enhance(token, authentication)
    assertThat(token.additionalInformation).containsOnly(
        entry("user_name", "user"),
        entry("auth_source", "none"),
        entry("user_id", "userID"))
  }
}
