package uk.gov.justice.digital.hmpps.oauth2server.maintain

import com.microsoft.applicationinsights.TelemetryClient
import org.hibernate.Hibernate
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ChildGroup
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserFilter
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.ChildGroupRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.GroupRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.resource.api.CreateChildGroup
import uk.gov.justice.digital.hmpps.oauth2server.resource.api.GroupAmendment
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck

@Service
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
class GroupsService(
  private val groupRepository: GroupRepository,
  private val childGroupRepository: ChildGroupRepository,
  private val userRepository: UserRepository,
  private val maintainUserCheck: MaintainUserCheck,
  private val authUserGroupService: AuthUserGroupService,
  private val telemetryClient: TelemetryClient,
) {

  @Throws(GroupNotFoundException::class)
  fun getGroupDetail(groupCode: String, maintainerName: String, authorities: Collection<GrantedAuthority>): Group {
    val requestedGroup = groupRepository.findByGroupCode(groupCode) ?: throw
    GroupNotFoundException("get", groupCode, "notfound")

    Hibernate.initialize(requestedGroup.assignableRoles)
    Hibernate.initialize(requestedGroup.children)
    maintainUserCheck.ensureMaintainerGroupRelationship(maintainerName, groupCode, authorities)
    return requestedGroup
  }

  @Throws(ChildGroupNotFoundException::class)
  fun getChildGroupDetail(
    groupCode: String,
    maintainerName: String,
    authorities: Collection<GrantedAuthority>
  ): ChildGroup {
    return childGroupRepository.findByGroupCode(groupCode) ?: throw
    GroupNotFoundException("get", groupCode, "notfound")
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(GroupNotFoundException::class)
  fun updateGroup(username: String, groupCode: String, groupAmendment: GroupAmendment) {
    val groupToUpdate = groupRepository.findByGroupCode(groupCode) ?: throw
    GroupNotFoundException("maintain", groupCode, "notfound")

    groupToUpdate.groupName = groupAmendment.groupName
    groupRepository.save(groupToUpdate)

    telemetryClient.trackEvent(
      "GroupUpdateSuccess",
      mapOf("username" to username, "groupCode" to groupCode, "newGroupName" to groupAmendment.groupName),
      null
    )
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(GroupNotFoundException::class, GroupHasChildGroupException::class)
  fun deleteGroup(username: String, groupCode: String, authorities: Collection<GrantedAuthority>) {
    val group = groupRepository.findByGroupCode(groupCode) ?: throw
    GroupNotFoundException("delete", groupCode, "notfound")

    when {
      group.children.isEmpty() -> {
        removeUsersFromGroup(groupCode, username, authorities)
        groupRepository.delete(group)

        telemetryClient.trackEvent(
          "GroupDeleteSuccess",
          mapOf("username" to username, "groupCode" to groupCode),
          null
        )
      }
      else -> {
        throw GroupHasChildGroupException(groupCode, "child group exist")
      }
    }
  }

  private fun removeUsersFromGroup(groupCode: String, username: String, authorities: Collection<GrantedAuthority>) {
    val usersWithGroup = userRepository.findAll(UserFilter(groupCodes = listOf(groupCode)))
    usersWithGroup.forEach { authUserGroupService.removeGroup(it.username, groupCode, username, authorities) }
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(ChildGroupNotFoundException::class)
  fun updateChildGroup(username: String, groupCode: String, groupAmendment: GroupAmendment) {
    val groupToUpdate = childGroupRepository.findByGroupCode(groupCode) ?: throw
    GroupNotFoundException("maintain", groupCode, "notfound")

    groupToUpdate.groupName = groupAmendment.groupName
    childGroupRepository.save(groupToUpdate)

    telemetryClient.trackEvent(
      "GroupChildUpdateSuccess",
      mapOf("username" to username, "childGroupCode" to groupCode, "newChildGroupName" to groupAmendment.groupName),
      null
    )
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(ChildGroupExistsException::class, GroupNotFoundException::class)
  fun createChildGroup(username: String, createChildGroup: CreateChildGroup) {
    val childGroupFromDB = childGroupRepository.findByGroupCode(createChildGroup.groupCode)
    if (childGroupFromDB != null) {
      throw ChildGroupExistsException(createChildGroup.groupCode, "group code already exists")
    }
    val parentGroupDetails = groupRepository.findByGroupCode(createChildGroup.parentGroupCode) ?: throw
    GroupNotFoundException("create", createChildGroup.parentGroupCode, "ParentGroupNotFound")

    val child = ChildGroup(groupCode = createChildGroup.groupCode, groupName = createChildGroup.groupName)
    child.group = parentGroupDetails

    childGroupRepository.save(child)

    telemetryClient.trackEvent(
      "GroupChildCreateSuccess",
      mapOf(
        "username" to username,
        "groupCode" to parentGroupDetails.groupCode,
        "childGroupCode" to createChildGroup.groupCode,
        "childGroupName" to createChildGroup.groupName
      ),
      null
    )
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(ChildGroupNotFoundException::class)
  fun deleteChildGroup(username: String, groupCode: String) {
    childGroupRepository.deleteByGroupCode(groupCode)

    telemetryClient.trackEvent(
      "GroupChildDeleteSuccess",
      mapOf("username" to username, "childGroupCode" to groupCode),
      null
    )
  }

  class GroupNotFoundException(val action: String, val group: String, val errorCode: String) :
    Exception("Unable to $action group: $group with reason: $errorCode")

  class GroupHasChildGroupException(val group: String, val errorCode: String) :
    Exception("Unable to delete group: $group with reason: $errorCode")

  class ChildGroupNotFoundException(val group: String, val errorCode: String) :
    Exception("Unable to maintain child group: $group with reason: $errorCode")

  class ChildGroupExistsException(val group: String, val errorCode: String) :
    Exception("Unable to create child group: $group with reason: $errorCode")
}
