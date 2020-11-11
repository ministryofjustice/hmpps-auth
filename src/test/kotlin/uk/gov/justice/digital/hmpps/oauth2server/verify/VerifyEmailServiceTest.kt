package uk.gov.justice.digital.hmpps.oauth2server.verify

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException
import java.time.LocalDateTime
import java.util.Optional
import javax.persistence.EntityNotFoundException

class VerifyEmailServiceTest {
  private val userRepository: UserRepository = mock()
  private val userTokenRepository: UserTokenRepository = mock()
  private val jdbcTemplate: NamedParameterJdbcTemplate = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val notificationClient: NotificationClientApi = mock()
  private val referenceCodesService: ReferenceCodesService = mock()
  private val verifyEmailService = VerifyEmailService(
    userRepository,
    userTokenRepository,
    jdbcTemplate,
    telemetryClient,
    notificationClient,
    referenceCodesService,
    "templateId"
  )

  @Test
  fun email() {
    val user = UserHelper.createSampleUser(username = "bob", email = "joe@bob.com")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    val userOptional = verifyEmailService.getEmail("user")
    assertThat(userOptional).get().isEqualTo(user)
  }

  @Test
  fun email_NoEmailSet() {
    val user = UserHelper.createSampleUser(username = "bob")
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
    val user = UserHelper.createSampleUser(username = "bob")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    assertThat(verifyEmailService.isNotVerified("user")).isTrue()
  }

  @Test
  fun isNotVerified_userFoundVerified() {
    val user = UserHelper.createSampleUser(username = "bob", email = "joe@bob.com", verified = true)
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    assertThat(verifyEmailService.isNotVerified("user")).isFalse()
  }

