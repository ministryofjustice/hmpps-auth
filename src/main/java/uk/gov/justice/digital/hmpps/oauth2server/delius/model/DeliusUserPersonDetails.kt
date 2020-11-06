package uk.gov.justice.digital.hmpps.oauth2server.delius.model

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails

data class DeliusUserPersonDetails(
  private val username: String,
  private val userId: String,
  private val firstName: String,
  private val surname: String,
  val email: String,
  private val enabled: Boolean = false,
  private val locked: Boolean = false,
  private val roles: Collection<GrantedAuthority?> = emptySet(),
) : UserPersonDetails {

  override fun getUsername(): String = username

  override fun getUserId(): String = userId

  override fun getFirstName(): String = firstName

  override fun getName(): String = "$firstName $surname"

  override fun isAdmin(): Boolean = false

  override fun getAuthSource(): String = "delius"

  override fun toUser(): User =
    User(
      username = username,
      source = AuthSource.delius,
      email = email,
      verified = true,
      enabled = enabled,
    )

  override fun eraseCredentials() {}

  // add in ROLE_PROBATION to standard roles
  override fun getAuthorities(): Collection<GrantedAuthority?> = roles.plus(SimpleGrantedAuthority("ROLE_PROBATION"))

  override fun getPassword(): String = "password"

  override fun isAccountNonExpired(): Boolean = true

  override fun isAccountNonLocked(): Boolean = !locked

  override fun isCredentialsNonExpired(): Boolean = true

  override fun isEnabled(): Boolean = enabled
}
