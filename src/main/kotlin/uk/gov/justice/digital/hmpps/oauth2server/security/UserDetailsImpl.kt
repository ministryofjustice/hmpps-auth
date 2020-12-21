package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

class UserDetailsImpl(
  username: String,
  override val name: String,
  authorities: Collection<GrantedAuthority>,
  override val authSource: String = AuthSource.none.source,
  override val userId: String,
  val jwtId: String,
  val passedMfa: Boolean = false,
) :
  User(username, "", authorities), UserPersonDetails {
  override val firstName: String
    get() = name
  override val isAdmin: Boolean = false

  override fun toUser(): uk.gov.justice.digital.hmpps.oauth2server.auth.model.User {
    throw IllegalStateException("Can't be converted into user")
  }

  companion object {
    // This class is serialized to the database (oauth_code table) during /auth/oauth/authorize and then read back
    // during /auth/oauth/token.  Therefore implemented serial version UID, although breaking changes should increment.
    private const val serialVersionUID = 1L
  }
}
