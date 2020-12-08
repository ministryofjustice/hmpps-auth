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
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.GroupRepository
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthGroupRelationshipException
import java.util.Optional

class GroupsServiceTest {
  private val groupRepository: GroupRepository = mock()
  private val maintainUserCheck: MaintainUserCheck = mock()
  private val groupsService = GroupsService(
    groupRepository,
    maintainUserCheck,
  )

  @Test
  fun `get group details`() {
    val dbGroup = Optional.of(Group("bob", "disc"))
    whenever(groupRepository.findByGroupCode(anyString())).thenReturn(dbGroup)

    val group = groupsService.getGroupDetail("bob", "Joe", SUPER_USER)

    assertThat(group).isEqualTo(dbGroup.get())
    verify(groupRepository).findByGroupCode("bob")
    verify(maintainUserCheck).ensureMaintainerGroupRelationship("Joe", "bob", SUPER_USER)
  }

  @Test
  fun `get group details throws error when user does not have group to be able to maintain`() {
    val dbGroup = Optional.of(Group("bob", "disc"))
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
