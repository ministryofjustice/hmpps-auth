package uk.gov.justice.digital.hmpps.oauth2server.maintain

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ChildGroup
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.ChildGroupRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.GroupRepository
import uk.gov.justice.digital.hmpps.oauth2server.resource.api.CreateChildGroup
import uk.gov.justice.digital.hmpps.oauth2server.resource.api.GroupAmendment
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthGroupRelationshipException

class GroupsServiceTest {
  private val groupRepository: GroupRepository = mock()
  private val childGroupRepository: ChildGroupRepository = mock()
  private val maintainUserCheck: MaintainUserCheck = mock()
  private val groupsService = GroupsService(
    groupRepository,
    childGroupRepository,
    maintainUserCheck,
  )

  @Test
  fun `get group details`() {
    val dbGroup = Group("bob", "disc")
    whenever(groupRepository.findByGroupCode(anyString())).thenReturn(dbGroup)

    val group = groupsService.getGroupDetail("bob", "Joe", SUPER_USER)

    assertThat(group).isEqualTo(dbGroup)
    verify(groupRepository).findByGroupCode("bob")
    verify(maintainUserCheck).ensureMaintainerGroupRelationship("Joe", "bob", SUPER_USER)
  }

  @Test
  fun `update group details`() {
    val dbGroup = Group("bob", "disc")
    val groupAmendment = GroupAmendment("Joe")
    whenever(groupRepository.findByGroupCode(anyString())).thenReturn(dbGroup)

    groupsService.updateGroup("bob", groupAmendment)

    verify(groupRepository).findByGroupCode("bob")
    verify(groupRepository).save(dbGroup)
  }

  @Test
  fun `Create child group`() {
    val parentGroup = Group("PG", "parent group")
    val createChildGroup = CreateChildGroup("PG", "CG", "Child Group")
    whenever(childGroupRepository.findByGroupCode(anyString())).thenReturn(null)
    whenever(groupRepository.findByGroupCode(anyString())).thenReturn(parentGroup)

    groupsService.createChildGroup(createChildGroup)
    val cg = ChildGroup("CG", "Child Group")
    verify(childGroupRepository).findByGroupCode("CG")
    verify(groupRepository).findByGroupCode("PG")
    verify(childGroupRepository).save(cg)
  }

  @Test
  fun `Delete child group`() {
    groupsService.deleteChildGroup("CG")
    verify(childGroupRepository).deleteByGroupCode("CG")
  }

  @Test
  fun `get child group details`() {
    val dbGroup = ChildGroup("bob", "disc")
    whenever(childGroupRepository.findByGroupCode(anyString())).thenReturn(dbGroup)

    val group = groupsService.getChildGroupDetail("bob", "Joe", SUPER_USER)

    assertThat(group).isEqualTo(dbGroup)
    verify(childGroupRepository).findByGroupCode("bob")
  }

  @Test
  fun `update child group details`() {
    val dbGroup = ChildGroup("bob", "disc")
    val groupAmendment = GroupAmendment("Joe")
    whenever(childGroupRepository.findByGroupCode(anyString())).thenReturn(dbGroup)

    groupsService.updateChildGroup("bob", groupAmendment)

    verify(childGroupRepository).findByGroupCode("bob")
    verify(childGroupRepository).save(dbGroup)
  }

  @Test
  fun `get group details throws error when user does not have group to be able to maintain`() {
    val dbGroup = Group("bob", "disc")
    whenever(groupRepository.findByGroupCode(anyString())).thenReturn(dbGroup)
    doThrow(AuthGroupRelationshipException("bob", "Group not with your groups")).whenever(maintainUserCheck)
      .ensureMaintainerGroupRelationship(
        anyString(),
        anyString(),
        any()
      )

    assertThatThrownBy { groupsService.getGroupDetail("bob", "Joe", GROUP_MANAGER_ROLE) }.isInstanceOf(
      AuthGroupRelationshipException::class.java
    ).hasMessage("Unable to maintain group: bob with reason: Group not with your groups")
  }

  companion object {
    private val SUPER_USER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
    private val GROUP_MANAGER_ROLE: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"))
  }
}
