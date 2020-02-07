package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService.AuthUserRoleException
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserRole
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException
import java.util.*

class AuthUserRolesControllerTest {
  private val principal: Authentication = UsernamePasswordAuthenticationToken("bob", "pass")
  private val authUserService: AuthUserService = mock()
  private val authUserRoleService: AuthUserRoleService = mock()
  private val authUserRolesController = AuthUserRolesController(authUserService, authUserRoleService)

  @Test
  fun roles_userNotFound() {
    val responseEntity = authUserRolesController.roles("bob")
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("Not Found", "Account for username bob not found", "username"))
  }

  @Test
  fun roles_success() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    val responseEntity = authUserRolesController.roles("joe")
    assertThat(responseEntity.statusCodeValue).isEqualTo(200)
    assertThat(responseEntity.body as Set<*>).containsOnly(AuthUserRole(Authority("FRED", "FRED")), AuthUserRole(Authority("GLOBAL_SEARCH", "Global Search")))
  }

  @Test
  fun addRole_userNotFound() {
    val responseEntity = authUserRolesController.addRole("bob", "role", principal)
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("Not Found", "Account for username bob not found", "username"))
  }

  @Test
  fun addRole_success() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    val responseEntity = authUserRolesController.addRole("someuser", "role", principal)
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    verify(authUserRoleService).addRole("USER", "role", "bob", principal.authorities)
  }

  @Test
  fun addRole_conflict() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    doThrow(AuthUserGroupRelationshipException("someuser", "User not with your groups")).whenever(authUserRoleService).addRole(anyString(), anyString(), anyString(), any())
    val responseEntity = authUserRolesController.addRole("someuser", "joe", principal)
    assertThat(responseEntity.statusCodeValue).isEqualTo(409)
  }

  @Test
  fun addRole_validation() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    doThrow(AuthUserRoleException("role", "error")).whenever(authUserRoleService).addRole(anyString(), anyString(), anyString(), any())
    val responseEntity = authUserRolesController.addRole("someuser", "harry", principal)
    assertThat(responseEntity.statusCodeValue).isEqualTo(400)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("role.error", "role failed validation", "role"))
  }

  @Test
  fun removeRole_userNotFound() {
    val responseEntity = authUserRolesController.removeRole("bob", "role", principal)
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("Not Found", "Account for username bob not found", "username"))
  }

  @Test
  fun removeRole_success() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    val responseEntity = authUserRolesController.removeRole("someuser", "joe", principal)
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    verify(authUserRoleService).removeRole("USER", "joe", "bob", principal.authorities)
  }

  @Test
  fun removeRole_roleMissing() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    doThrow(AuthUserRoleException("role", "error")).whenever(authUserRoleService).removeRole(anyString(), anyString(), anyString(), any())
    val responseEntity = authUserRolesController.removeRole("someuser", "harry", principal)
    assertThat(responseEntity.statusCodeValue).isEqualTo(400)
  }

  @Test
  fun assignableRoles() {
    whenever(authUserRoleService.getAssignableRoles(anyString(), any())).thenReturn(listOf(Authority("FRED", "FRED"), Authority("GLOBAL_SEARCH", "Global Search")))
    val response = authUserRolesController.assignableRoles("someuser", principal)
    assertThat(response).containsOnly(
        AuthUserRole(Authority("FRED", "FRED")),
        AuthUserRole(Authority("GLOBAL_SEARCH", "Global Search")))
  }

  private val authUser: User
    get() {
      val user = User.builder().username("USER").email("email").verified(true).build()
      user.authorities = setOf(Authority("FRED", "FRED"), Authority("GLOBAL_SEARCH", "Global Search"))
      return user
    }
}
