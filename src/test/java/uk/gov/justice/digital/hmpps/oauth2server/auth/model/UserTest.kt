package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ContactType.MOBILE_PHONE
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ContactType.SECONDARY_EMAIL
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.MfaPreferenceType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.CHANGE
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.RESET
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource

@Suppress("UsePropertyAccessSyntax")
class UserTest {
  @Nested
  inner class CreateToken {
    @Test
    fun `test create token overwrites previous`() {
      val user = User.of("user")
      user.createToken(RESET)
      val changeToken = user.createToken(CHANGE)
      val resetToken = user.createToken(RESET)
      assertThat(user.tokens).containsOnly(changeToken, resetToken)
      assertThat(user.tokens).extracting<String> { it.token }.containsOnly(changeToken.token, resetToken.token)
    }
  }

  @Nested
  inner class AddContact {
    @Test
    fun `test add contact overwrites previous`() {
      val user = User.of("user")
      user.addContact(MOBILE_PHONE, "previous")
      val mobileContact = user.addContact(MOBILE_PHONE, "currentPhone")
      val emailContact = user.addContact(SECONDARY_EMAIL, "currentEmail")
      assertThat(user.contacts).containsOnly(mobileContact, emailContact)
      assertThat(user.contacts).extracting<String> { it.value }.containsOnly(mobileContact.value, emailContact.value)
    }

    @Test
    fun `test correct contact details retrieved not verified`() {
      val user = User.of("user")
      user.addContact(MOBILE_PHONE, "mobileValue")
      assertThat(user.mobile).isEqualTo("mobileValue")
      assertThat(user.isMobileVerified).isEqualTo(false)
    }

    @Test
    fun `test correct contact details retrieved verified`() {
      val user = User.of("user")
      user.addContact(MOBILE_PHONE, "mobileValue").verified = true
      assertThat(user.mobile).isEqualTo("mobileValue")
      assertThat(user.isMobileVerified).isEqualTo(true)
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
      val user = userBuilder().contacts(setOf(Contact(MOBILE_PHONE, "someemail"))).build()
      assertThat(user.hasVerifiedMfaMethod()).isFalse()
    }

    @Test
    fun `mobile set and verified, no email`() {
      val user = userBuilder().contacts(setOf(Contact(MOBILE_PHONE, "someemail", true))).build()
      assertThat(user.hasVerifiedMfaMethod()).isTrue()
    }

    @Test
    fun `mobile not set but verified, no email`() {
      val user = userBuilder().contacts(setOf(Contact(MOBILE_PHONE, "", true))).build()
      assertThat(user.hasVerifiedMfaMethod()).isFalse()
    }

    @Test
    fun `mobile set and verified, email set and verified`() {
      val user = userBuilder().email("someemail").verified(true).contacts(setOf(Contact(MOBILE_PHONE, "077009000000", true))).build()
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
      val user = userBuilder().contacts(setOf(Contact(MOBILE_PHONE, "someemail"))).build()
      assertThat(user.calculateMfaFromPreference()).isEmpty()
    }

    @Test
    fun `mobile set and verified, no email`() {
      val user = userBuilder().contacts(setOf(Contact(MOBILE_PHONE, "someemail", true))).build()
      assertThat(user.calculateMfaFromPreference()).get().isEqualTo(MfaPreferenceType.TEXT)
    }

    @Test
    fun `mobile not set but verified, no email`() {
      val user = userBuilder().contacts(setOf(Contact(MOBILE_PHONE, "", true))).build()
      assertThat(user.calculateMfaFromPreference()).isEmpty()
    }

    @Test
    fun `mobile set and verified, email set and verified`() {
      val user = userBuilder().email("someemail").verified(true).contacts(setOf(Contact(MOBILE_PHONE, "someemail", true))).build()
      assertThat(user.calculateMfaFromPreference()).get().isEqualTo(MfaPreferenceType.EMAIL)
    }

    @Test
    fun `mobile set and verified, email set and verified, preference text`() {
      val user = userBuilder().email("someemail").verified(true).contacts(setOf(Contact(MOBILE_PHONE, "someemail", true))).mfaPreference(MfaPreferenceType.TEXT).build()
      assertThat(user.calculateMfaFromPreference()).get().isEqualTo(MfaPreferenceType.TEXT)
    }
  }

  @Nested
  inner class ApplyMask {
    @Test
    fun `getMaskedMobile check mask`() {
      val user = User.builder().contacts(setOf(Contact(MOBILE_PHONE, "07700900321"))).build()
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

  @Nested
  inner class MfaPreferenceVerified {
    @Test
    fun `mfaPreferenceVerified preference email verified`() {
      val user = User.builder().verified(true).mfaPreference(MfaPreferenceType.EMAIL).build()
      assertThat(user.mfaPreferenceVerified()).isTrue()
    }

    @Test
    fun `mfaPreferenceVerified preference email not verified`() {
      val user = User.builder().verified(false).mfaPreference(MfaPreferenceType.EMAIL).build()
      assertThat(user.mfaPreferenceVerified()).isFalse()
    }

    @Test
    fun `mfaPreferenceVerified preference text verified`() {
      val user = User.builder().contacts(setOf(Contact(MOBILE_PHONE, "", true))).mfaPreference(MfaPreferenceType.TEXT).build()
      assertThat(user.mfaPreferenceVerified()).isTrue()
    }

    @Test
    fun `mfaPreferenceVerified preference text not verified`() {
      val user = User.builder().contacts(setOf(Contact(MOBILE_PHONE, ""))).mfaPreference(MfaPreferenceType.TEXT).build()
      assertThat(user.mfaPreferenceVerified()).isFalse()
    }

    @Test
    fun `mfaPreferenceEmailVerified verified`() {
      val user = User.builder().verified(true).mfaPreference(MfaPreferenceType.EMAIL).build()
      assertThat(user.mfaPreferenceEmailVerified()).isTrue()
    }

    @Test
    fun `mfaPreferenceEmailVerified not verified`() {
      val user = User.builder().verified(false).mfaPreference(MfaPreferenceType.EMAIL).build()
      assertThat(user.mfaPreferenceEmailVerified()).isFalse()
    }

    @Test
    fun `mfaPreferenceTextVerified verified`() {
      val user = User.builder().contacts(setOf(Contact(MOBILE_PHONE, "", true))).mfaPreference(MfaPreferenceType.TEXT).build()
      assertThat(user.mfaPreferenceTextVerified()).isTrue()
    }

    @Test
    fun `mfaPreferenceTextVerified not verified`() {
      val user = User.builder().contacts(setOf(Contact(MOBILE_PHONE, ""))).mfaPreference(MfaPreferenceType.TEXT).build()
      assertThat(user.mfaPreferenceTextVerified()).isFalse()
    }
  }

  private fun userBuilder() = User.builder().username("user")
}
