package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ContactType.MOBILE_PHONE
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ContactType.SECONDARY_EMAIL
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.MfaPreferenceType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.CHANGE
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.RESET
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import java.time.LocalDateTime

@Suppress("UsePropertyAccessSyntax")
class UserTest {
  @Nested
  inner class CreateToken {
    @Test
    fun `test create token overwrites previous`() {
      val user = createSampleUser(username = "user")
      user.createToken(RESET)
      val changeToken = user.createToken(CHANGE)
      val resetToken = user.createToken(RESET)
      assertThat(user.tokens).containsOnly(changeToken, resetToken)
      assertThat(user.tokens).extracting<String> { it.token }.containsOnly(changeToken.token, resetToken.token)
    }

    @Test
    fun `test create token reuses existing token and resets expiry`() {
      val user = createSampleUser(username = "user")
      val token = user.createToken(RESET)
      val now = LocalDateTime.now()
      token.tokenExpiry = now

      val newToken = user.createToken(RESET)
      assertThat(newToken).isSameAs(token)
      val tomorrow = now.plusDays(1) // reset tokens expire one day later
      assertThat(newToken.tokenExpiry).isBetween(tomorrow.minusMinutes(5), tomorrow.plusMinutes(5))
    }
  }

  @Nested
  inner class AddContact {
    @Test
    fun `test add contact overwrites previous`() {
      val user = createSampleUser(username = "user")
      user.addContact(MOBILE_PHONE, "previous")
      val mobileContact = user.addContact(MOBILE_PHONE, "currentPhone")
      val emailContact = user.addContact(SECONDARY_EMAIL, "currentEmail")
      assertThat(user.contacts).containsOnly(mobileContact, emailContact)
      assertThat(user.contacts).extracting<String> { it.value }.containsOnly(mobileContact.value, emailContact.value)
    }

    @Test
    fun `test correct contact details retrieved not verified`() {
      val user = createSampleUser(username = "user")
      user.addContact(MOBILE_PHONE, "mobileValue")
      assertThat(user.mobile).isEqualTo("mobileValue")
      assertThat(user.isMobileVerified).isEqualTo(false)
    }

    @Test
    fun `test correct contact details retrieved verified`() {
      val user = createSampleUser(username = "user")
      user.addContact(MOBILE_PHONE, "mobileValue").verified = true
      assertThat(user.mobile).isEqualTo("mobileValue")
      assertThat(user.isMobileVerified).isEqualTo(true)
    }
  }

  @Nested
  inner class ToUser {
    @Test
    fun toUser() {
      val user = createSampleUser(username = "user")
      assertThat(user.toUser()).isSameAs(user)
    }

    @Test
    fun `to user auth source`() {
      val user = createSampleUser(username = "user", source = AuthSource.auth)
      assertThat(user.source).isEqualTo(AuthSource.auth)
    }
  }

  @Nested
  inner class fullName {
    @Test
    fun getName() {
      val user = createSampleUser(firstName = "First", lastName = "Last")
      assertThat(user.name).isEqualTo("First Last")
    }
  }

  @Nested
  inner class HasVerifiedMfaMethod {
    @Test
    fun `email and mobile null`() {
      val user = createSampleUser(username = "user")
      assertThat(user.hasVerifiedMfaMethod()).isFalse()
    }

    @Test
    fun `email set but not verified, no mobile`() {
      val user = createSampleUser(username = "user", email = "someemail")
      assertThat(user.hasVerifiedMfaMethod()).isFalse()
    }

    @Test
    fun `email set and verified, no mobile`() {
      val user = createSampleUser(username = "user", email = "someemail", verified = true)
      assertThat(user.hasVerifiedMfaMethod()).isTrue()
    }

    @Test
    fun `email not set but verified, no mobile`() {
      val user = createSampleUser(username = "user", verified = true)
      assertThat(user.hasVerifiedMfaMethod()).isFalse()
    }

    @Test
    fun `mobile set but not verified, no email`() {
      val user = createSampleUser(username = "user", mobile = "07")
      assertThat(user.hasVerifiedMfaMethod()).isFalse()
    }

    @Test
    fun `mobile set and verified, no email`() {
      val user = createSampleUser(username = "user", mobile = "07", mobileVerified = true)
      assertThat(user.hasVerifiedMfaMethod()).isTrue()
    }

    @Test
    fun `mobile not set but verified, no email`() {
      val user = createSampleUser(username = "user", mobile = "", mobileVerified = true)
      assertThat(user.hasVerifiedMfaMethod()).isFalse()
    }

    @Test
    fun `mobile set and verified, email set and verified`() {
      val user = createSampleUser(username = "user", email = "someemail", verified = true, mobile = "077009000000", mobileVerified = true)
      assertThat(user.hasVerifiedMfaMethod()).isTrue()
    }
  }

