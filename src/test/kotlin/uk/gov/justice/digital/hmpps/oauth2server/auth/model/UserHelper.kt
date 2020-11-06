package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import java.time.LocalDateTime
import java.util.UUID

class UserHelper {

  companion object {
    fun createSampleUser(
      username: String = "firstlast",
      firstName: String = "first",
      lastName: String = "last",
      email: String? = null,
      verified: Boolean = false,
      enabled: Boolean = false,
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
      password: String? = null,
      lastLoggedIn: LocalDateTime? = null,
      passwordExpiry: LocalDateTime? = null,
      person: Person? = null,
    ): User {

      val user = User(
        username = username,
        person = person ?: Person(firstName = firstName, lastName = lastName),
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
      if (lastLoggedIn != null) user.lastLoggedIn = lastLoggedIn
      user.setPassword(password)
      if (passwordExpiry != null) user.passwordExpiry = passwordExpiry

      return user
    }
  }
}
