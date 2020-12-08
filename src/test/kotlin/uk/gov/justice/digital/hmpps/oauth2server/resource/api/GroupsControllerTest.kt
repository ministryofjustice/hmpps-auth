package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ChildGroup
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.GroupAssignableRole
import uk.gov.justice.digital.hmpps.oauth2server.maintain.GroupsService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.GroupsService.GroupNotFoundException
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserGroup
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserRole
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl

class GroupsControllerTest {
  private val groupsService: GroupsService = mock()
  private val authentication =
    TestingAuthenticationToken(
      UserDetailsImpl("user", "name", GROUP_MANAGER, AuthSource.auth.name, "userid", "jwtId"),
      "pass",
      "ROLE_AUTH_GROUP_MANAGER"
    )
  private val groupsController = GroupsController(groupsService)

  @Test
  fun `get group details`() {
    val authority = Authority(roleCode = "RO1", roleName = "Role1")
    val group1 = Group(groupCode = "FRED", groupName = "desc")
    group1.assignableRoles.add(GroupAssignableRole(role = authority, group = group1, automatic = false))
    group1.children.add(ChildGroup(groupCode = "BOB", groupName = "desc"))

    whenever(
      groupsService.getGroupDetail(
        groupCode = anyString(),
        maintainerName = anyString(),
        authorities = any()
      )
    ).thenReturn(group1)

    val groupDetails = groupsController.getGroupDetail("group1", authentication)
    assertThat(groupDetails).isEqualTo(
      GroupDetails(
        groupCode = "FRED",
        groupName = "desc",
        assignableRoles = listOf(AuthUserRole(roleCode = "RO1", roleName = "Role1")),
        children = listOf(AuthUserGroup(groupCode = "BOB", groupName = "desc"))
      )
    )
  }

  @Test
  fun `Group Not Found`() {

    doThrow(GroupNotFoundException("NotGroup", "not found")).whenever(groupsService).getGroupDetail(
      anyString(),
      any(),
      any()
    )

    assertThatThrownBy { groupsController.getGroupDetail("NotGroup", authentication) }
      .isInstanceOf(GroupNotFoundException::class.java)
      .withFailMessage("Unable to maintain group: NotGroup with reason: not found")
  }

  companion object {
    private val SUPER_USER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
    private val GROUP_MANAGER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"))
  }
}
