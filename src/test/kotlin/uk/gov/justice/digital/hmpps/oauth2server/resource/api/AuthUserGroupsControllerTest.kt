package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ChildGroup
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService.AuthUserGroupException
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService.AuthUserGroupExistsException
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService.AuthUserGroupManagerException
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserGroup
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import java.security.Principal
import java.util.Optional

class AuthUserGroupsControllerTest {
  private val principal: Principal = UsernamePasswordAuthenticationToken("bob", "pass")
  private val authUserService: AuthUserService = mock()
  private val authUserGroupService: AuthUserGroupService = mock()
  private val authUserGroupsController = AuthUserGroupsController(authUserService, authUserGroupService)
  private val authenticationSuperUser =
    TestingAuthenticationToken(
      UserDetailsImpl("bob", "name", SUPER_USER, AuthSource.auth.name, "userid", "jwtId"),
      "pass",
      "ROLE_MAINTAIN_OAUTH_USERS"
    )
  private val authenticationGroupManager =
    TestingAuthenticationToken(
      UserDetailsImpl("JOHN", "name", GROUP_MANAGER, AuthSource.auth.name, "userid", "jwtId"),
      "pass",
      "ROLE_AUTH_GROUP_MANAGER"
    )

  @Nested
  inner class Groups {
    @Test
    fun `groups userNotFound`() {
      whenever(authUserGroupService.getAuthUserGroups(anyString())).thenReturn(null)
      assertThatThrownBy { authUserGroupsController.groups("bob") }
        .isInstanceOf(UsernameNotFoundException::class.java)
    }

    @Test
    fun `groups no children`() {
      val group1 = Group("FRED", "desc")
      val group2 = Group("GLOBAL_SEARCH", "desc2")
      whenever(authUserGroupService.getAuthUserGroups(anyString())).thenReturn(setOf(group1, group2))
      val responseEntity = authUserGroupsController.groups(username = "joe", children = false)
      assertThat(responseEntity).containsOnly(AuthUserGroup(group1), AuthUserGroup(group2))
    }

    @Test
    fun `groups default children`() {
      val group1 = Group("FRED", "desc")
      val group2 = Group("GLOBAL_SEARCH", "desc2")
      val childGroup = ChildGroup("CHILD_1", "child 1")
      group2.children.add(childGroup)
      whenever(authUserGroupService.getAuthUserGroups(anyString())).thenReturn(setOf(group1, group2))
      val responseEntity = authUserGroupsController.groups(username = "joe")
      assertThat(responseEntity).containsOnly(AuthUserGroup("FRED", "desc"), AuthUserGroup("CHILD_1", "child 1"))
    }

    @Test
    fun `groups with children requested`() {
      val group1 = Group("FRED", "desc")
      val group2 = Group("GLOBAL_SEARCH", "desc2")
      val childGroup = ChildGroup("CHILD_1", "child 1")
      group2.children.add(childGroup)
      whenever(authUserGroupService.getAuthUserGroups(anyString())).thenReturn(setOf(group1, group2))
      val responseEntity = authUserGroupsController.groups(username = "joe")
      assertThat(responseEntity).containsOnly(AuthUserGroup("FRED", "desc"), AuthUserGroup("CHILD_1", "child 1"))
    }
  }

