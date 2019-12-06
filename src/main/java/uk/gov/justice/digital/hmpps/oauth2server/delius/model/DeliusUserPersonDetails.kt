package uk.gov.justice.digital.hmpps.oauth2server.delius.model

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails
import java.util.stream.Collectors
import java.util.stream.Stream

data class DeliusUserPersonDetails(private val surname: String,
                                   private val firstName: String,
                                   private val username: String,
                                   private val enabled: Boolean = false,
                                   val email: String,
                                   private val roles: Collection<GrantedAuthority?> = emptySet()) : UserPersonDetails {

  override fun getUsername(): String = username

  override fun getFirstName(): String = firstName

  override fun getUserId(): String = username

  override fun getName(): String = String.format("%s %s", firstName, surname)

  override fun isAdmin(): Boolean = false

  override fun getAuthSource(): String = "delius"

  override fun toUser(): User =
      User.builder().username(username).source(AuthSource.delius).email(email).verified(true).build()

  override fun eraseCredentials() {}

  // add in ROLE_PROBATION to standard roles
  override fun getAuthorities(): Collection<GrantedAuthority?> =
      Stream.concat(roles.stream(), setOf(SimpleGrantedAuthority("ROLE_PROBATION")).stream()).collect(Collectors.toSet())

  override fun getPassword(): String = "password"

  override fun isAccountNonExpired(): Boolean = true

  override fun isAccountNonLocked(): Boolean = true

  override fun isCredentialsNonExpired(): Boolean = true

  override fun isEnabled(): Boolean = enabled
}
