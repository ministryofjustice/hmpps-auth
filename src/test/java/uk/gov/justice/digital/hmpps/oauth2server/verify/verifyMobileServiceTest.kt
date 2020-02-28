package uk.gov.justice.digital.hmpps.oauth2server.verify

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException
import java.util.*

class VerifyMobileServiceTest {
  private val userRepository: UserRepository = mock()
  private val userTokenRepository: UserTokenRepository = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val notificationClient: NotificationClientApi = mock()
  private val verifyMobileService = VerifyMobileService(userRepository, userTokenRepository, telemetryClient, notificationClient, "templateId")


  @Test
  fun mobile() {
    val user = User.builder().username("bob").mobile("07987654321").build()
    whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenReturn(Optional.of(user))
    val userOptional = verifyMobileService.getMobile("user")
    Assertions.assertThat(userOptional).get().isEqualTo(user)
  }

  @Test
  fun mobile_NoMobileSet() {
    val user = User.of("bob")
    whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenReturn(Optional.of(user))
    val userOptionalOptional = verifyMobileService.getMobile("user")
    Assertions.assertThat(userOptionalOptional).isEmpty
  }

  @Test
  fun isNotVerified_userMissing() {
    whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenReturn(Optional.empty())
    Assertions.assertThat(verifyMobileService.isNotVerified("user")).isTrue()
    verify(userRepository).findByUsername("user")
  }

  @Test
  fun isNotVerified_userFoundNotVerified() {
    val user = User.of("bob")
    whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenReturn(Optional.of(user))
    Assertions.assertThat(verifyMobileService.isNotVerified("user")).isTrue()
  }

  @Test
  fun isNotVerified_userFoundVerified() {
    val user = User.builder().username("bob").mobile("07987654321").build()
    user.isVerified = true
    whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenReturn(Optional.of(user))
    Assertions.assertThat(verifyMobileService.isNotVerified("user")).isFalse()
  }

  @Test
  fun requestVerification_existingToken() {
    val user = User.of("someuser")
    val existingUserToken = user.createToken(UserToken.TokenType.MOBILE)
    whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenReturn(Optional.of(user))
    verifyMobileService.requestVerification("user", "07987654321")
    Assertions.assertThat(user.tokens).hasSize(1).extracting<String, RuntimeException> { it.token }.doesNotContain(existingUserToken.token)
  }

  @Test
  fun requestVerification_verifyToken() {
    val user = User.of("someuser")
    whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenReturn(Optional.of(user))
    val verification = verifyMobileService.requestVerification("user", "07987654321")
    val value = user.tokens.stream().findFirst().orElseThrow()
    Assertions.assertThat(verification).isEqualTo(value.token)
  }

  @Test
  fun requestVerification_saveMobile() {
    val user = User.of("someuser")
    whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenReturn(Optional.of(user))
    verifyMobileService.requestVerification("user", "07987654321")
    verify(userRepository).save(user)
    Assertions.assertThat(user.mobile).isEqualTo("07987654321")
    Assertions.assertThat(user.isVerified).isFalse()
  }

  @Test
  fun requestVerification_sendFailure() {
    val user = User.of("someuser")
    whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenReturn(Optional.of(user))
    whenever(notificationClient.sendSms(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyMap<String, Any?>(), isNull())).thenThrow(NotificationClientException("message"))
    Assertions.assertThatThrownBy { verifyMobileService.requestVerification("user", "07987654321") }.hasMessage("message")
  }

  @Test
  fun requestVerification_formatMobileInput() {
    val user = Optional.of(User.of("someuser"))
    whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenReturn(user)
    verifyMobileService.requestVerification("user", "07987654321")
    verify(notificationClient).sendSms(eq("templateId"), eq("07987654321"), ArgumentMatchers.anyMap<String, Any?>(), isNull())
  }

  @Test
  fun verifyMobile_Blank() {
    verifyMobileFailure("", "blank")
  }

  @Test
  fun verifyMobile_format() {
    verifyMobileFailure("0", "format")
  }

  private fun verifyMobileFailure(mobile: String, reason: String) {
    Assertions.assertThatThrownBy { verifyMobileService.validateMobileNumber(mobile) }.isInstanceOf(VerifyMobileService.VerifyMobileException::class.java).extracting("reason").isEqualTo(reason)
  }

}
