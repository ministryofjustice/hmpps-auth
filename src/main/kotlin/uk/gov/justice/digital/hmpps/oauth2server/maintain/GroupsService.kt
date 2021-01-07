package uk.gov.justice.digital.hmpps.oauth2server.maintain

import org.hibernate.Hibernate
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ChildGroup
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.ChildGroupRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.GroupRepository
import uk.gov.justice.digital.hmpps.oauth2server.resource.api.CreateChildGroup
import uk.gov.justice.digital.hmpps.oauth2server.resource.api.GroupAmendment
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck

@Service
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
class GroupsService(
  private val groupRepository: GroupRepository,
  private val childGroupRepository: ChildGroupRepository,
  private val maintainUserCheck: MaintainUserCheck,
) {

  @Throws(GroupNotFoundException::class)
  fun getGroupDetail(groupCode: String, maintainerName: String, authorities: Collection<GrantedAuthority>): Group {
    val requestedGroup = groupRepository.findByGroupCode(groupCode) ?: throw
    GroupNotFoundException(groupCode, "notfound")

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
    GroupNotFoundException(groupCode, "notfound")
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(GroupNotFoundException::class)
  fun updateGroup(groupCode: String, groupAmendment: GroupAmendment) {
    val groupToUpdate = groupRepository.findByGroupCode(groupCode) ?: throw
    GroupNotFoundException(groupCode, "notfound")

    groupToUpdate.groupName = groupAmendment.groupName
    groupRepository.save(groupToUpdate)
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(ChildGroupNotFoundException::class)
  fun updateChildGroup(groupCode: String, groupAmendment: GroupAmendment) {
    val groupToUpdate = childGroupRepository.findByGroupCode(groupCode) ?: throw
    GroupNotFoundException(groupCode, "notfound")

    groupToUpdate.groupName = groupAmendment.groupName
    childGroupRepository.save(groupToUpdate)
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(ChildGroupExistsException::class, GroupNotFoundException::class)
  fun createChildGroup(createChildGroup: CreateChildGroup) {
    val childGroupFromDB = childGroupRepository.findByGroupCode(createChildGroup.groupCode)
    if (childGroupFromDB != null) {
      throw ChildGroupExistsException(createChildGroup.groupCode, "group code already exists")
    }
    val parentGroupDetails = groupRepository.findByGroupCode(createChildGroup.parentGroupCode) ?: throw
    GroupNotFoundException(createChildGroup.parentGroupCode, "ParentGroupNotFound")

    val child = ChildGroup(groupCode = createChildGroup.groupCode, groupName = createChildGroup.groupName)
    child.group = parentGroupDetails

    childGroupRepository.save(child)
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(ChildGroupNotFoundException::class)
  fun deleteChildGroup(groupCode: String) {
    childGroupRepository.deleteByGroupCode(groupCode)
  }

  class GroupNotFoundException(val group: String, val errorCode: String) :
    Exception("Unable to maintain group: $group with reason: $errorCode")

  class ChildGroupNotFoundException(val group: String, val errorCode: String) :
    Exception("Unable to maintain child group: $group with reason: $errorCode")

  class ChildGroupExistsException(val group: String, val errorCode: String) :
    Exception("Unable to create child group: $group with reason: $errorCode")
}
