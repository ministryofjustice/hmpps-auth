package uk.gov.justice.digital.hmpps.oauth2server.security

import com.google.common.collect.Sets
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository

@Service
class MaintainUserCheck(
  private val userRepository: UserRepository
) {

  companion object {
    fun canMaintainAuthUsers(authorities: Collection<GrantedAuthority>): Boolean =
      authorities.map { it.authority }
        .any { it == "ROLE_MAINTAIN_OAUTH_USERS" }
  }

  @Throws(AuthUserGroupRelationshipException::class)
  fun ensureUserLoggedInUserRelationship(loggedInUser: String, authorities: Collection<GrantedAuthority>, user: User) {
    // if they have maintain privileges then all good
    if (canMaintainAuthUsers(authorities)) {
      return
    }
    // otherwise group managers must have a group in common for maintenance
    val loggedInUserEmail = userRepository.findByUsernameAndMasterIsTrue(loggedInUser).orElseThrow()
    if (Sets.intersection(loggedInUserEmail.groups, user.groups).isEmpty()) {
      // no group in common, so disallow
      throw AuthUserGroupRelationshipException(user.name, "User not with your groups")
    }
  }

  @Throws(AuthGroupRelationshipException::class)
  fun ensureMaintainerGroupRelationship(
    maintainerName: String,
    groupCode: String,
    authorities: Collection<GrantedAuthority>
  ) {
    // if they have maintain privileges then all good
    if (canMaintainAuthUsers(authorities)) {
      return
    }
    val maintainer = userRepository.findByUsernameAndMasterIsTrue(maintainerName).orElseThrow()
    // otherwise group managers must have a group in common for maintenance
    if (maintainer.groups.none { it.groupCode == groupCode }) {
      // no group in common, so disallow
      throw AuthGroupRelationshipException(groupCode, "Group not with your groups")
    }
  }

  class AuthUserGroupRelationshipException(val username: String, val errorCode: String) :
    Exception("Unable to maintain user: $username with reason: $errorCode")

  class AuthGroupRelationshipException(val group: String, val errorCode: String) :
    Exception("Unable to maintain group: $group with reason: $errorCode")
}