  @Nested
  inner class CalculateMfaFromPreference {
    @Test
    fun `email and mobile null`() {
      val user = createSampleUser(username = "user")
      assertThat(user.calculateMfaFromPreference()).isEmpty()
    }

    @Test
    fun `email set but not verified, no mobile`() {
      val user = createSampleUser(username = "user", email = "someemail")
      assertThat(user.calculateMfaFromPreference()).isEmpty()
    }

    @Test
    fun `email set and verified, no mobile`() {
      val user = createSampleUser(username = "user", email = "someemail", verified = true)
      assertThat(user.calculateMfaFromPreference()).get().isEqualTo(MfaPreferenceType.EMAIL)
    }

    @Test
    fun `email set and verified, no mobile, preference mobile`() {
      val user = createSampleUser(username = "user", email = "someemail", verified = true, mfaPreference = MfaPreferenceType.TEXT)
      assertThat(user.calculateMfaFromPreference()).get().isEqualTo(MfaPreferenceType.EMAIL)
    }

    @Test
    fun `email not set but verified, no mobile`() {
      val user = createSampleUser(username = "user", verified = true)
      assertThat(user.calculateMfaFromPreference()).isEmpty()
    }

    @Test
    fun `mobile set but not verified, no email`() {
      val user = createSampleUser(username = "user", mobile = "07")
      assertThat(user.calculateMfaFromPreference()).isEmpty()
    }

    @Test
    fun `mobile set and verified, no email`() {
      val user = createSampleUser(username = "user", mobile = "07", mobileVerified = true)
      assertThat(user.calculateMfaFromPreference()).get().isEqualTo(MfaPreferenceType.TEXT)
    }

    @Test
    fun `mobile not set but verified, no email`() {
      val user = createSampleUser(username = "user", mobile = "", mobileVerified = true)
      assertThat(user.calculateMfaFromPreference()).isEmpty()
    }

    @Test
    fun `mobile set and verified, email set and verified`() {
      val user = createSampleUser(username = "user", email = "someemail", verified = true, mobile = "07", mobileVerified = true)
      assertThat(user.calculateMfaFromPreference()).get().isEqualTo(MfaPreferenceType.EMAIL)
    }

    @Test
    fun `mobile set and verified, email set and verified, preference text`() {
      val user = createSampleUser(username = "user", email = "someemail", verified = true, mobile = "07", mobileVerified = true, mfaPreference = MfaPreferenceType.TEXT)
      assertThat(user.calculateMfaFromPreference()).get().isEqualTo(MfaPreferenceType.TEXT)
    }
  }

  @Nested
  inner class ApplyMask {
    @Test
    fun `getMaskedMobile check mask`() {
      val user = createSampleUser(username = "user", mobile = "07700900321", mobileVerified = true)
      assertThat(user.maskedMobile).isEqualTo("*******0321")
    }

    @Test
    fun `getMaskedEmail check mask`() {
      val user = createSampleUser(username = "user", email = "john.smithson34@digital.justice.gov.uk")
      assertThat(user.maskedEmail).isEqualTo("john.s******@******.gov.uk")
    }

    @Test
    fun `getMaskedEmail check mask short username`() {
      val user = createSampleUser(username = "user", email = "bob@digital.justice.gov.uk")
      assertThat(user.maskedEmail).isEqualTo("b******@******.gov.uk")
    }
  }

  @Nested
  inner class MfaPreferenceVerified {
    @Test
    fun `mfaPreferenceVerified preference email verified`() {
      val user = createSampleUser(username = "user", verified = true, mfaPreference = MfaPreferenceType.EMAIL)
      assertThat(user.mfaPreferenceVerified()).isTrue()
    }

    @Test
    fun `mfaPreferenceVerified preference email not verified`() {
      val user = createSampleUser(username = "user", verified = false, mfaPreference = MfaPreferenceType.EMAIL)
      assertThat(user.mfaPreferenceVerified()).isFalse()
    }

    @Test
    fun `mfaPreferenceVerified preference text verified`() {
      val user = createSampleUser(username = "user", mobile = "", mobileVerified = true, mfaPreference = MfaPreferenceType.TEXT)
      assertThat(user.mfaPreferenceVerified()).isTrue()
    }

    @Test
    fun `mfaPreferenceVerified preference text not verified`() {
      val user = createSampleUser(username = "user", mobile = "", mfaPreference = MfaPreferenceType.TEXT)
      assertThat(user.mfaPreferenceVerified()).isFalse()
    }

    @Test
    fun `mfaPreferenceEmailVerified verified`() {
      val user = createSampleUser(username = "user", verified = true, mfaPreference = MfaPreferenceType.EMAIL)
      assertThat(user.mfaPreferenceEmailVerified()).isTrue()
    }

    @Test
    fun `mfaPreferenceEmailVerified not verified`() {
      val user = createSampleUser(username = "user", mfaPreference = MfaPreferenceType.EMAIL)
      assertThat(user.mfaPreferenceEmailVerified()).isFalse()
    }

    @Test
    fun `mfaPreferenceTextVerified verified`() {
      val user = createSampleUser(username = "user", mobile = "", mobileVerified = true, mfaPreference = MfaPreferenceType.TEXT)
      assertThat(user.mfaPreferenceTextVerified()).isTrue()
    }

    @Test
    fun `mfaPreferenceTextVerified not verified`() {
      val user = createSampleUser(username = "user", mobile = "", mfaPreference = MfaPreferenceType.TEXT)
      assertThat(user.mfaPreferenceTextVerified()).isFalse()
    }
  }
}
