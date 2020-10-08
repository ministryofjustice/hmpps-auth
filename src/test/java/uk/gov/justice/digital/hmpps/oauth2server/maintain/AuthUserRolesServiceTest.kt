package uk.gov.justice.digital.hmpps.oauth2server.maintain

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.RoleRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService.AuthUserRoleException
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException
import java.util.HashSet
import java.util.Optional

internal class AuthUserRolesServiceTest {
  private val userRepository: UserRepository = mock()
  private val roleRepository: RoleRepository = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val maintainUserCheck: MaintainUserCheck = mock()
  private val service = AuthUserRoleService(userRepository, roleRepository, telemetryClient, maintainUserCheck)

  @Nested
  inner class AddRoles {
    @Test
    fun addRoles_notfound() {
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(User.of("user")))
      assertThatThrownBy { service.addRoles("user", listOf("        "), "admin", SUPER_USER) }.isInstanceOf(
        AuthUserRoleException::class.java
      ).hasMessage("Modify role failed for field role with reason: role.notfound")
    }

    @Test
    fun addRoles_invalidRole() {
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(User.of("user")))
      val role = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role))
      whenever(roleRepository.findAllByOrderByRoleName()).thenReturn(listOf(Authority("FRED", "Role Fred")))
      assertThatThrownBy { service.addRoles("user", listOf("BOB"), "admin", SUPER_USER) }.isInstanceOf(
        AuthUserRoleException::class.java
      ).hasMessage("Modify role failed for field role with reason: invalid")
    }

    @Test
    fun addRoles_noaccess() {
      val user = User.of("user")
      user.groups = setOf(Group("group", "desc"))
      val role = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      val role2 = Authority("BOB", "Bloggs")
      user.authorities = HashSet(listOf(role, role2))
      val groupManager = User.of("groupManager")
      groupManager.groups = setOf(Group("group2", "desc"))
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString()))
        .thenReturn(Optional.of(user))
        .thenReturn(Optional.of(groupManager))
      doThrow(AuthUserGroupRelationshipException("user", "User not with your groups")).whenever(maintainUserCheck)
        .ensureUserLoggedInUserRelationship(
          anyString(),
          ArgumentMatchers.anyCollection(),
          ArgumentMatchers.any(User::class.java)
        )
      assertThatThrownBy { service.addRoles("user", listOf("BOB"), "admin", GROUP_MANAGER) }.isInstanceOf(
        AuthUserGroupRelationshipException::class.java
      ).hasMessage("Unable to maintain user: user with reason: User not with your groups")
    }

    @Test
    fun addRoles_invalidRoleGroupManager() {
      val user = User.of("user")
      val group1 = Group("group", "desc")
      user.groups = setOf(group1)
      val groupManager = User.of("groupManager")
      groupManager.groups = setOf(group1)
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString()))
        .thenReturn(Optional.of(user))
        .thenReturn(Optional.of(groupManager))
      val role = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role))
      whenever(roleRepository.findByGroupAssignableRolesForUsername(anyString())).thenReturn(
        setOf(
          Authority(
            "FRED",
            "Role Fred"
          )
        )
      )
      assertThatThrownBy { service.addRoles("user", listOf("BOB"), "admin", GROUP_MANAGER) }.isInstanceOf(
        AuthUserRoleException::class.java
      ).hasMessage("Modify role failed for field role with reason: invalid")
    }

    @Test
    fun addRoles_oauthAdminRestricted() {
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(User.of("user")))
      val role = Authority("ROLE_OAUTH_ADMIN", "Role Licence Vary")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role))
      whenever(roleRepository.findAllByOrderByRoleName()).thenReturn(listOf(role))
      assertThatThrownBy { service.addRoles("user", listOf("BOB"), "admin", SUPER_USER) }.isInstanceOf(
        AuthUserRoleException::class.java
      ).hasMessage("Modify role failed for field role with reason: invalid")
    }

    @Test
    fun addRoles_oauthAdminRestricted_success() {
      val user = User.of("user")
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
      val role = Authority("ROLE_OAUTH_ADMIN", "Role Auth Admin")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role))
      whenever(roleRepository.findAllByOrderByRoleName()).thenReturn(listOf(role))
      service.addRoles(
        "user",
        listOf("BOB"),
        "admin",
        setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"), SimpleGrantedAuthority("ROLE_OAUTH_ADMIN"))
      )
      assertThat(user.authorities).extracting<String> { it.authority }.containsOnly("ROLE_OAUTH_ADMIN")
    }

    @Test
    fun addRoles_roleAlreadyOnUser() {
      val user = User.of("user")
      val role = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      user.authorities = HashSet(listOf(role))
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role))
      assertThatThrownBy { service.addRoles("user", listOf("LICENCE_VARY"), "admin", SUPER_USER) }.isInstanceOf(
        AuthUserRoleException::class.java
      ).hasMessage("Modify role failed for field role with reason: role.exists")
    }

    @Test
    fun addRoles_success() {
      val user = User.of("user")
      user.authorities = HashSet(listOf(Authority("JOE", "bloggs")))
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
      val role = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role))
      whenever(roleRepository.findAllByOrderByRoleName()).thenReturn(listOf(role))
      service.addRoles("user", listOf("ROLE_LICENCE_VARY"), "admin", SUPER_USER)
      assertThat(user.authorities).extracting<String> { it.authority }.containsOnly("ROLE_JOE", "ROLE_LICENCE_VARY")
    }

    @Test
    fun `addRoles success multiple roles`() {
      val user = User.of("user")
      user.authorities = HashSet(listOf(Authority("JOE", "bloggs")))
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
      val role1 = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      val role2 = Authority("ROLE_OTHER", "Role Other")
      whenever(roleRepository.findByRoleCode(anyString()))
        .thenReturn(Optional.of(role1))
        .thenReturn(Optional.of(role2))
      whenever(roleRepository.findAllByOrderByRoleName()).thenReturn(listOf(role1, role2))
      service.addRoles("user", listOf("ROLE_LICENCE_VARY", "ROLE_OTHER"), "admin", SUPER_USER)
      assertThat(user.authorities).extracting<String> { it.authority }
        .containsOnly("ROLE_JOE", "ROLE_LICENCE_VARY", "ROLE_OTHER")

      verify(telemetryClient).trackEvent(
        "AuthUserRoleAddSuccess",
        mapOf("username" to "user", "role" to "LICENCE_VARY", "admin" to "admin"),
        null
      )
      verify(telemetryClient).trackEvent(
        "AuthUserRoleAddSuccess",
        mapOf("username" to "user", "role" to "OTHER", "admin" to "admin"),
        null
      )
    }

    @Test
    fun addRoles_successGroupManager() {
      val user = User.of("user")
      val group1 = Group("group", "desc")
      user.groups = setOf(group1, Group("group2", "desc"))
      user.authorities = HashSet(listOf(Authority("JOE", "bloggs")))
      val groupManager = User.of("groupManager")
      groupManager.groups = setOf(Group("group3", "desc"), group1)
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString()))
        .thenReturn(Optional.of(user))
        .thenReturn(Optional.of(groupManager))
      val role = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role))
      whenever(roleRepository.findByGroupAssignableRolesForUsername(anyString())).thenReturn(setOf(role))
      service.addRoles("user", listOf("ROLE_LICENCE_VARY"), "admin", GROUP_MANAGER)
      assertThat(user.authorities).extracting<String> { it.authority }.containsOnly("ROLE_JOE", "ROLE_LICENCE_VARY")
    }
  }

  @Nested
  inner class RemoveRole {

    @Test
    fun removeRole_roleNotOnUser() {
      val user = User.of("user")
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
      val role2 = Authority("BOB", "Bloggs")
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role2))
      assertThatThrownBy {
        service.removeRole(
          "user",
          "BOB",
          "admin",
          SUPER_USER
        )
      }.isInstanceOf(AuthUserRoleException::class.java)
        .hasMessage("Modify role failed for field role with reason: role.missing")
    }

    @Test
    fun removeRole_invalid() {
      val user = User.of("user")
      val role = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      val role2 = Authority("BOB", "Bloggs")
      user.authorities = HashSet(listOf(role, role2))
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role2))
      whenever(roleRepository.findAllByOrderByRoleName()).thenReturn(listOf(role))
      assertThatThrownBy {
        service.removeRole(
          "user",
          "BOB",
          "admin",
          SUPER_USER
        )
      }.isInstanceOf(AuthUserRoleException::class.java)
        .hasMessage("Modify role failed for field role with reason: invalid")
    }

    @Test
    fun removeRole_noaccess() {
      val user = User.of("user")
      user.groups = setOf(Group("group", "desc"))
      val role = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      val role2 = Authority("BOB", "Bloggs")
      val groupManager = User.of("groupManager")
      groupManager.groups = setOf(Group("group2", "desc"))
      user.authorities = HashSet(listOf(role, role2))
      doThrow(AuthUserGroupRelationshipException("user", "User not with your groups")).whenever(maintainUserCheck)
        .ensureUserLoggedInUserRelationship(
          anyString(),
          ArgumentMatchers.anyCollection(),
          ArgumentMatchers.any(User::class.java)
        )
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString()))
        .thenReturn(Optional.of(user))
        .thenReturn(Optional.of(groupManager))
      assertThatThrownBy { service.removeRole("user", "BOB", "admin", GROUP_MANAGER) }.isInstanceOf(
        AuthUserGroupRelationshipException::class.java
      ).hasMessage("Unable to maintain user: user with reason: User not with your groups")
    }

    @Test
    fun removeRole_invalidGroupManager() {
      val user = User.of("user")
      val group1 = Group("group", "desc")
      user.groups = setOf(group1)
      val role = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      val role2 = Authority("BOB", "Bloggs")
      val groupManager = User.of("groupManager")
      groupManager.groups = setOf(group1)
      user.authorities = HashSet(listOf(role, role2))
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString()))
        .thenReturn(Optional.of(user))
        .thenReturn(Optional.of(groupManager))
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role2))
      whenever(roleRepository.findByGroupAssignableRolesForUsername(anyString())).thenReturn(setOf(role))
      assertThatThrownBy { service.removeRole("user", "BOB", "admin", GROUP_MANAGER) }.isInstanceOf(
        AuthUserRoleException::class.java
      ).hasMessage("Modify role failed for field role with reason: invalid")
    }

    @Test
    fun removeRole_notfound() {
      val user = User.of("user")
      val role = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      val role2 = Authority("BOB", "Bloggs")
      user.authorities = HashSet(listOf(role, role2))
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
      assertThatThrownBy {
        service.removeRole(
          "user",
          "BOB",
          "admin",
          SUPER_USER
        )
      }.isInstanceOf(AuthUserRoleException::class.java)
        .hasMessage("Modify role failed for field role with reason: role.notfound")
    }

    @Test
    fun removeRole_success() {
      val user = User.of("user")
      val group1 = Group("group", "desc")
      user.groups = setOf(group1)
      user.authorities = HashSet(listOf(Authority("JOE", "bloggs")))
      val groupManager = User.of("groupManager")
      groupManager.groups = setOf(group1)
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString()))
        .thenReturn(Optional.of(user))
        .thenReturn(Optional.of(groupManager))
      val role = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      val role2 = Authority("JOE", "Bloggs")
      user.authorities = HashSet(listOf(role, role2))
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role))
      whenever(roleRepository.findAllByOrderByRoleName()).thenReturn(listOf(role, role2))
      service.removeRole("user", "  licence_vary   ", "admin", SUPER_USER)
      assertThat(user.authorities).extracting<String> { it.authority }.containsOnly("ROLE_JOE")
    }

    @Test
    fun removeRole_successGroupManager() {
      val user = User.of("user")
      val group1 = Group("group", "desc")
      user.groups = setOf(group1)
      val role = Authority("ROLE_LICENCE_VARY", "Role Licence Vary")
      val role2 = Authority("JOE", "Bloggs")
      user.authorities = HashSet(listOf(role, role2))
      val groupManager = User.of("groupManager")
      groupManager.groups = setOf(group1)
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString()))
        .thenReturn(Optional.of(user))
        .thenReturn(Optional.of(groupManager))
      whenever(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role))
      whenever(roleRepository.findByGroupAssignableRolesForUsername(anyString())).thenReturn(setOf(role, role2))
      service.removeRole("user", "  licence_vary   ", "admin", GROUP_MANAGER)
      assertThat(user.authorities).extracting<String> { it.authority }.containsOnly("ROLE_JOE")
    }
  }

  @Nested
  inner class AssignableRoles {
    @Test
    fun `assignable roles for group manager`() {
      val first = Authority("FIRST", "Role First")
      val second = Authority("SECOND", "Role Second")
      val fred = Authority("FRED", "Role Fred")
      val joe = Authority("JOE", "Role Joe")

      whenever(roleRepository.findByGroupAssignableRolesForUsername(anyString())).thenReturn(setOf(first, fred, second))
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(
        Optional.of(
          User.builder().authorities(setOf(fred, joe)).build()
        )
      )
      assertThat(service.getAssignableRoles("BOB", GROUP_MANAGER)).containsExactly(first, second)
    }

    @Test
    fun `assignable roles for super user`() {
      val first = Authority("FIRST", "Role First")
      val second = Authority("SECOND", "Role Second")
      val fred = Authority("FRED", "Role Fred")
      val joe = Authority("JOE", "Role Joe")

      whenever(roleRepository.findAllByOrderByRoleName()).thenReturn(listOf(first, fred, second))
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(
        Optional.of(
          User.builder().authorities(setOf(fred, joe)).build()
        )
      )
      assertThat(service.getAssignableRoles("BOB", SUPER_USER)).containsExactly(first, second)
    }
  }

  companion object {
    private val SUPER_USER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
    private val GROUP_MANAGER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"))
  }
}