  @Nested
  inner class GroupsByUserId {
    @Test
    fun `groups userNotFound`() {
      whenever(authUserGroupService.getAuthUserGroupsByUserId(anyString())).thenReturn(null)
      assertThatThrownBy { authUserGroupsController.groupsByUserId(userId = "00000000-aaaa-0000-aaaa-0a0a0a0a0a0a") }
        .isInstanceOf(UsernameNotFoundException::class.java)
    }

    @Test
    fun `groups no children`() {
      val group1 = Group("FRED", "desc")
      val group2 = Group("GLOBAL_SEARCH", "desc2")
      whenever(authUserGroupService.getAuthUserGroupsByUserId(anyString())).thenReturn(setOf(group1, group2))
      val responseEntity =
        authUserGroupsController.groupsByUserId(userId = "00000000-aaaa-0000-aaaa-0a0a0a0a0a0a", children = false)
      assertThat(responseEntity).containsOnly(AuthUserGroup(group1), AuthUserGroup(group2))
    }

    @Test
    fun `groups default children`() {
      val group1 = Group("FRED", "desc")
      val group2 = Group("GLOBAL_SEARCH", "desc2")
      val childGroup = ChildGroup("CHILD_1", "child 1")
      group2.children.add(childGroup)
      whenever(authUserGroupService.getAuthUserGroupsByUserId(anyString())).thenReturn(setOf(group1, group2))
      val responseEntity = authUserGroupsController.groupsByUserId(userId = "00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
      assertThat(responseEntity).containsOnly(AuthUserGroup("FRED", "desc"), AuthUserGroup("CHILD_1", "child 1"))
    }

    @Test
    fun `groups with children requested`() {
      val group1 = Group("FRED", "desc")
      val group2 = Group("GLOBAL_SEARCH", "desc2")
      val childGroup = ChildGroup("CHILD_1", "child 1")
      group2.children.add(childGroup)
      whenever(authUserGroupService.getAuthUserGroupsByUserId(anyString())).thenReturn(setOf(group1, group2))
      val responseEntity = authUserGroupsController.groupsByUserId(userId = "00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")
      assertThat(responseEntity).containsOnly(AuthUserGroup("FRED", "desc"), AuthUserGroup("CHILD_1", "child 1"))
    }
  }

  @Test
  fun addGroup_userNotFound() {
    val responseEntity = authUserGroupsController.addGroup("bob", "group", authenticationSuperUser)
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
    assertThat(responseEntity.body).isEqualTo(
      ErrorDetail(
        "Not Found",
        "Account for username bob not found",
        "username"
      )
    )
  }

  @Test
  fun addGroup_userNotFound_groupManager() {
    val responseEntity = authUserGroupsController.addGroup("bob", "group", authenticationGroupManager)
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
    assertThat(responseEntity.body).isEqualTo(
      ErrorDetail(
        "Not Found",
        "Account for username bob not found",
        "username"
      )
    )
  }

  @Test
  fun addGroup_success() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    val responseEntity = authUserGroupsController.addGroup("someuser", "group", authenticationSuperUser)
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    verify(authUserGroupService).addGroup("USER", "group", "bob", authenticationSuperUser.authorities)
  }

