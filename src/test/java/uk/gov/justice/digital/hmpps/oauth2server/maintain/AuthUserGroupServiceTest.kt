package uk.gov.justice.digital.hmpps.oauth2server.maintain

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.GroupRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService.AuthUserGroupException
import java.util.HashSet
import java.util.Optional

class AuthUserGroupServiceTest {
  private val userRepository: UserRepository = mock()
  private val groupRepository: GroupRepository = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val service = AuthUserGroupService(userRepository, groupRepository, telemetryClient)

  @Test
  fun addGroup_blank() {
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(User.of("user")))
    assertThatThrownBy {
      service.addGroup(
        "user",
        "        ",
        "admin"
      )
    }.isInstanceOf(AuthUserGroupException::class.java)
      .hasMessage("Add group failed for field group with reason: notfound")
  }

  @Test
  fun addGroup_groupAlreadyOnUser() {
    val user = User.of("user")
    val group = Group("GROUP_LICENCE_VARY", "desc")
    user.groups = HashSet(listOf(group))
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
    whenever(groupRepository.findByGroupCode(anyString())).thenReturn(Optional.of(group))
    assertThatThrownBy {
      service.addGroup(
        "user",
        "LICENCE_VARY",
        "admin"
      )
    }.isInstanceOf(AuthUserGroupException::class.java)
      .hasMessage("Add group failed for field group with reason: exists")
  }

  @Test
  fun addGroup_success() {
    val user = User.of("user")
    user.groups = HashSet(listOf(Group("GROUP_JOE", "desc")))
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
    val group = Group("GROUP_LICENCE_VARY", "desc")
    whenever(groupRepository.findByGroupCode(anyString())).thenReturn(Optional.of(group))
    service.addGroup("user", "GROUP_LICENCE_VARY", "admin")
    assertThat(user.groups).extracting<String> { it.groupCode }.containsOnly("GROUP_JOE", "GROUP_LICENCE_VARY")
  }

  @Test
  fun removeGroup_groupNotOnUser() {
    val user = User.of("user")
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
    assertThatThrownBy { service.removeGroup("user", "BOB", "admin") }.isInstanceOf(AuthUserGroupException::class.java)
      .hasMessage("Add group failed for field group with reason: missing")
  }

  @Test
  fun removeGroup_success() {
    val user = User.of("user")
    user.groups = HashSet(listOf(Group("JOE", "desc"), Group("LICENCE_VARY", "desc2")))
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
    service.removeGroup("user", "  licence_vary   ", "admin")
    assertThat(user.groups).extracting<String> { it.groupCode }.containsOnly("JOE")
  }

  @Test
  fun authUserGroups_notfound() {
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.empty())
    val groups = service.getAuthUserGroups(" BOB ")
    assertThat(groups).isNotPresent
  }

  @Test
  fun authUserAssignableGroups_notAdminAndNoUser() {
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.empty())
    val groups = service.getAssignableGroups(" BOB ", setOf())
    assertThat(groups).isEmpty()
  }

  @Test
  fun authUserGroups_success() {
    val user = User.of("user")
    user.groups = HashSet(listOf(Group("JOE", "desc"), Group("LICENCE_VARY", "desc2")))
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
    val groups = service.getAuthUserGroups(" BOB ")
    assertThat(groups.get()).extracting<String> { it.groupCode }.containsOnly("JOE", "LICENCE_VARY")
  }

  @Test
  fun authUserAssignableGroups_normalUser() {
    val user = User.of("user")
    user.groups = HashSet(listOf(Group("JOE", "desc"), Group("LICENCE_VARY", "desc2")))
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
    val groups = service.getAssignableGroups(" BOB ", setOf())
    assertThat(groups).extracting<String> { it.groupCode }.containsOnly("JOE", "LICENCE_VARY")
  }

  @Test
  fun authUserAssignableGroups_superUser() {
    whenever(groupRepository.findAllByOrderByGroupName()).thenReturn(
      listOf(
        Group("JOE", "desc"),
        Group("LICENCE_VARY", "desc2")
      )
    )
    val groups = service.getAssignableGroups(" BOB ", setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS")))
    assertThat(groups).extracting<String> { it.groupCode }.containsOnly("JOE", "LICENCE_VARY")
  }
}
