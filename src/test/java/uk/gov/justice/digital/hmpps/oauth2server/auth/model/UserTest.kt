package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.MfaPreferenceType
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource

@Suppress("UsePropertyAccessSyntax")
class UserTest {
  @Nested
  inner class CreateToken {
    @Test
    fun `test create token overwrites previous`() {
      val user = User.of("user")
      user.createToken(UserToken.TokenType.RESET)
      val changeToken = user.createToken(UserToken.TokenType.CHANGE)
      val resetToken = user.createToken(UserToken.TokenType.RESET)
      assertThat(user.tokens).containsOnly(changeToken, resetToken)
      assertThat(user.tokens).extracting<String> { obj: UserToken -> obj.token }.containsOnly(changeToken.token, resetToken.token)
    }
  }

  @Nested
  inner class ToUser {
    @Test
    fun toUser() {
      val user = User.of("user")
      assertThat(user.toUser()).isSameAs(user)
    }

    @Test
    fun `to user auth source`() {
      val user = userBuilder().source(AuthSource.auth).build().toUser()
      assertThat(user.source).isEqualTo(AuthSource.auth)
    }
  }

  @Nested
  inner class HasVerifiedMfaMethod {
    @Test
    fun `email and mobile null`() {
      val user = User.of("user")
      assertThat(user.hasVerifiedMfaMethod()).isFalse()
    }

    @Test
    fun `email set but not verified, no mobile`() {
      val user = userBuilder().email("someemail").build()
      assertThat(user.hasVerifiedMfaMethod()).isFalse()
    }

    @Test
    fun `email set and verified, no mobile`() {
      val user = userBuilder().email("someemail").verified(true).build()
      assertThat(user.hasVerifiedMfaMethod()).isTrue()
    }

    @Test
    fun `email not set but verified, no mobile`() {
      val user = userBuilder().verified(true).build()
      assertThat(user.hasVerifiedMfaMethod()).isFalse()
    }

    @Test
    fun `mobile set but not verified, no email`() {
      val user = userBuilder().mobile("someemail").build()
      assertThat(user.hasVerifiedMfaMethod()).isFalse()
    }

    @Test
    fun `mobile set and verified, no email`() {
      val user = userBuilder().mobile("someemail").mobileVerified(true).build()
      assertThat(user.hasVerifiedMfaMethod()).isTrue()
    }

    @Test
    fun `mobile not set but verified, no email`() {
      val user = userBuilder().mobileVerified(true).build()
      assertThat(user.hasVerifiedMfaMethod()).isFalse()
    }

    @Test
    fun `mobile set and verified, email set and verified`() {
      val user = userBuilder().email("someemail").verified(true).mobile("someemail").mobileVerified(true).build()
      assertThat(user.hasVerifiedMfaMethod()).isTrue()
    }
  }


  @Nested
  inner class CalculateMfaFromPreference {
    @Test
    fun `email and mobile null`() {
      val user = User.of("user")
      assertThat(user.calculateMfaFromPreference()).isEmpty()
    }

    @Test
    fun `email set but not verified, no mobile`() {
      val user = userBuilder().email("someemail").build()
      assertThat(user.calculateMfaFromPreference()).isEmpty()
    }

    @Test
    fun `email set and verified, no mobile`() {
      val user = userBuilder().email("someemail").verified(true).build()
      assertThat(user.calculateMfaFromPreference()).get().isEqualTo(MfaPreferenceType.EMAIL)
    }

    @Test
    fun `email set and verified, no mobile, preference mobile`() {
      val user = userBuilder().email("someemail").verified(true).mfaPreference(MfaPreferenceType.TEXT).build()
      assertThat(user.calculateMfaFromPreference()).get().isEqualTo(MfaPreferenceType.EMAIL)
    }

    @Test
    fun `email not set but verified, no mobile`() {
      val user = userBuilder().verified(true).build()
      assertThat(user.calculateMfaFromPreference()).isEmpty()
    }

    @Test
    fun `mobile set but not verified, no email`() {
      val user = userBuilder().mobile("someemail").build()
      assertThat(user.calculateMfaFromPreference()).isEmpty()
    }

    @Test
    fun `mobile set and verified, no email`() {
      val user = userBuilder().mobile("someemail").mobileVerified(true).build()
      assertThat(user.calculateMfaFromPreference()).get().isEqualTo(MfaPreferenceType.TEXT)
    }

    @Test
    fun `mobile not set but verified, no email`() {
      val user = userBuilder().mobileVerified(true).build()
      assertThat(user.calculateMfaFromPreference()).isEmpty()
    }

    @Test
    fun `mobile set and verified, email set and verified`() {
      val user = userBuilder().email("someemail").verified(true).mobile("someemail").mobileVerified(true).build()
      assertThat(user.calculateMfaFromPreference()).get().isEqualTo(MfaPreferenceType.EMAIL)
    }

    @Test
    fun `mobile set and verified, email set and verified, preference text`() {
      val user = userBuilder().email("someemail").verified(true).mobile("someemail").mobileVerified(true).mfaPreference(MfaPreferenceType.TEXT).build()
      assertThat(user.calculateMfaFromPreference()).get().isEqualTo(MfaPreferenceType.TEXT)
    }
  }

  @Nested
  inner class ApplyMask {
    @Test
    fun `getMaskedMobile check mask`() {
      val user = User.builder().mobile("07700900321").build()
      assertThat(user.getMaskedMobile()).isEqualTo("*******0321")
    }

    @Test
    fun `getMaskedEmail check mask`() {
      val user = User.builder().email("john.smithson34@digital.justice.gov.uk").build()
      assertThat(user.getMaskedEmail()).isEqualTo("john.s******@******.gov.uk")
    }

    @Test
    fun `getMaskedEmail check mask short username`() {
      val user = User.builder().email("bob@digital.justice.gov.uk").build()
      assertThat(user.getMaskedEmail()).isEqualTo("b******@******.gov.uk")
    }
  }

  private fun userBuilder() = User.builder().username("user")
}