  @Test
  fun addGroup_success_groupManager() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    val responseEntity = authUserGroupsController.addGroup("someuser", "group", authenticationGroupManager)
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    verify(authUserGroupService).addGroup("USER", "group", "JOHN", authenticationGroupManager.authorities)
  }

  @Test
  fun addGroup_conflict() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    doThrow(AuthUserGroupExistsException()).whenever(authUserGroupService)
      .addGroup(anyString(), anyString(), anyString(), any())
    val responseEntity = authUserGroupsController.addGroup("someuser", "joe", authenticationSuperUser)
    assertThat(responseEntity.statusCodeValue).isEqualTo(409)
  }

  @Test
  fun addGroup_conflict_groupManager() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    doThrow(AuthUserGroupExistsException()).whenever(authUserGroupService)
      .addGroup(anyString(), anyString(), anyString(), any())
    val responseEntity = authUserGroupsController.addGroup("someuser", "joe", authenticationGroupManager)
    assertThat(responseEntity.statusCodeValue).isEqualTo(409)
  }

  @Test
  fun addGroup_validation() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    doThrow(AuthUserGroupException("group", "error")).whenever(authUserGroupService)
      .addGroup(anyString(), anyString(), anyString(), any())
    val responseEntity = authUserGroupsController.addGroup("someuser", "harry", authenticationSuperUser)
    assertThat(responseEntity.statusCodeValue).isEqualTo(400)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("group.error", "group failed validation", "group"))
  }

  @Test
  fun addGroup_validation_groupManager() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    doThrow(AuthUserGroupException("group", "error")).whenever(authUserGroupService)
      .addGroup(anyString(), anyString(), anyString(), any())
    val responseEntity = authUserGroupsController.addGroup("someuser", "harry", authenticationGroupManager)
    assertThat(responseEntity.statusCodeValue).isEqualTo(400)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("group.error", "group failed validation", "group"))
  }

  @Test
  fun addGroup_notInGroup_groupManager() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    doThrow(AuthUserGroupManagerException("Add", "group", "managerNotMember")).whenever(authUserGroupService)
      .addGroup(anyString(), anyString(), anyString(), any())
    val responseEntity = authUserGroupsController.addGroup("someuser", "John", authenticationGroupManager)
    assertThat(responseEntity.statusCodeValue).isEqualTo(400)
    assertThat(responseEntity.body).isEqualTo(
      ErrorDetail(
        "group.managerNotMember",
        "Group Manager is not a member of group",
        "group"
      )
    )
  }

  @Test
  fun addGroup_groupManagerNotAllowedToMaintainUser() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    doThrow(AuthUserGroupRelationshipException("someuser", "User not with your groups")).whenever(authUserGroupService)
      .addGroup(anyString(), anyString(), anyString(), any())
    val responseEntity = authUserGroupsController.addGroup("someuser", "joe", authenticationGroupManager)
    assertThat(responseEntity.statusCodeValue).isEqualTo(403)
  }

  @Test
  fun removeGroup_userNotFound() {
    val responseEntity = authUserGroupsController.removeGroup("bob", "group", authenticationSuperUser)
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
    assertThat(responseEntity.body).isEqualTo(
      ErrorDetail(
        "Not Found",
        "Account for username bob not found",
        "username"
      )
    )
  }

  @Test
  fun removeGroup_userNotFound_groupManager() {
    val responseEntity = authUserGroupsController.removeGroup("bob", "group", authenticationGroupManager)
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
    assertThat(responseEntity.body).isEqualTo(
      ErrorDetail(
        "Not Found",
        "Account for username bob not found",
        "username"
      )
    )
  }

  @Test
  fun removeGroup_success() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    val responseEntity = authUserGroupsController.removeGroup("someuser", "joe", authenticationSuperUser)
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    verify(authUserGroupService).removeGroup("USER", "joe", "bob", authenticationSuperUser.authorities)
  }

  @Test
  fun removeGroup_success_groupManager() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    val responseEntity = authUserGroupsController.removeGroup("someuser", "joe", authenticationGroupManager)
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    verify(authUserGroupService).removeGroup("USER", "joe", "JOHN", authenticationGroupManager.authorities)
  }

  @Test
  fun removeGroup_groupMissing() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    doThrow(AuthUserGroupException("group", "error")).whenever(authUserGroupService)
      .removeGroup(anyString(), anyString(), anyString(), any())
    val responseEntity = authUserGroupsController.removeGroup("someuser", "harry", authenticationSuperUser)
    assertThat(responseEntity.statusCodeValue).isEqualTo(400)
  }

  @Test
  fun removeGroup_groupMissing_groupManager() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    doThrow(AuthUserGroupException("group", "error")).whenever(authUserGroupService)
      .removeGroup(anyString(), anyString(), anyString(), any())
    val responseEntity = authUserGroupsController.removeGroup("someuser", "harry", authenticationGroupManager)
    assertThat(responseEntity.statusCodeValue).isEqualTo(400)
  }

  @Test
  fun removeGroup_notInGroup_groupManager() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    doThrow(AuthUserGroupManagerException("delete", "group", "managerNotMember")).whenever(authUserGroupService)
      .removeGroup(anyString(), anyString(), anyString(), any())
    val responseEntity = authUserGroupsController.removeGroup("someuser", "FRED", authenticationGroupManager)
    assertThat(responseEntity.statusCodeValue).isEqualTo(400)
    assertThat(responseEntity.body).isEqualTo(
      ErrorDetail(
        "group.managerNotMember",
        "Group Manager is not a member of group: FRED",
        "group"
      )
    )
  }

  private val authUser: User
    get() {
      return createSampleUser(
        username = "USER",
        email = "email",
        verified = true,
        groups = setOf(Group("GLOBAL_SEARCH", "desc2"), Group("FRED", "desc"))
      )
    }

  companion object {
    private val SUPER_USER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
    private val GROUP_MANAGER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"))
  }
}
