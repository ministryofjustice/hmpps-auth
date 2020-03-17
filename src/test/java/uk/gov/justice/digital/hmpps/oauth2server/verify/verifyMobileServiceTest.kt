package uk.gov.justice.digital.hmpps.oauth2server.verify

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.isNull
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyMobileService.VerifyMobileException
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
    val user = User.builder().username("bob").mobile("07700900321").build()
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    val userOptional = verifyMobileService.getMobile("user")
    assertThat(userOptional).get().isEqualTo(user)
  }

  @Test
  fun mobile_NoMobileSet() {
    val user = User.of("bob")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    val userOptionalOptional = verifyMobileService.getMobile("user")
    assertThat(userOptionalOptional).isEmpty
  }

  @Test
  fun isNotVerified_userMissing() {
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.empty())
    assertThat(verifyMobileService.isNotVerified("user")).isTrue()
    verify(userRepository).findByUsername("user")
  }

  @Test
  fun isNotVerified_userFoundNotVerified() {
    val user = User.of("bob")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    assertThat(verifyMobileService.isNotVerified("user")).isTrue()
  }

  @Test
  fun isNotVerified_userFoundVerified() {
    val user = User.builder().username("bob").mobile("07700900321").build()
    user.isVerified = true
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    assertThat(verifyMobileService.isNotVerified("user")).isFalse()
  }

  @Test
  fun requestVerification_existingToken() {
    val user = User.of("someuser")
    val existingUserToken = user.createToken(TokenType.MOBILE)
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    verifyMobileService.changeMobileAndRequestVerification("user", "07700900321")
    assertThat(user.tokens).hasSize(1).extracting<String> { it.token }.doesNotContain(existingUserToken.token)
  }

  @Test
  fun requestVerification_verifyToken() {
    val user = User.of("someuser")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    val verification = verifyMobileService.changeMobileAndRequestVerification("user", "07700900321")
    val value = user.tokens.stream().findFirst().orElseThrow()
    assertThat(verification).isEqualTo(value.token)
  }

  @Test
  fun requestVerification_saveMobile() {
    val user = User.of("someuser")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    verifyMobileService.changeMobileAndRequestVerification("user", "07700900321")
    verify(userRepository).save(user)
    assertThat(user.mobile).isEqualTo("07700900321")
    assertThat(user.isVerified).isFalse()
  }

  @Test
  fun requestVerification_sendFailure() {
    val user = User.of("someuser")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    whenever(notificationClient.sendSms(anyString(), anyString(), anyMap<String, Any?>(), isNull())).thenThrow(NotificationClientException("message"))
    assertThatThrownBy { verifyMobileService.changeMobileAndRequestVerification("user", "07700900321") }.hasMessage("message")
  }

  @Test
  fun requestVerification_formatMobileInput() {
    val user = Optional.of(User.of("someuser"))
    whenever(userRepository.findByUsername(anyString())).thenReturn(user)
    verifyMobileService.changeMobileAndRequestVerification("user", "07700900321")
    verify(notificationClient).sendSms(eq("templateId"), eq("07700900321"), anyMap<String, Any?>(), isNull())
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
    assertThatThrownBy { verifyMobileService.validateMobileNumber(mobile) }.isInstanceOf(VerifyMobileException::class.java).extracting("reason").isEqualTo(reason)
  }
}
