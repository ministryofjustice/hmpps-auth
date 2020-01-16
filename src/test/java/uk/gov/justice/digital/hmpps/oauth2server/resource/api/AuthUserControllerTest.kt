package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.never
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService.AmendUserException
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService.CreateUserException
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserGroup
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail
import uk.gov.justice.digital.hmpps.oauth2server.resource.api.AuthUserController.*
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException
import uk.gov.service.notify.NotificationClientException
import java.time.LocalDateTime
import java.util.*
import javax.persistence.EntityNotFoundException
import javax.servlet.http.HttpServletRequest

class AuthUserControllerTest {
  private val userService: UserService = mock()
  private val authUserService: AuthUserService = mock()
  private val authUserGroupService: AuthUserGroupService = mock()
  private val request: HttpServletRequest = mock()
  private lateinit var authUserController: AuthUserController
  private val authentication: Authentication = UsernamePasswordAuthenticationToken("bob", "pass")
  @Before
  fun setUp() {
    authUserController = AuthUserController(userService, authUserService, authUserGroupService, false)
  }

  @Test
  fun user_userNotFound() {
    val responseEntity = authUserController.user("bob")
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("Not Found", "Account for username bob not found", "username"))
  }

  @Test
  fun user_success() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    val responseEntity = authUserController.user("joe")
    assertThat(responseEntity.statusCodeValue).isEqualTo(200)
    assertThat(responseEntity.body).isEqualTo(AuthUser(USER_ID, "authentication", "email", "Joe", "Bloggs", false, true, true, LocalDateTime.of(2019, 1, 1, 12, 0)))
  }

  @Test
  fun search() {
    whenever(authUserService.findAuthUsersByEmail(anyString())).thenReturn(listOf(authUser))
    val responseEntity = authUserController.searchForUser("joe")
    assertThat(responseEntity.statusCodeValue).isEqualTo(200)
    assertThat(responseEntity.body).isEqualTo(listOf(AuthUser(USER_ID, "authentication", "email", "Joe", "Bloggs", false, true, true, LocalDateTime.of(2019, 1, 1, 12, 0))))
  }

  @Test
  fun search_noResults() {
    whenever(authUserService.findAuthUsersByEmail(anyString())).thenReturn(listOf())
    val responseEntity = authUserController.searchForUser("joe")
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    assertThat(responseEntity.body).isNull()
  }

  @Test
  @Throws(NotificationClientException::class)
  fun createUser_AlreadyExists() {
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(UserDetailsImpl("name", "bob", setOf(), null, null)))
    val responseEntity = authUserController.createUser("user", CreateUser("email", "first", "last", null), request, authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(409)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("username.exists", "Username user already exists", "username"))
  }

  @Test
  @Throws(NotificationClientException::class)
  fun createUser_BlankDoesntCallUserService() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    val responseEntity = authUserController.createUser("  ", CreateUser("email", "first", "last", null), request, authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    verify(userService, never()).findMasterUserPersonDetails(anyString())
  }

  @Test
  @Throws(NotificationClientException::class)
  fun createUser_Success() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    val responseEntity = authUserController.createUser("newusername", CreateUser("email", "first", "last", null), request, authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    assertThat(responseEntity.body).isNull()
  }

  @Test
  @Throws(NotificationClientException::class, CreateUserException::class, VerifyEmailException::class)
  fun createUser_trim() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    authUserController.createUser("   newusername   ", CreateUser("email", "first", "last", null), request, authentication)
    verify(userService).findMasterUserPersonDetails("newusername")
    verify(authUserService).createUser("newusername", "email", "first", "last", null, "http://some.url/auth/initial-password?token=", "bob", authentication.authorities)
  }

  @Test
  @Throws(NotificationClientException::class, CreateUserException::class, VerifyEmailException::class)
  fun createUser_CreateUserError() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    whenever(authUserService.createUser(anyString(), anyString(), anyString(), anyString(), isNull(), anyString(), anyString(), any())).thenThrow(CreateUserException("username", "errorcode"))
    val responseEntity = authUserController.createUser("newusername", CreateUser("email", "first", "last", null), request, authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(400)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("username.errorcode", "username failed validation", "username"))
  }

  @Test
  @Throws(NotificationClientException::class, CreateUserException::class, VerifyEmailException::class)
  fun createUser_VerifyUserError() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    whenever(authUserService.createUser(anyString(), anyString(), anyString(), anyString(), isNull(), anyString(), anyString(), any())).thenThrow(VerifyEmailException("reason"))
    val responseEntity = authUserController.createUser("newusername", CreateUser("email", "first", "last", null), request, authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(400)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("email.reason", "Email address failed validation", "email"))
  }

  @Test
  @Throws(NotificationClientException::class, CreateUserException::class, VerifyEmailException::class)
  fun createUser_InitialPasswordUrl() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    authUserController.createUser("newusername", CreateUser("email", "first", "last", null), request, authentication)
    verify(authUserService).createUser("newusername", "email", "first", "last", null, "http://some.url/auth/initial-password?token=", "bob", authentication.authorities)
  }

  @Test
  @Throws(NotificationClientException::class, CreateUserException::class, VerifyEmailException::class)
  fun createUser_NoAdditionalRoles() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    authUserController.createUser("newusername", CreateUser("email", "first", "last", null), request, authentication)
    verify(authUserService).createUser("newusername", "email", "first", "last", null, "http://some.url/auth/initial-password?token=", "bob", authentication.authorities)
  }

  @Test
  @Throws(NotificationClientException::class)
  fun createUser() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/api/authuser/newusername"))
    val responseEntity = authUserController.createUser("newusername", CreateUser("email", "first", "last", null), request, authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    assertThat(responseEntity.body).isNull()
  }

  @Test
  @Throws(AuthUserGroupRelationshipException::class)
  fun enableUser() {
    val user = User.builder().username("USER").email("email").verified(true).build()
    whenever(authUserService.getAuthUserByUsername("user")).thenReturn(Optional.of(user))
    val responseEntity = authUserController.enableUser("user", authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    verify(authUserService).enableUser("USER", "bob", authentication.authorities)
  }

  @Test
  @Throws(AuthUserGroupRelationshipException::class)
  fun enableUser_notFound() {
    val user = User.builder().username("USER").email("email").verified(true).build()
    whenever(authUserService.getAuthUserByUsername("user")).thenReturn(Optional.of(user))
    doThrow(EntityNotFoundException("message")).whenever(authUserService).enableUser(anyString(), anyString(), any())
    val responseEntity = authUserController.enableUser("user", authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
  }

  @Test
  @Throws(AuthUserGroupRelationshipException::class)
  fun disableUser() {
    val user = User.builder().username("USER").email("email").verified(true).build()
    whenever(authUserService.getAuthUserByUsername("user")).thenReturn(Optional.of(user))
    val responseEntity = authUserController.disableUser("user", authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    verify(authUserService).disableUser("USER", "bob", authentication.authorities)
  }

  @Test
  @Throws(AuthUserGroupRelationshipException::class)
  fun disableUser_notFound() {
    val user = User.builder().username("USER").email("email").verified(true).build()
    whenever(authUserService.getAuthUserByUsername("user")).thenReturn(Optional.of(user))
    doThrow(EntityNotFoundException("message")).whenever(authUserService).disableUser(anyString(), anyString(), any())
    val responseEntity = authUserController.disableUser("user", authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
  }

  @Test
  @Throws(NotificationClientException::class, VerifyEmailException::class, AmendUserException::class, AuthUserGroupRelationshipException::class)
  fun amendUser_checkService() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    authUserController.amendUser("user", AmendUser("a@b.com"), request, authentication)
    verify(authUserService).amendUserEmail("user", "a@b.com", "http://some.url/auth/initial-password?token=", "bob", authentication.authorities)
  }

  @Test
  @Throws(NotificationClientException::class)
  fun amendUser_statusCode() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    val responseEntity = authUserController.amendUser("user", AmendUser("a@b.com"), request, authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    assertThat(responseEntity.body).isNull()
  }

  @Test
  @Throws(NotificationClientException::class, VerifyEmailException::class, AmendUserException::class, AuthUserGroupRelationshipException::class)
  fun amendUser_notFound() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    whenever(authUserService.amendUserEmail(anyString(), anyString(), anyString(), anyString(), any())).thenThrow(EntityNotFoundException("not found"))
    val responseEntity = authUserController.amendUser("user", AmendUser("a@b.com"), request, authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
  }

  @Test
  @Throws(NotificationClientException::class, VerifyEmailException::class, AmendUserException::class, AuthUserGroupRelationshipException::class)
  fun amendUser_amendException() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    whenever(authUserService.amendUserEmail(anyString(), anyString(), anyString(), anyString(), any())).thenThrow(AmendUserException("fiel", "cod"))
    val responseEntity = authUserController.amendUser("user", AmendUser("a@b.com"), request, authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(400)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("fiel.cod", "fiel failed validation", "fiel"))
  }

  @Test
  @Throws(NotificationClientException::class, VerifyEmailException::class, AmendUserException::class, AuthUserGroupRelationshipException::class)
  fun amendUser_verifyException() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    whenever(authUserService.amendUserEmail(anyString(), anyString(), anyString(), anyString(), any())).thenThrow(VerifyEmailException("reason"))
    val responseEntity = authUserController.amendUser("user", AmendUser("a@b.com"), request, authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(400)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("email.reason", "Email address failed validation", "email"))
  }

  @Test
  @Throws(NotificationClientException::class, VerifyEmailException::class, AmendUserException::class, AuthUserGroupRelationshipException::class)
  fun amendUser_groupException() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    whenever(authUserService.amendUserEmail(anyString(), anyString(), anyString(), anyString(), any())).thenThrow(AuthUserGroupRelationshipException("user", "reason"))
    val responseEntity = authUserController.amendUser("user", AmendUser("a@b.com"), request, authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(403)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("unable to maintain user", "Unable to amend user, the user is not within one of your groups", "groups"))
  }

  @Test
  fun assignableGroups_success() {
    val group1 = Group("FRED", "desc")
    val group2 = Group("GLOBAL_SEARCH", "desc2")
    whenever(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(listOf(group1, group2))
    val responseEntity = authUserController.assignableGroups(authentication)
    assertThat(responseEntity).containsOnly(AuthUserGroup(group1), AuthUserGroup(group2))
  }

  private val authUser: User
    get() {
      val user = User.builder().id(UUID.fromString(USER_ID)).username("authentication").email("email").verified(true).enabled(true).lastLoggedIn(LocalDateTime.of(2019, 1, 1, 12, 0)).build()
      user.person = Person()
      user.person.firstName = "Joe"
      user.person.lastName = "Bloggs"
      return user
    }

  @Test
  fun searchForUser() {
    val unpaged = Pageable.unpaged()
    whenever(authUserService.findAuthUsers(anyString(), anyString(), anyString(), any())).thenReturn(PageImpl(listOf(authUser)))
    authUserController.searchForUser("somename", "somerole", "somegroup", unpaged)
    verify(authUserService).findAuthUsers("somename", "somerole", "somegroup", unpaged)
  }

  companion object {
    private const val USER_ID = "07395ef9-53ec-4d6c-8bb1-0dc96cd4bd2f"
  }
}
