package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import java.util.UUID

class UserHelper {

  companion object {
    fun createSampleUser(
      username: String = "firstlast",
      firstName: String = "first",
      lastName: String = "last",
      email: String? = null,
      verified: Boolean = false,
      enabled: Boolean = true,
      locked: Boolean = false,
      authorities: Set<Authority> = emptySet(),
      groups: Set<Group> = emptySet(),
      mobile: String? = null,
      mobileVerified: Boolean = false,
      secondaryEmail: String? = null,
      secondaryEmailVerified: Boolean = false,
      source: AuthSource = AuthSource.auth,
      mfaPreference: User.MfaPreferenceType = User.MfaPreferenceType.EMAIL,
      id: UUID? = null,
    ): User {

      val user = User(
        username = username,
        person = Person(firstName = firstName, lastName = lastName),
        email = email,
        verified = verified,
        enabled = enabled,
        authorities = authorities,
        groups = groups,
        source = source,
      )
      user.id = id
      if (mobile != null || mobileVerified) {
        val contact = user.addContact(ContactType.MOBILE_PHONE, mobile)
        contact.verified = mobileVerified
      }
      if (secondaryEmail != null || secondaryEmailVerified) {
        val contact = user.addContact(ContactType.SECONDARY_EMAIL, secondaryEmail)
        contact.verified = secondaryEmailVerified
      }

      user.locked = locked
      user.mfaPreference = mfaPreference

      return user
    }
  }
}

// return User.builder()
//   .id(UUID.randomUUID())
//   .username("firstlast")
//   .email("first.last@.justice.go.uk")
//   .verified(true)
//   .locked(false)
//   .enabled(true)
//   .source(AuthSource.auth)
//   .passwordExpiry(LocalDateTime.now().plusHours(1L))
//   .lastLoggedIn(LocalDateTime.now().minusHours(1L))
//   .mobile("07700 900322")
//   .mobileVerified(true)
//   .mfaPreference(User.MfaPreferenceType.EMAIL)
//   .person(Person("first", "last"))
//   .build()
