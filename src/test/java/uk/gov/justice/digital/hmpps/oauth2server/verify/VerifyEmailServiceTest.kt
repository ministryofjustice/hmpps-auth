package uk.gov.justice.digital.hmpps.oauth2server.verify

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.isNull
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.springframework.jdbc.core.JdbcTemplate
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException
import java.time.LocalDateTime
import java.util.*

class VerifyEmailServiceTest {
  private val userRepository: UserRepository = mock()
  private val userTokenRepository: UserTokenRepository = mock()
  private val jdbcTemplate: JdbcTemplate = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val notificationClient: NotificationClientApi = mock()
  private val referenceCodesService: ReferenceCodesService = mock()
  private val verifyEmailService = VerifyEmailService(userRepository, userTokenRepository, jdbcTemplate, telemetryClient, notificationClient, referenceCodesService, "templateId")

  @Test
  fun email() {
    val user = User.builder().username("bob").email("joe@bob.com").build()
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    val userOptional = verifyEmailService.getEmail("user")
    assertThat(userOptional).get().isEqualTo(user)
  }

  @Test
  fun email_NoEmailSet() {
    val user = User.of("bob")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    val userOptionalOptional = verifyEmailService.getEmail("user")
    assertThat(userOptionalOptional).isEmpty
  }

  @Test
  fun isNotVerified_userMissing() {
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.empty())
    assertThat(verifyEmailService.isNotVerified("user")).isTrue()
    verify(userRepository).findByUsername("user")
  }

  @Test
  fun isNotVerified_userFoundNotVerified() {
    val user = User.of("bob")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    assertThat(verifyEmailService.isNotVerified("user")).isTrue()
  }

  @Test
  fun isNotVerified_userFoundVerified() {
    val user = User.builder().username("bob").email("joe@bob.com").build()
    user.isVerified = true
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    assertThat(verifyEmailService.isNotVerified("user")).isFalse()
  }

  @Test
  fun requestVerification_existingToken() {
    val user = User.of("someuser")
    val existingUserToken = user.createToken(TokenType.VERIFIED)
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    whenever(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(true)
    verifyEmailService.requestVerification("user", "email@john.com", "firstname", "url", User.EmailType.PRIMARY)
    assertThat(user.tokens).hasSize(1).extracting<String> { it.token }.doesNotContain(existingUserToken.token)
  }

  @Test
  fun requestVerification_verifyToken() {
    val user = User.of("someuser")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    whenever(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(true)
    val verification = verifyEmailService.requestVerification("user", "email@john.com", "firstname", "url", User.EmailType.PRIMARY)
    val value = user.tokens.stream().findFirst().orElseThrow()
    assertThat(verification).isEqualTo("url" + value.token)
  }

  @Test
  fun requestVerification_verifyToken_second() {
    val user = User.of("someuser")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    whenever(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(true)
    val verification = verifyEmailService.requestVerification("user", "email@john.com", "firstname", "url", User.EmailType.SECONDARY)
    val value = user.tokens.stream().findFirst().orElseThrow()
    assertThat(verification).isEqualTo("url" + value.token)
  }

  @Test
  fun requestVerification_saveEmail() {
    val user = User.of("someuser")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    whenever(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(true)
    verifyEmailService.requestVerification("user", "eMail@john.COM", "firstname", "url", User.EmailType.PRIMARY)
    verify(userRepository).save(user)
    assertThat(user.email).isEqualTo("email@john.com")
    assertThat(user.isVerified).isFalse()
  }

  @Test
  fun requestVerification_sendFailure() {
    val user = User.of("someuser")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    whenever(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(true)
    whenever(notificationClient.sendEmail(anyString(), anyString(), anyMap<String, Any?>(), isNull())).thenThrow(NotificationClientException("message"))
    assertThatThrownBy { verifyEmailService.requestVerification("user", "email@john.com", "firstname", "url", User.EmailType.PRIMARY) }.hasMessage("message")
  }

  @Test
  fun requestVerification_formatEmailInput() {
    val user = Optional.of(User.of("someuser"))
    whenever(userRepository.findByUsername(anyString())).thenReturn(user)
    whenever(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(true)
    verifyEmailService.requestVerification("user", "some.u’ser@SOMEwhere.COM", "firstname", "url", User.EmailType.PRIMARY)
    verify(notificationClient).sendEmail(eq("templateId"), eq("some.u'ser@somewhere.com"), anyMap<String, Any?>(), isNull())
  }

  @Test
  fun requestVerification_gsiEmail() {
    val user = Optional.of(User.of("someuser"))
    whenever(userRepository.findByUsername(anyString())).thenReturn(user)
    whenever(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(true)
    verifyEmailFailure("some.u'ser@SOMEwhe.gsi.gov.uk", "gsi")
  }

  @Test
  fun verifyEmail_NoAtSign() {
    verifyEmailFailure("a", "format")
  }

  @Test
  fun verifyEmail_MultipleAtSigns() {
    verifyEmailFailure("a@b.fred@joe.com", "at")
  }

  @Test
  fun verifyEmail_NoExtension() {
    verifyEmailFailure("a@bee", "format")
  }

  @Test
  fun verifyEmail_FirstLastStopFirst() {
    verifyEmailFailure(".a@bee.com", "firstlast")
  }

  @Test
  fun verifyEmail_FirstLastStopLast() {
    verifyEmailFailure("a@bee.com.", "firstlast")
  }

  @Test
  fun verifyEmail_FirstLastAtFirst() {
    verifyEmailFailure("@a@bee.com", "firstlast")
  }

  @Test
  fun verifyEmail_FirstLastAtLast() {
    verifyEmailFailure("a@bee.com@", "firstlast")
  }

  @Test
  fun verifyEmail_TogetherAtBefore() {
    verifyEmailFailure("a@.com", "together")
  }

  @Test
  fun verifyEmail_TogetherAtAfter() {
    verifyEmailFailure("a.@joe.com", "together")
  }

  @Test
  fun verifyEmail_White() {
    verifyEmailFailure("a@be\te.com", "white")
  }

  @Test
  fun verifyEmail_InvalidDomain() {
    verifyEmailFailure("a@b.com", "domain")
    verify(referenceCodesService).isValidEmailDomain("b.com")
  }

  @Test
  fun verifyEmail_InvalidCharacters() {
    verifyEmailFailure("a@b.&com", "characters")
  }

  private fun verifyEmailFailure(email: String, reason: String) {
    assertThatThrownBy { verifyEmailService.validateEmailAddress(email, User.EmailType.PRIMARY) }.isInstanceOf(VerifyEmailException::class.java).extracting("reason").isEqualTo(reason)
  }

  @Test
  fun confirmEmail_happyPath() {
    val user = User.of("bob")
    val userToken = user.createToken(TokenType.VERIFIED)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    val result = verifyEmailService.confirmEmail("token")
    assertThat(result).isEmpty
    verify(userRepository).save(user)
    assertThat(user.isVerified).isTrue()
  }

  @Test
  fun confirmEmail_invalid() {
    val result = verifyEmailService.confirmEmail("bob")
    assertThat(result).get().isEqualTo("invalid")
  }

  @Test
  fun confirmEmail_expired() {
    val user = User.of("bob")
    val userToken = user.createToken(TokenType.VERIFIED)
    userToken.tokenExpiry = LocalDateTime.now().minusSeconds(1)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    val result = verifyEmailService.confirmEmail("token")
    assertThat(result).get().isEqualTo("expired")
  }
}
