package uk.gov.justice.digital.hmpps.oauth2server.maintain

import com.google.common.collect.Sets
import com.microsoft.applicationinsights.TelemetryClient
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.RoleRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.Companion.canMaintainAuthUsers
import java.util.UUID
import java.util.function.Consumer

@Service

@Transactional(transactionManager = "authTransactionManager", readOnly = true)
class AuthUserRoleService(
  private val userRepository: UserRepository,
  private val roleRepository: RoleRepository,
  private val telemetryClient: TelemetryClient,
  private val maintainUserCheck: MaintainUserCheck
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    private fun canAddAuthClients(authorities: Collection<GrantedAuthority>) = authorities.map { it.authority }
      .any { "ROLE_OAUTH_ADMIN" == it }
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(
    AuthUserRoleException::class,
    AuthUserGroupRelationshipException::class
  )
  fun addRoles(
    username: String,
    roleCodes: List<String>,
    loggedInUser: String,
    authorities: Collection<GrantedAuthority>
  ) {
    // already checked that user exists
    val user = userRepository.findByUsernameAndMasterIsTrue(username).orElseThrow()
    maintainUserCheck.ensureUserLoggedInUserRelationship(loggedInUser, authorities, user)
    val formattedRoles = roleCodes.map { formatRole(it) }
    val allAssignableRoles = getAllAssignableRoles(username, authorities)
    for (roleCode in formattedRoles) {
      // check that role exists
      val role =
        roleRepository.findByRoleCode(roleCode).orElseThrow { AuthUserRoleException("role", "role.notfound") }
      if (user.authorities.contains(role)) {
        throw AuthUserRoleExistsException()
      }
      if (!allAssignableRoles.contains(role)) {
        throw AuthUserRoleException("role", "invalid")
      }
      user.authorities.add(role)
    }
    // now that roles have all been added, then audit the role additions
    formattedRoles.forEach(
      Consumer { roleCode: String ->
        telemetryClient.trackEvent(
          "AuthUserRoleAddSuccess",
          mapOf("username" to username, "role" to roleCode, "admin" to loggedInUser),
          null
        )
        log.info("Adding role {} to user {}", roleCode, username)
      }
    )
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(
    AuthUserRoleException::class,
    AuthUserGroupRelationshipException::class
  )
  fun addRolesByUserId(
    userId: String,
    roleCodes: List<String>,
    loggedInUser: String,
    authorities: Collection<GrantedAuthority>
  ) {
    // already checked that user exists
    userRepository.findByIdOrNull(UUID.fromString(userId))?.let { user ->
      maintainUserCheck.ensureUserLoggedInUserRelationship(loggedInUser, authorities, user)
      val formattedRoles = roleCodes.map { formatRole(it) }
      val allAssignableRoles = getAllAssignableRolesByUserId(userId, authorities)
      for (roleCode in formattedRoles) {
        // check that role exists
        val role =
          roleRepository.findByRoleCode(roleCode).orElseThrow { AuthUserRoleException("role", "role.notfound") }
        if (user.authorities.contains(role)) {
          throw AuthUserRoleExistsException()
        }
        if (!allAssignableRoles.contains(role)) {
          throw AuthUserRoleException("role", "invalid")
        }
        user.authorities.add(role)
      }
      // now that roles have all been added, then audit the role additions
      formattedRoles.forEach(
        Consumer { roleCode: String ->
          telemetryClient.trackEvent(
            "AuthUserRoleAddSuccess",
            mapOf("userId" to userId, "role" to roleCode, "admin" to loggedInUser),
            null
          )
          log.info("Adding role {} to user {}", roleCode, userId)
        }
      )
    } ?: throw UsernameNotFoundException("User $userId not found")
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(
    AuthUserRoleException::class,
    AuthUserGroupRelationshipException::class
  )
  fun removeRole(username: String, roleCode: String, loggedInUser: String, authorities: Collection<GrantedAuthority>) {
    // already checked that user exists
    val user = userRepository.findByUsernameAndMasterIsTrue(username).orElseThrow()

    // check that the logged in user has permission to modify user
    maintainUserCheck.ensureUserLoggedInUserRelationship(loggedInUser, authorities, user)
    val roleFormatted = formatRole(roleCode)
    val role =
      roleRepository.findByRoleCode(roleFormatted).orElseThrow { AuthUserRoleException("role", "role.notfound") }
    if (!user.authorities.contains(role)) {
      throw AuthUserRoleException("role", "role.missing")
    }
    if (!getAllAssignableRoles(username, authorities).contains(role)) {
      throw AuthUserRoleException("role", "invalid")
    }
    log.info("Removing role {} from user {}", roleFormatted, username)
    user.authorities.removeIf { role == it }
    telemetryClient.trackEvent(
      "AuthUserRoleRemoveSuccess",
      mapOf("username" to username, "role" to roleFormatted, "admin" to loggedInUser),
      null
    )
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(
    AuthUserRoleException::class,
    AuthUserGroupRelationshipException::class
  )
  fun removeRoleByUserId(
    userId: String,
    roleCode: String,
    loggedInUser: String,
    authorities: Collection<GrantedAuthority>
  ) {
    // already checked that user exists
    // check that the logged in user has permission to modify user
    userRepository.findByIdOrNull(UUID.fromString(userId))?.let { user ->
      maintainUserCheck.ensureUserLoggedInUserRelationship(loggedInUser, authorities, user)
      val roleFormatted = formatRole(roleCode)
      val role =
        roleRepository.findByRoleCode(roleFormatted).orElseThrow { AuthUserRoleException("role", "role.notfound") }
      if (!user.authorities.contains(role)) {
        throw AuthUserRoleException("role", "role.missing")
      }
      if (!getAllAssignableRolesByUserId(userId, authorities).contains(role)) {
        throw AuthUserRoleException("role", "invalid")
      }
      log.info("Removing role {} from userId {}", roleFormatted, userId)
      user.authorities.removeIf { role == it }
      telemetryClient.trackEvent(
        "AuthUserRoleRemoveSuccess",
        mapOf("userId" to userId, "role" to roleFormatted, "admin" to loggedInUser),
        null
      )
    } ?: throw UsernameNotFoundException("User $userId not found")
  }

  private fun formatRole(role: String) =
    Authority.removeRolePrefixIfNecessary(StringUtils.upperCase(StringUtils.trim(role)))

  val allRoles: List<Authority>
    get() = roleRepository.findAllByOrderByRoleName()

  fun getAllAssignableRoles(username: String, authorities: Collection<GrantedAuthority>) =
    if (canMaintainAuthUsers(authorities)) {
      // only allow oauth admins to see that role
      allRoles.filter { r: Authority -> "OAUTH_ADMIN" != r.roleCode || canAddAuthClients(authorities) }.toSet()
      // otherwise they can assign all roles that can be assigned to any of their groups
    } else roleRepository.findByGroupAssignableRolesForUsername(username)

  fun getAllAssignableRolesByUserId(userId: String, authorities: Collection<GrantedAuthority>) =
    if (canMaintainAuthUsers(authorities)) {
      // only allow oauth admins to see that role
      allRoles.filter { r: Authority -> "OAUTH_ADMIN" != r.roleCode || canAddAuthClients(authorities) }.toSet()
      // otherwise they can assign all roles that can be assigned to any of their groups
    } else roleRepository.findByGroupAssignableRolesForUserId(UUID.fromString(userId))

  fun getAssignableRoles(username: String, authorities: Collection<GrantedAuthority>): List<Authority> {
    val user = userRepository.findByUsernameAndMasterIsTrue(username.uppercase()).orElseThrow()
    val userRoles = user.authorities
    val allAssignableRoles = getAllAssignableRoles(username, authorities)
    return Sets.difference(allAssignableRoles, userRoles)
      .sortedBy { it.roleName }
  }

  fun getAssignableRolesByUserId(userId: String, authorities: Collection<GrantedAuthority>): List<Authority> =
    userRepository.findByIdOrNull(UUID.fromString(userId))?.let {
      Sets.difference(
        getAllAssignableRolesByUserId
        (userId, authorities),
        it.authorities
      )
        .sortedBy { it.roleName }
    }!!

  class AuthUserRoleExistsException : AuthUserRoleException("role", "role.exists")
  open class AuthUserRoleException(val field: String, val errorCode: String) :
    Exception("Modify role failed for field $field with reason: $errorCode")
}
