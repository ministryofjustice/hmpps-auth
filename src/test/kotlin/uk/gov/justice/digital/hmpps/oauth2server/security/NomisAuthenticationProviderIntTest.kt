package uk.gov.justice.digital.hmpps.oauth2server.security

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.LockedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = MOCK)
@ActiveProfiles("test")
class NomisAuthenticationProviderIntTest {
  @Autowired
  private lateinit var provider: NomisAuthenticationProvider

  @Test
  fun authenticate_Success() {
    val auth = provider.authenticate(UsernamePasswordAuthenticationToken("ITAG_USER", "password"))
    assertThat(auth).isNotNull()
  }

  @Test
  fun authenticate_SuccessWithAuthorities() {
    val auth = provider.authenticate(UsernamePasswordAuthenticationToken("ITAG_USER_ADM", "password123456"))
    assertThat(auth).isNotNull()
    assertThat(auth.authorities).extracting<String> { it.authority }
      .containsOnly(
        "ROLE_PRISON",
        "ROLE_OAUTH_ADMIN",
        "ROLE_MAINTAIN_ACCESS_ROLES",
        "ROLE_KW_MIGRATION",
        "ROLE_MAINTAIN_OAUTH_USERS"
      )
  }

  @Test
  fun authenticate_NullUsername() {
    assertThatThrownBy { provider.authenticate(UsernamePasswordAuthenticationToken(null, "password")) }.isInstanceOf(
      MissingCredentialsException::class.java
    )
  }

  @Test
  fun authenticate_MissingUsername() {
    assertThatThrownBy {
      provider.authenticate(
        UsernamePasswordAuthenticationToken(
          "      ",
          "password"
        )
      )
    }.isInstanceOf(MissingCredentialsException::class.java)
  }

  @Test
  fun authenticate_MissingPassword() {
    assertThatThrownBy { provider.authenticate(UsernamePasswordAuthenticationToken("ITAG_USER", "   ")) }.isInstanceOf(
      MissingCredentialsException::class.java
    )
  }

  @Test
  fun authenticate_LockAfterThreeFailures() {
    assertThatThrownBy { provider.authenticate(UsernamePasswordAuthenticationToken("CA_USER", "wrong")) }.isInstanceOf(
      BadCredentialsException::class.java
    )
    assertThatThrownBy { provider.authenticate(UsernamePasswordAuthenticationToken("CA_USER", "wrong")) }.isInstanceOf(
      BadCredentialsException::class.java
    )
    assertThatThrownBy { provider.authenticate(UsernamePasswordAuthenticationToken("CA_USER", "wrong")) }.isInstanceOf(
      LockedException::class.java
    )
  }

  @Test
  fun `authenticate ResetAfterSuccess`() {
    assertThatThrownBy {
      provider.authenticate(
        UsernamePasswordAuthenticationToken(
          "RESET_USER",
          "wrong"
        )
      )
    }.isInstanceOf(BadCredentialsException::class.java)
    assertThatThrownBy {
      provider.authenticate(
        UsernamePasswordAuthenticationToken(
          "RESET_USER",
          "wrong"
        )
      )
    }.isInstanceOf(BadCredentialsException::class.java)

    // success in middle should cause reset of count
    val auth = provider.authenticate(UsernamePasswordAuthenticationToken("RESET_USER", "password"))
    assertThat(auth).isNotNull()
    assertThatThrownBy {
      provider.authenticate(
        UsernamePasswordAuthenticationToken(
          "RESET_USER",
          "wrong"
        )
      )
    }.isInstanceOf(BadCredentialsException::class.java)
  }

  @Test
  fun authenticate_ExpiredUserWrongPassword() {
    assertThatThrownBy {
      provider.authenticate(
        UsernamePasswordAuthenticationToken(
          "EXPIRED_USER",
          "wrong"
        )
      )
    }.isInstanceOf(BadCredentialsException::class.java)
  }

  @Test
  fun authenticate_ExpiredUser() {
    assertThatThrownBy {
      provider.authenticate(
        UsernamePasswordAuthenticationToken(
          "EXPIRED_USER",
          "password123456"
        )
      )
    }.isInstanceOf(CredentialsExpiredException::class.java)
  }
}
