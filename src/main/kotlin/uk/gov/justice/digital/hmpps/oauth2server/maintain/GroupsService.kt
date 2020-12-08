package uk.gov.justice.digital.hmpps.oauth2server.maintain

import org.hibernate.Hibernate
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.GroupRepository
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck

@Service
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
class GroupsService(
  private val groupRepository: GroupRepository,
  private val maintainUserCheck: MaintainUserCheck,
) {

  @Throws(GroupNotFoundException::class)
  fun getGroupDetail(groupCode: String, maintainerName: String, authorities: Collection<GrantedAuthority>): Group {
    val requestedGroup = groupRepository.findByGroupCode(groupCode).orElseThrow {
      GroupNotFoundException(groupCode, "notfound")
    }
    Hibernate.initialize(requestedGroup.assignableRoles)
    Hibernate.initialize(requestedGroup.children)
    maintainUserCheck.ensureMaintainerGroupRelationship(maintainerName, groupCode, authorities)
    return requestedGroup
  }

  class GroupNotFoundException(val group: String, val errorCode: String) :
    Exception("Unable to maintain group: $group with reason: $errorCode")
}
