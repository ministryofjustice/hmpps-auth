package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserRole
import java.util.*

class AuthUserRolesControllerTest {
  private val principal: Authentication = UsernamePasswordAuthenticationToken("bob", "pass")
  private val authUserService: AuthUserService = mock()
  private val authUserRoleService: AuthUserRoleService = mock()
  private val authUserRolesController = AuthUserRolesController(authUserService, authUserRoleService)

  @Test
  fun roles_userNotFound() {
    assertThatThrownBy { authUserRolesController.roles("bob") }
        .isInstanceOf(UsernameNotFoundException::class.java).withFailMessage("Account for username bob not found")
  }

  @Test
  fun roles_success() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    val responseEntity = authUserRolesController.roles("joe")
    assertThat(responseEntity).containsOnly(
        AuthUserRole(Authority("FRED", "FRED")),
        AuthUserRole(Authority("GLOBAL_SEARCH", "Global Search")))
  }

  @Test
  fun addRole_userNotFound() {
    assertThatThrownBy { authUserRolesController.addRole("bob", "role", principal) }
        .isInstanceOf(UsernameNotFoundException::class.java).withFailMessage("Account for username bob not found")
  }

  @Test
  fun addRole_success() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    authUserRolesController.addRole("someuser", "role", principal)
    verify(authUserRoleService).addRole("USER", "role", "bob", principal.authorities)
  }

  @Test
  fun removeRole_userNotFound() {
    assertThatThrownBy { authUserRolesController.removeRole("bob", "role", principal) }
        .isInstanceOf(UsernameNotFoundException::class.java).withFailMessage("Account for username bob not found")
  }

  @Test
  fun removeRole_success() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    authUserRolesController.removeRole("someuser", "joe", principal)
    verify(authUserRoleService).removeRole("USER", "joe", "bob", principal.authorities)
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
