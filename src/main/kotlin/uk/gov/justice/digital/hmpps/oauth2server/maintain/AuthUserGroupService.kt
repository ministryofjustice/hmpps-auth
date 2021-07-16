package uk.gov.justice.digital.hmpps.oauth2server.maintain

import com.microsoft.applicationinsights.TelemetryClient
import org.hibernate.Hibernate
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.GroupRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.Companion.canMaintainAuthUsers
import java.util.UUID

@Service
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
class AuthUserGroupService(
  private val userRepository: UserRepository,
  private val groupRepository: GroupRepository,
  private val maintainUserCheck: MaintainUserCheck,
  private val telemetryClient: TelemetryClient
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(
    AuthUserGroupException::class,
    AuthUserGroupManagerException::class,
    AuthUserGroupRelationshipException::class
  )
  fun addGroup(username: String, groupCode: String, modifier: String, authorities: Collection<GrantedAuthority>) {
    // already checked that user exists
    val user = userRepository.findByUsernameAndMasterIsTrue(username).orElseThrow()
    val groupFormatted = formatGroup(groupCode)

    // check that group exists
    val group =
      groupRepository.findByGroupCode(groupFormatted) ?: throw AuthUserGroupException("group", "notfound")
    if (user.groups.contains(group)) {
      throw AuthUserGroupExistsException()
    }
    // check that modifier is able to add user to group
    if (!checkGroupModifier(groupCode, authorities, modifier)) {
      throw AuthUserGroupManagerException("Add", "group", "managerNotMember")
    }
    // check that modifier is able to maintain the user
    maintainUserCheck.ensureUserLoggedInUserRelationship(modifier, authorities, user)

    log.info("Adding group {} to user {}", groupFormatted, username)
    user.groups.add(group)
    user.authorities.addAll(group.assignableRoles.filter { it.automatic }.map { it.role })
    telemetryClient.trackEvent(
      "AuthUserGroupAddSuccess",
      mapOf("username" to username, "group" to groupFormatted, "admin" to modifier),
      null
    )
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(AuthUserGroupException::class, AuthUserGroupManagerException::class, AuthUserLastGroupException::class)
  fun removeGroup(username: String, groupCode: String, modifier: String, authorities: Collection<GrantedAuthority>) {
    val groupFormatted = formatGroup(groupCode)
    // already checked that user exists
    val user = userRepository.findByUsernameAndMasterIsTrue(username).orElseThrow()
    if (user.groups.map { it.groupCode }.none { it == groupFormatted }
    ) {
      throw AuthUserGroupException("group", "missing")
    }
    if (!checkGroupModifier(groupFormatted, authorities, modifier)) {
      throw AuthUserGroupManagerException("delete", "group", "managerNotMember")
    }

    if (user.groups.count() == 1 && !canMaintainAuthUsers(authorities)) {
      throw AuthUserLastGroupException("group", "last")
    }

    log.info("Removing group {} from user {}", groupFormatted, username)
    user.groups.removeIf { a: Group -> a.groupCode == groupFormatted }
    telemetryClient.trackEvent(
      "AuthUserGroupRemoveSuccess",
      mapOf("username" to username, "group" to groupCode, "admin" to modifier),
      null
    )
  }

  private fun checkGroupModifier(
    groupCode: String,
    authorities: Collection<GrantedAuthority>,
    modifier: String
  ): Boolean {
    return if (canMaintainAuthUsers(authorities)) {
      true
    } else {
      val modifierGroups = getAssignableGroups(modifier, authorities)
      return modifierGroups.map { it.groupCode }.contains(groupCode)
    }
  }

  private fun formatGroup(group: String) = group.trim().uppercase()

  val allGroups: List<Group>
    get() = groupRepository.findAllByOrderByGroupName()

  fun getAuthUserGroups(username: String): Set<Group>? {
    val user = userRepository.findByUsernameAndMasterIsTrue(username.trim().uppercase())
    return user.map { u: User ->
      Hibernate.initialize(u.groups)
      u.groups.forEach { Hibernate.initialize(it.children) }
      u.groups
    }.orElse(null)
  }

  fun getAuthUserGroupsByUserId(userId: String, admin: String, authorities: Collection<GrantedAuthority>): Set<Group>? =
    userRepository.findByIdOrNull(UUID.fromString(userId))?.let { u: User ->
      maintainUserCheck.ensureUserLoggedInUserRelationship(admin, authorities, u)
      Hibernate.initialize(u.groups)
      u.groups.forEach { Hibernate.initialize(it.children) }
      u.groups
    }

  fun getAssignableGroups(username: String, authorities: Collection<GrantedAuthority>): List<Group> =
    if (canMaintainAuthUsers(authorities)) allGroups.toList()
    else getAuthUserGroups(username)?.sortedBy { it.groupName } ?: listOf()

  class AuthUserGroupExistsException : AuthUserGroupException("group", "exists")
  open class AuthUserGroupException(val field: String, val errorCode: String) :
    Exception("Add group failed for field $field with reason: $errorCode")

  open class AuthUserGroupManagerException(val action: String = "add", val field: String, val errorCode: String) :
    Exception("$action group failed for field $field with reason: $errorCode")

  open class AuthUserLastGroupException(val field: String, val errorCode: String) :
    Exception("remove group failed for field $field with reason: $errorCode")
}
