package uk.gov.justice.digital.hmpps.oauth2server.verify

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.data.MapEntry.entry
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.*
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import java.time.LocalDateTime
import java.util.*
import javax.persistence.EntityNotFoundException

class TokenServiceTest {
  private val userTokenRepository: UserTokenRepository = mock()
  private val userService: UserService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val tokenService = TokenService(userTokenRepository, userService, telemetryClient)

  @Test
  fun `get token notfound`() {
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.empty())
    assertThat(tokenService.getToken(RESET, "token")).isEmpty
  }

  @Test
  fun `get token WrongType`() {
    val userToken = User.of("user").createToken(VERIFIED)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    assertThat(tokenService.getToken(RESET, "token")).isEmpty
  }

  @Test
  fun `get token`() {
    val userToken = User.of("user").createToken(RESET)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    assertThat(tokenService.getToken(RESET, "token")).get().isSameAs(userToken)
  }

  @Test
  fun `get user from token notfound`() {
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.empty())
    assertThatThrownBy { tokenService.getUserFromToken(RESET, "token") }.isInstanceOf(EntityNotFoundException::class.java)
  }

  @Test
  fun `get user from token WrongType`() {
    val userToken = User.of("user").createToken(VERIFIED)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    assertThatThrownBy { tokenService.getUserFromToken(RESET, "token") }.isInstanceOf(EntityNotFoundException::class.java)
  }

  @Test
  fun `get user from token token`() {
    val user = User.of("user")
    val userToken = user.createToken(RESET)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    assertThat(tokenService.getUserFromToken(RESET, "token")).isSameAs(user)
  }

  @Test
  fun checkToken() {
    val userToken = User.of("user").createToken(RESET)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    assertThat(tokenService.checkToken(RESET, "token")).isEmpty
  }

  @Test
  fun `checkToken invalid`() {
    val userToken = User.of("user").createToken(CHANGE)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    assertThat(tokenService.checkToken(RESET, "token")).get().isEqualTo("invalid")
  }

  @Test
  fun `checkToken expiredTelemetryUsername`() {
    val userToken = User.of("user").createToken(RESET)
    userToken.tokenExpiry = LocalDateTime.now().minusHours(1)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    tokenService.checkToken(RESET, "token")
    verify(telemetryClient).trackEvent(eq("ResetPasswordFailure"), check {
      assertThat(it).containsOnly(entry("username", "user"), entry("reason", "expired"))
    }, isNull())
  }

  @Test
  fun `checkToken expired`() {
    val userToken = User.of("joe").createToken(RESET)
    userToken.tokenExpiry = LocalDateTime.now().minusHours(1)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    assertThat(tokenService.checkToken(RESET, "token")).get().isEqualTo("expired")
  }

  @Test
  fun createToken() {
    val user = User.of("joe")
    whenever(userService.getOrCreateUser(anyString())).thenReturn(user)
    val token = tokenService.createToken(RESET, "token")
    assertThat(token).isNotNull()
    assertThat(user.tokens.map { it.token }).contains(token)
  }

  @Test
  fun `createToken check telemetry`() {
    val user = User.of("joe")
    whenever(userService.getOrCreateUser(anyString())).thenReturn(user)
    tokenService.createToken(RESET, "token")
    verify(telemetryClient).trackEvent(eq("ResetPasswordRequest"), check {
      assertThat(it).containsOnly(entry("username", "token"))
    }, isNull())
  }

  @Test
  fun `remove token notfound`() {
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.empty())
    tokenService.removeToken(RESET, "token")
    verify(userTokenRepository, never()).delete(any())
  }

  @Test
  fun `remove token WrongType`() {
    val userToken = User.of("user").createToken(VERIFIED)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    tokenService.removeToken(RESET, "token")
    verify(userTokenRepository, never()).delete(any())
  }

  @Test
  fun `remove token`() {
    val userToken = User.of("user").createToken(RESET)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    tokenService.removeToken(RESET, "token")
    verify(userTokenRepository).delete(userToken)
  }
}
