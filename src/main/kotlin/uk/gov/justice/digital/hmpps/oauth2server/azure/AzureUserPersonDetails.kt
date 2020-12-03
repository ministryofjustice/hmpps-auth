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
  override val firstName: String,
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

  override val userId: String
    get() = email

  override val name: String
    get() = "$firstName $surname"

  override val authSource: String
    get() = AuthSource.azuread.name

  override fun eraseCredentials() {
  }

  override fun toUser(): User {
    return User(
      username = username,
      source = AuthSource.azuread,
      email = email,
      verified = true,
      enabled = enabled,
      person = Person(firstName, surname),
    )
  }

  override val isAdmin: Boolean = false
}
