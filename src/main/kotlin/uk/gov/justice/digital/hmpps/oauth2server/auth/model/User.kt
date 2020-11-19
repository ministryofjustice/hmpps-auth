package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import org.apache.commons.lang3.StringUtils
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Type
import org.springframework.security.core.CredentialsContainer
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails
import java.time.LocalDateTime
import java.util.ArrayList
import java.util.Optional
import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.Transient

@Entity
@Table(name = "USERS")
class User(
  @Column(name = "username", nullable = false)
  private val username: String,

  @Embedded
  val person: Person? = null,

  @Column(name = "email")
  var email: String? = null,

  @Column(name = "verified", nullable = false)
  var verified: Boolean = false,

  @Column(name = "enabled", nullable = false)
  private var enabled: Boolean = false,

  /**
   * Source of last login information
   */
  @Column(name = "source", nullable = false)
  @Enumerated(EnumType.STRING)
  var source: AuthSource,

  authorities: Set<Authority> = emptySet(),

  groups: Set<Group> = emptySet(),

) : UserPersonDetails, CredentialsContainer {
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "user_id", updatable = false, nullable = false)
  @Type(type = "uuid-char")
  var id: UUID? = null

  @Column(name = "password")
  private var password: String? = null

  @OneToMany(fetch = FetchType.EAGER)
  @JoinTable(
    name = "user_role",
    joinColumns = [JoinColumn(name = "user_id")],
    inverseJoinColumns = [JoinColumn(name = "role_id")]
  )
  private val authorities: MutableSet<Authority> = authorities.toMutableSet()

  @OneToMany
  @JoinTable(
    name = "user_group",
    joinColumns = [JoinColumn(name = "user_id")],
    inverseJoinColumns = [JoinColumn(name = "group_id")]
  )
  val groups: MutableSet<Group> = groups.toMutableSet()

  @Column(name = "locked", nullable = false)
  var locked = false

  /**
   * Used for NOMIS accounts to force change password so that they don't get locked out due to not changing password
   */
  @Column(name = "password_expiry")
  var passwordExpiry: LocalDateTime = LocalDateTime.now()

  @Column(name = "last_logged_in")
  var lastLoggedIn: LocalDateTime = LocalDateTime.now()

  @Column(name = "mfa_preference")
  @Enumerated(EnumType.STRING)
  var mfaPreference = MfaPreferenceType.EMAIL

  @ElementCollection
  @CollectionTable(name = "USER_CONTACT", joinColumns = [JoinColumn(name = "user_id")])
  val contacts: MutableSet<Contact> = mutableSetOf()

  @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
  val tokens: MutableSet<UserToken> = mutableSetOf()

  override fun eraseCredentials() {
    password = null
  }

  override fun isAccountNonExpired(): Boolean = true

  override fun isAccountNonLocked(): Boolean = !locked

  override fun isCredentialsNonExpired(): Boolean = passwordExpiry.isAfter(LocalDateTime.now())

  override val name: String
    get() = person?.name ?: username

  override val firstName: String
    get() = person?.firstName ?: username

  @Transient
  override val isAdmin: Boolean = false

  override val authSource: String
    get() = source.toString()

  override fun toUser(): User = this

  override val userId: String
    get() = id.toString()

  fun createToken(tokenType: UserToken.TokenType): UserToken {
    val optionalToken = tokens.stream().filter { t: UserToken -> t.tokenType === tokenType }.findFirst()
    val token: UserToken
    if (optionalToken.isPresent) {
      token = optionalToken.get()
      token.resetExpiry()
    } else {
      token = UserToken(tokenType, this)
      tokens.add(token)
    }
    return token
  }

  fun removeToken(userToken: UserToken) {
    tokens.remove(userToken)
  }

  val isMaster: Boolean
    get() = source === AuthSource.auth
  val maskedMobile: String
    get() = "*******" + mobile!!.substring(7)
  val maskedEmail: String
    get() {
      val emailCharacters = StringUtils.substringBefore(email, "@").length
      val emailCharactersReduced = Math.min(emailCharacters / 2, 6)
      return email!!.substring(0, emailCharactersReduced) + "******@******" + email!!.substring(email!!.length - 7)
    }
  val maskedSecondaryEmail: String
    get() {
      val emailCharacters = StringUtils.substringBefore(secondaryEmail, "@").length
      val emailCharactersReduced = Math.min(emailCharacters / 2, 6)
      return secondaryEmail!!.substring(0, emailCharactersReduced) + "******@******" + secondaryEmail!!.substring(
        secondaryEmail!!.length - 7
      )
    }

  fun mfaPreferenceVerified(): Boolean =
    mfaPreferenceTextVerified() || mfaPreferenceEmailVerified() || mfaPreferenceSecondaryEmailVerified()

  fun mfaPreferenceTextVerified(): Boolean = mfaPreference == MfaPreferenceType.TEXT && isMobileVerified

  fun mfaPreferenceEmailVerified(): Boolean = mfaPreference == MfaPreferenceType.EMAIL && verified

  fun mfaPreferenceSecondaryEmailVerified(): Boolean =
    mfaPreference == MfaPreferenceType.SECONDARY_EMAIL && isSecondaryEmailVerified

  override fun getUsername(): String = username

  override fun getPassword(): String? = password

  override fun isEnabled(): Boolean = enabled

  val mobile: String?
    get() = getContactValue(ContactType.MOBILE_PHONE)
  val isMobileVerified: Boolean
    get() = isContactVerified(ContactType.MOBILE_PHONE)

  fun addContact(type: ContactType?, value: String?): Contact {
    val contact = Contact(
      type!!,
      value!!,
      false
    )
    // equals and hashcode by contact type so remove will remove any contact of same type
    contacts.remove(contact)
    contacts.add(contact)
    return contact
  }

  private fun isContactVerified(mobilePhone: ContactType): Boolean =
    findContact(mobilePhone).map(Contact::verified).orElse(false)

  private fun getContactValue(mobilePhone: ContactType): String? =
    findContact(mobilePhone).map(Contact::value).orElse(null)

  val secondaryEmail: String?
    get() = getContactValue(ContactType.SECONDARY_EMAIL)
  val isSecondaryEmailVerified: Boolean
    get() = isContactVerified(ContactType.SECONDARY_EMAIL)

  fun findContact(type: ContactType): Optional<Contact> {
    return contacts.stream()
      .filter { (type1) -> type1 === type }
      .findFirst()
  }

  override fun getAuthorities(): MutableSet<Authority> = authorities

  fun setPassword(password: String?) {
    this.password = password
  }

  fun setEnabled(enabled: Boolean) {
    this.enabled = enabled
  }

  fun hasVerifiedMfaMethod(): Boolean = !allowedMfaPreferences.isEmpty()

  fun calculateMfaFromPreference(): Optional<MfaPreferenceType> {
    val preferences = allowedMfaPreferences
    return if (preferences.contains(mfaPreference)) Optional.of(mfaPreference) else preferences.stream().findFirst()
  }

  override fun toString(): String =
    "User(username='$username', person=$person, email=$email, verified=$verified, enabled=$enabled, source=$source, id=$id, locked=$locked, passwordExpiry=$passwordExpiry, lastLoggedIn=$lastLoggedIn, mfaPreference=$mfaPreference)"

  private val allowedMfaPreferences: List<MfaPreferenceType>
    get() {
      val preferences = ArrayList<MfaPreferenceType>()
      if (StringUtils.isNotEmpty(email) && verified) preferences.add(MfaPreferenceType.EMAIL)
      findContact(ContactType.MOBILE_PHONE)
        .filter { c: Contact -> StringUtils.isNotBlank(c.value) && c.verified }
        .ifPresent { c: Contact? -> preferences.add(MfaPreferenceType.TEXT) }
      findContact(ContactType.SECONDARY_EMAIL)
        .filter { c: Contact -> StringUtils.isNotBlank(c.value) && c.verified }
        .ifPresent { c: Contact? -> preferences.add(MfaPreferenceType.SECONDARY_EMAIL) }
      return preferences
    }

  enum class MfaPreferenceType(val description: String) {
    EMAIL("email"),
    TEXT("text"),
    SECONDARY_EMAIL("secondary email");
  }

  enum class EmailType(val description: String) {
    PRIMARY("primary"),
    SECONDARY("secondary");
  }
}