  @Test
  fun requestVerification_existingToken() {
    val user = UserHelper.createSampleUser(username = "someuser")
    val existingUserToken = user.createToken(TokenType.VERIFIED)
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    whenever(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(true)
    verifyEmailService.requestVerification(
      "user",
      "email@john.com",
      "firstname",
      "full name",
      "url",
      User.EmailType.PRIMARY
    )
    assertThat(user.tokens).hasSize(1).extracting<String> { it.token }.containsExactly(existingUserToken.token)
  }

  @Test
  fun requestVerificationSecondaryEmail_existingToken() {
    val user = UserHelper.createSampleUser(username = "someuser")
    val existingUserToken = user.createToken(TokenType.SECONDARY)
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    whenever(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(true)
    verifyEmailService.requestVerification(
      "user",
      "email@john.com",
      "firstname",
      "full name",
      "url",
      User.EmailType.SECONDARY
    )
    assertThat(user.tokens).hasSize(1).extracting<String> { it.token }.containsExactly(existingUserToken.token)
  }

  @Test
  fun requestVerification_verifyToken() {
    val user = UserHelper.createSampleUser(username = "someuser")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    whenever(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(true)
    val verification = verifyEmailService.requestVerification(
      "user",
      "email@john.com",
      "full name",
      "firstname",
      "url",
      User.EmailType.PRIMARY
    )
    val value = user.tokens.stream().findFirst().orElseThrow()
    assertThat(verification).isEqualTo("url" + value.token)
  }

  @Test
  fun requestVerification_verifyToken_second() {
    val user = UserHelper.createSampleUser(username = "someuser")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    whenever(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(true)
    val verification = verifyEmailService.requestVerification(
      "user",
      "email@john.com",
      "firstname",
      "full name",
      "url",
      User.EmailType.SECONDARY
    )
    val value = user.tokens.stream().findFirst().orElseThrow()
    assertThat(verification).isEqualTo("url" + value.token)
  }

  @Test
  fun requestVerification_saveEmail() {
    val user = UserHelper.createSampleUser(username = "someuser")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    whenever(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(true)
    verifyEmailService.requestVerification(
      "user",
      "eMail@john.COM",
      "firstname",
      "full name",
      "url",
      User.EmailType.PRIMARY
    )
    verify(userRepository).save(user)
    assertThat(user.email).isEqualTo("email@john.com")
    assertThat(user.verified).isFalse()
  }

  @Test
  fun requestVerification_sendFailure() {
    val user = UserHelper.createSampleUser(username = "someuser")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    whenever(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(true)
    whenever(notificationClient.sendEmail(anyString(), anyString(), anyMap<String, Any?>(), isNull())).thenThrow(
      NotificationClientException("message")
    )
    assertThatThrownBy {
      verifyEmailService.requestVerification(
        "user",
        "email@john.com",
        "firstname",
        "full name",
        "url",
        User.EmailType.PRIMARY
      )
    }.hasMessage("message")
  }

  @Test
  fun requestVerification_formatEmailInput() {
    val user = Optional.of(UserHelper.createSampleUser(username = "someuser"))
    whenever(userRepository.findByUsername(anyString())).thenReturn(user)
    whenever(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(true)
    verifyEmailService.requestVerification(
      "user",
      "some.u’ser@SOMEwhere.COM",
      "firstname",
      "full name",
      "url",
      User.EmailType.PRIMARY
    )
    verify(notificationClient).sendEmail(
      eq("templateId"),
      eq("some.u'ser@somewhere.com"),
      anyMap<String, Any?>(),
      isNull()
    )
  }

  @Test
  fun requestVerification_gsiEmail() {
    val user = Optional.of(UserHelper.createSampleUser(username = "someuser"))
    whenever(userRepository.findByUsername(anyString())).thenReturn(user)
    whenever(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(true)
    verifyPrimaryEmailFailure("some.u'ser@SOMEwhe.gsi.gov.uk", "gsi")
  }

  @Test
  fun requestVerification_justiceEmail() {
    assertThatThrownBy {
      verifyEmailService.validateEmailAddressExcludingGsiAndFormatForJustice("someuser@justice.gov.uk", User.EmailType.PRIMARY)
    }.isInstanceOf(
      VerifyEmailException::class.java
    ).extracting("reason").isEqualTo("missingdot")
  }

  @Test
  fun verifyEmail_NoAtSign() {
    verifyPrimaryEmailFailure("a", "format")
    verifySecondaryEmailFailure("a", "format")
  }

  @Test
  fun verifyEmail_MultipleAtSigns() {
    verifyPrimaryEmailFailure("a@b.fred@joe.com", "at")
    verifySecondaryEmailFailure("a@b.fred@joe.com", "at")
  }

  @Test
  fun verifyEmail_NoExtension() {
    verifyPrimaryEmailFailure("a@bee", "format")
    verifySecondaryEmailFailure("a@bee", "format")
  }

  @Test
  fun verifyEmail_FirstLastStopFirst() {
    verifyPrimaryEmailFailure(".a@bee.com", "firstlast")
    verifySecondaryEmailFailure(".a@bee.com", "firstlast")
  }

  @Test
  fun verifyEmail_FirstLastStopLast() {
    verifyPrimaryEmailFailure("a@bee.com.", "firstlast")
    verifySecondaryEmailFailure("a@bee.com.", "firstlast")
  }

  @Test
  fun verifyEmail_FirstLastAtFirst() {
    verifyPrimaryEmailFailure("@a@bee.com", "firstlast")
    verifySecondaryEmailFailure("@a@bee.com", "firstlast")
  }

  @Test
  fun verifyEmail_FirstLastAtLast() {
    verifyPrimaryEmailFailure("a@bee.com@", "firstlast")
    verifySecondaryEmailFailure("a@bee.com@", "firstlast")
  }

  @Test
  fun verifyEmail_TogetherAtBefore() {
    verifyPrimaryEmailFailure("a@.com", "together")
    verifySecondaryEmailFailure("a@.com", "together")
  }

  @Test
  fun verifyEmail_TogetherAtAfter() {
    verifyPrimaryEmailFailure("a.@joe.com", "together")
    verifySecondaryEmailFailure("a.@joe.com", "together")
  }

  @Test
  fun verifyEmail_White() {
    verifyPrimaryEmailFailure("a@be\te.com", "white")
    verifySecondaryEmailFailure("a@be\te.com", "white")
  }

  @Test
  fun verifyEmail_InvalidCharacters() {
    verifyPrimaryEmailFailure("a@b.&com", "characters")
    verifySecondaryEmailFailure("a@b.&com", "characters")
  }

  @Test
  fun verifyEmail_InvalidDomain() {
    verifyPrimaryEmailFailure("a@b.com", "domain")
    verify(referenceCodesService).isValidEmailDomain("b.com")
  }

  private fun verifyPrimaryEmailFailure(email: String, reason: String) {
    assertThatThrownBy { verifyEmailService.validateEmailAddress(email, User.EmailType.PRIMARY) }.isInstanceOf(
      VerifyEmailException::class.java
    ).extracting("reason").isEqualTo(reason)
  }

  private fun verifySecondaryEmailFailure(email: String, reason: String) {
    assertThatThrownBy { verifyEmailService.validateEmailAddress(email, User.EmailType.SECONDARY) }.isInstanceOf(
      VerifyEmailException::class.java
    ).extracting("reason").isEqualTo(reason)
  }

  @Test
  fun `resendVerificationCodeEmail send code`() {
    val user = UserHelper.createSampleUser(firstName = "bob", lastName = "last", email = "someemail")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))

    val verifyLink = verifyEmailService.resendVerificationCodeEmail("bob", "http://some.url?token=")
    val token = user.tokens.first().token
    assertThat(verifyLink).get().isEqualTo("http://some.url?token=$token")

    verify(notificationClient).sendEmail(eq("templateId"), eq("someemail"), anyMap<String, Any?>(), isNull())
  }

  @Test
  fun `resendVerificationCodeEmail no email`() {
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(createSampleUser()))
    assertThatThrownBy { verifyEmailService.resendVerificationCodeEmail("bob", "http://some.url") }
      .isInstanceOf(VerifyEmailException::class.java).extracting("reason").isEqualTo("noemail")
  }

  @Test
  fun `resendVerificationCodeEmail already verified`() {
    val user = UserHelper.createSampleUser(email = "someemail", verified = true)
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    assertThat(
      verifyEmailService.resendVerificationCodeEmail(
        "bob",
        "http://some.url"
      )
    ).isEqualTo(Optional.empty<String>())
  }

  @Test
  fun `resendVerificationCodeSecondaryEmail send code`() {
    val user =
      UserHelper.createSampleUser(firstName = "bob", lastName = "last", secondaryEmail = "someemail")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))

    val verifyLink = verifyEmailService.resendVerificationCodeSecondaryEmail("bob", "http://some.url?token=")
    val token = user.tokens.first().token
    assertThat(verifyLink).get().isEqualTo("http://some.url?token=$token")

    verify(notificationClient).sendEmail(eq("templateId"), eq("someemail"), anyMap<String, Any?>(), isNull())
  }

  @Test
  fun `resendVerificationCodeSecondaryEmail no second email`() {
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(createSampleUser()))
    assertThatThrownBy { verifyEmailService.resendVerificationCodeSecondaryEmail("bob", "http://some.url") }
      .isInstanceOf(VerifyEmailException::class.java).extracting("reason").isEqualTo("nosecondaryemail")
  }

  @Test
  fun `resendVerificationCodeSecondaryEmail already verified`() {
    val user = UserHelper.createSampleUser(secondaryEmail = "someemail", secondaryEmailVerified = true)
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    assertThat(
      verifyEmailService.resendVerificationCodeSecondaryEmail(
        "bob",
        "http://some.url"
      )
    ).isEqualTo(Optional.empty<String>())
  }

  @Test
  fun `secondaryEmailVerified returns true`() {
    val user = UserHelper.createSampleUser(secondaryEmail = "someemail", secondaryEmailVerified = true)
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    assertThat(verifyEmailService.secondaryEmailVerified("bob")).isTrue()
  }

  @Test
  fun `secondaryEmailVerified returns false`() {
    val user = UserHelper.createSampleUser(secondaryEmail = "someemail")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    assertThat(verifyEmailService.secondaryEmailVerified("bob")).isFalse()
  }

  @Test
  fun `secondaryEmailVerified throws EntityNotFoundException`() {
    assertThatThrownBy { verifyEmailService.secondaryEmailVerified("bob") }
      .isInstanceOf(EntityNotFoundException::class.java).hasMessageContaining("User not found with username bob")
  }

  @Test
  fun confirmEmail_happyPath() {
    val user = UserHelper.createSampleUser(username = "bob")
    val userToken = user.createToken(TokenType.VERIFIED)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    val result = verifyEmailService.confirmEmail("token")
    assertThat(result).isEmpty
    verify(userRepository).save(user)
    assertThat(user.verified).isTrue()
  }

  @Test
  fun confirmSecondaryEmail_happyPath() {
    val user = UserHelper.createSampleUser(username = "bob")
    val userToken = user.createToken(TokenType.SECONDARY)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    val result = verifyEmailService.confirmEmail("token")
    assertThat(result).isEmpty
    verify(userRepository).save(user)
    assertThat(user.verified).isTrue()
  }

  @Test
  fun confirmEmail_invalid() {
    val result = verifyEmailService.confirmEmail("bob")
    assertThat(result).get().isEqualTo("invalid")
  }

  @Test
  fun confirmEmail_expired() {
    val user = UserHelper.createSampleUser(username = "bob")
    val userToken = user.createToken(TokenType.VERIFIED)
    userToken.tokenExpiry = LocalDateTime.now().minusSeconds(1)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    val result = verifyEmailService.confirmEmail("token")
    assertThat(result).get().isEqualTo("expired")
  }

  @Test
  fun confirmSecondaryEmail_expired() {
    val user = UserHelper.createSampleUser(username = "bob")
    val userToken = user.createToken(TokenType.SECONDARY)
    userToken.tokenExpiry = LocalDateTime.now().minusSeconds(1)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    val result = verifyEmailService.confirmEmail("token")
    assertThat(result).get().isEqualTo("expired")
  }
}
