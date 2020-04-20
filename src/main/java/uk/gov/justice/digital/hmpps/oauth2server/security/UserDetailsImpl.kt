package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

class UserDetailsImpl(username: String, private val name: String, authorities: Collection<GrantedAuthority>,
                      private val authSource: String, private val userId: String, val jwtId: String) :
    User(username, "", authorities), UserPersonDetails {
  override fun getName(): String = name
  override fun getAuthSource(): String = authSource
  override fun getFirstName(): String = name
  override fun getUserId(): String = userId

  override fun isAdmin(): Boolean = false

  override fun toUser(): uk.gov.justice.digital.hmpps.oauth2server.auth.model.User {
    throw IllegalStateException("Can't be converted into user")
  }

  companion object {
    // This class is serialized to the database (oauth_code table) during /auth/oauth/authorize and then read back
    // during /auth/oauth/token.  Therefore implemented serial version UID, although breaking changes should increment.
    private const val serialVersionUID = 1L
  }
}
