package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService.AuthUserGroupException
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService.AuthUserGroupExistsException
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserGroup
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail
import java.security.Principal
import java.util.*

class AuthUserGroupsControllerTest {
  private val principal: Principal = UsernamePasswordAuthenticationToken("bob", "pass")
  private val authUserService: AuthUserService = mock()
  private val authUserGroupService: AuthUserGroupService = mock()
  private val authUserGroupsController = AuthUserGroupsController(authUserService, authUserGroupService)

  @Test
  fun groups_userNotFound() {
    val responseEntity = authUserGroupsController.groups("bob")
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("Not Found", "Account for username bob not found", "username"))
  }

  @Test
  fun groups_success() {
    val group1 = Group("FRED", "desc")
    val group2 = Group("GLOBAL_SEARCH", "desc2")
    whenever(authUserGroupService.getAuthUserGroups(anyString())).thenReturn(Optional.of(setOf(group1, group2)))
    val responseEntity = authUserGroupsController.groups("joe")
    assertThat(responseEntity.statusCodeValue).isEqualTo(200)
    assertThat(responseEntity.body as List<*>).containsOnly(AuthUserGroup(group1), AuthUserGroup(group2))
  }

  @Test
  fun addGroup_userNotFound() {
    val responseEntity = authUserGroupsController.addGroup("bob", "group", principal)
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("Not Found", "Account for username bob not found", "username"))
  }

  @Test
  fun addGroup_success() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    val responseEntity = authUserGroupsController.addGroup("someuser", "group", principal)
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    verify(authUserGroupService).addGroup("USER", "group", "bob")
  }

  @Test
  fun addGroup_conflict() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    doThrow(AuthUserGroupExistsException()).whenever(authUserGroupService).addGroup(anyString(), anyString(), anyString())
    val responseEntity = authUserGroupsController.addGroup("someuser", "joe", principal)
    assertThat(responseEntity.statusCodeValue).isEqualTo(409)
  }

  @Test
  fun addGroup_validation() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    doThrow(AuthUserGroupException("group", "error")).whenever(authUserGroupService).addGroup(anyString(), anyString(), anyString())
    val responseEntity = authUserGroupsController.addGroup("someuser", "harry", principal)
    assertThat(responseEntity.statusCodeValue).isEqualTo(400)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("group.error", "group failed validation", "group"))
  }

  @Test
  fun removeGroup_userNotFound() {
    val responseEntity = authUserGroupsController.removeGroup("bob", "group", principal)
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("Not Found", "Account for username bob not found", "username"))
  }

  @Test
  fun removeGroup_success() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    val responseEntity = authUserGroupsController.removeGroup("someuser", "joe", principal)
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    verify(authUserGroupService).removeGroup("USER", "joe", "bob")
  }

  @Test
  fun removeGroup_groupMissing() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    doThrow(AuthUserGroupException("group", "error")).whenever(authUserGroupService).removeGroup(anyString(), anyString(), anyString())
    val responseEntity = authUserGroupsController.removeGroup("someuser", "harry", principal)
    assertThat(responseEntity.statusCodeValue).isEqualTo(400)
  }

  private val authUser: User
    get() {
      val user = User.builder().username("USER").email("email").verified(true).build()
      user.groups = setOf(Group("GLOBAL_SEARCH", "desc2"), Group("FRED", "desc"))
      return user
    }
}
