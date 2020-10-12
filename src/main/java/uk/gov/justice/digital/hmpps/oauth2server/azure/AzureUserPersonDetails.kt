package uk.gov.justice.digital.hmpps.oauth2server.azure

import org.springframework.security.core.GrantedAuthority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails

data class AzureUserPersonDetails(
  private val authorities: MutableCollection<GrantedAuthority>,
  private val enabled: Boolean,
  private val username: String,
  private val firstName: String,
  val surname: String,
  val email: String,
  private val credentialsNonExpired: Boolean,
  private val accountNonExpired: Boolean,
  private val accountNonLocked: Boolean,
) : UserPersonDetails {
  override fun getAuthorities(): Collection<GrantedAuthority?> = authorities

  override fun isEnabled(): Boolean = enabled

  override fun getUsername(): String = username

  override fun isCredentialsNonExpired(): Boolean = credentialsNonExpired

  override fun getPassword(): String = ""

  override fun isAccountNonExpired(): Boolean = accountNonExpired

  override fun isAccountNonLocked(): Boolean = accountNonLocked

  override fun getUserId(): String = email

  override fun getName(): String = "$firstName $surname"

  override fun getFirstName(): String = firstName

  override fun getAuthSource(): String? = AuthSource.azuread.name

  override fun eraseCredentials() {
  }

  override fun toUser(): User? {
    return User.builder()
      .username(username)
      .source(AuthSource.azuread)
      .email(email)
      .verified(true)
      .enabled(enabled)
      .person(Person(firstName, surname))
      .build()
  }

  override fun isAdmin(): Boolean = false
}
