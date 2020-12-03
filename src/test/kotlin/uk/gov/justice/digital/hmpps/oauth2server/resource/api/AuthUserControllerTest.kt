package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.never
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.EmailType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService.CreateUserException
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserGroup
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail
import uk.gov.justice.digital.hmpps.oauth2server.resource.api.AuthUserController.AmendUser
import uk.gov.justice.digital.hmpps.oauth2server.resource.api.AuthUserController.AuthUser
import uk.gov.justice.digital.hmpps.oauth2server.resource.api.AuthUserController.CreateUser
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID
import javax.persistence.EntityNotFoundException
import javax.servlet.http.HttpServletRequest

class AuthUserControllerTest {
  private val userService: UserService = mock()
  private val authUserService: AuthUserService = mock()
  private val authUserGroupService: AuthUserGroupService = mock()
  private val request: HttpServletRequest = mock()
  private val authUserController = AuthUserController(userService, authUserService, authUserGroupService, false)
  private val authentication = UsernamePasswordAuthenticationToken("bob", "pass", listOf())

  @Test
  fun user_userNotFound() {
    val responseEntity = authUserController.user("bob")
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
  fun user_success() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(authUser))
    val responseEntity = authUserController.user("joe")
    assertThat(responseEntity.statusCodeValue).isEqualTo(200)
    assertThat(responseEntity.body).isEqualTo(
      AuthUser(
        USER_ID,
        "authentication",
        "email",
        "Joe",
        "Bloggs",
        false,
        true,
        true,
        LocalDateTime.of(2019, 1, 1, 12, 0)
      )
    )
  }

  @Test
  fun search() {
    whenever(authUserService.findAuthUsersByEmail(anyString())).thenReturn(listOf(authUser))
    val responseEntity = authUserController.searchForUser("joe")
    assertThat(responseEntity.statusCodeValue).isEqualTo(200)
    assertThat(responseEntity.body).isEqualTo(
      listOf(
        AuthUser(
          USER_ID,
          "authentication",
          "email",
          "Joe",
          "Bloggs",
          false,
          true,
          true,
          LocalDateTime.of(2019, 1, 1, 12, 0)
        )
      )
    )
  }

  @Test
  fun search_noResults() {
    whenever(authUserService.findAuthUsersByEmail(anyString())).thenReturn(listOf())
    val responseEntity = authUserController.searchForUser("joe")
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    assertThat(responseEntity.body).isNull()
  }

  @Test
  fun `createUser username already exists`() {
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(
      Optional.of(UserDetailsImpl("name", "bob", setOf(), "none", "userid", "jwtId"))
    )
    val responseEntity =
      authUserController.createUser(
        "user",
        CreateUser("email", "first", "last", null, null),
        true,
        request,
        authentication
      )
    assertThat(responseEntity.statusCodeValue).isEqualTo(409)
    assertThat(responseEntity.body).isEqualTo(
      ErrorDetail("username.exists", "User user already exists", "username")
    )
  }

  @Test
  fun `createUser email already exists`() {
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.empty())
    whenever(authUserService.findAuthUsersByEmail(anyString())).thenReturn(listOf(createSampleUser("joe")))
    val responseEntity =
      authUserController.createUser(
        "user",
        CreateUser("email", "first", "last", null, null),
        true,
        request,
        authentication
      )
    assertThat(responseEntity.statusCodeValue).isEqualTo(409)
    assertThat(responseEntity.body).isEqualTo(
      ErrorDetail("email.exists", "User email already exists", "email")
    )
  }

  @Test
  fun createUser_BlankDoesntCallUserService() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    val responseEntity =
      authUserController.createUser(
        "  ",
        CreateUser("email", "first", "last", null, null),
        true,
        request,
        authentication
      )
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    verify(userService, never()).findMasterUserPersonDetails(anyString())
  }

  @Test
  fun createUser_Success() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    val responseEntity =
      authUserController.createUser(
        "newusername",
        CreateUser("email", "first", "last", null, null),
        false,
        request,
        authentication
      )
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    assertThat(responseEntity.body).isNull()
  }

  @Test
  fun `createUser enforce unique email not called if parameter false`() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    val responseEntity =
      authUserController.createUser(
        "newusername",
        CreateUser("email", "first", "last", null, null),
        false,
        request,
        authentication
      )
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    verify(authUserService, never()).findAuthUsersByEmail(anyString())
  }

  @Test
  fun createUser_trim() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    authUserController.createUser(
      "   newusername   ",
      CreateUser("email", "first", "last", null, null),
      true,
      request,
      authentication
    )
    verify(userService).findMasterUserPersonDetails("newusername")
    verify(authUserService).createUser(
      "newusername",
      "email",
      "first",
      "last",
      emptySet(),
      "http://some.url/auth/initial-password?token=",
      "bob",
      authentication.authorities
    )
  }

  @Test
  fun createUser_CreateUserError() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    whenever(
      authUserService.createUser(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any(),
        anyString(),
        anyString(),
        any()
      )
    ).thenThrow(CreateUserException("username", "errorcode"))
    val responseEntity =
      authUserController.createUser(
        "newusername",
        CreateUser("email", "first", "last", null, null),
        true,
        request,
        authentication
      )
    assertThat(responseEntity.statusCodeValue).isEqualTo(400)
    assertThat(responseEntity.body).isEqualTo(
      ErrorDetail(
        "username.errorcode",
        "username failed validation",
        "username"
      )
    )
  }

  @Test
  fun createUser_VerifyUserError() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    whenever(
      authUserService.createUser(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any(),
        anyString(),
        anyString(),
        any()
      )
    ).thenThrow(VerifyEmailException("reason"))
    val responseEntity =
      authUserController.createUser(
        "newusername",
        CreateUser("email", "first", "last", null, null),
        true,
        request,
        authentication
      )
    assertThat(responseEntity.statusCodeValue).isEqualTo(400)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("email.reason", "Email address failed validation", "email"))
  }

  @Test
  fun createUser_InitialPasswordUrl() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    authUserController.createUser(
      "newusername",
      CreateUser("email", "first", "last", null, null),
      true,
      request,
      authentication
    )
    verify(authUserService).createUser(
      "newusername",
      "email",
      "first",
      "last",
      emptySet(),
      "http://some.url/auth/initial-password?token=",
      "bob",
      authentication.authorities
    )
  }

  @Test
  fun createUser_NoAdditionalRoles() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    authUserController.createUser(
      "newusername",
      CreateUser("email", "first", "last", null, null),
      true,
      request,
      authentication
    )
    verify(authUserService).createUser(
      "newusername",
      "email",
      "first",
      "last",
      emptySet(),
      "http://some.url/auth/initial-password?token=",
      "bob",
      authentication.authorities
    )
  }

  @Test
  fun createUser_MultipleAdditionalRoles() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    authUserController.createUser(
      "newusername",
      CreateUser("email", "first", "last", null, setOf("ROLE1", "ROLE2")),
      true,
      request,
      authentication
    )
    verify(authUserService).createUser(
      "newusername",
      "email",
      "first",
      "last",
      setOf("ROLE1", "ROLE2"),
      "http://some.url/auth/initial-password?token=",
      "bob",
      authentication.authorities
    )
  }

  @Test
  fun createUser_SingleAdditionalRole() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    authUserController.createUser(
      "newusername",
      CreateUser("email", "first", "last", "ROLE1", null),
      true,
      request,
      authentication
    )
    verify(authUserService).createUser(
      "newusername",
      "email",
      "first",
      "last",
      setOf("ROLE1"),
      "http://some.url/auth/initial-password?token=",
      "bob",
      authentication.authorities
    )
  }

  @Test
  fun createUser() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/api/authuser/newusername"))
    val responseEntity =
      authUserController.createUser(
        "newusername",
        CreateUser("email", "first", "last", null, null),
        true,
        request,
        authentication
      )
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    assertThat(responseEntity.body).isNull()
  }

  @Test
  fun createUserHandlesGroupCodeAsEmptyString() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/api/authuser/newusername"))
    val responseEntity =
      authUserController.createUser(
        "newusername",
        CreateUser("email", "first", "last", "", null),
        true,
        request,
        authentication
      )
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    assertThat(responseEntity.body).isNull()
  }

  @Test
  fun enableUser() {
    val user = createSampleUser(username = "USER", email = "email", verified = true)
    whenever(authUserService.getAuthUserByUsername("user")).thenReturn(Optional.of(user))
    val responseEntity = authUserController.enableUser("user", authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    verify(authUserService).enableUser("USER", "bob", authentication.authorities)
  }

  @Test
  fun enableUser_notFound() {
    val user = createSampleUser(username = "USER", email = "email", verified = true)
    whenever(authUserService.getAuthUserByUsername("user")).thenReturn(Optional.of(user))
    doThrow(EntityNotFoundException("message")).whenever(authUserService).enableUser(anyString(), anyString(), any())
    val responseEntity = authUserController.enableUser("user", authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
  }

  @Test
  fun disableUser() {
    val user = createSampleUser(username = "USER", email = "email", verified = true)
    whenever(authUserService.getAuthUserByUsername("user")).thenReturn(Optional.of(user))
    val responseEntity = authUserController.disableUser("user", authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    verify(authUserService).disableUser("USER", "bob", authentication.authorities)
  }

  @Test
  fun disableUser_notFound() {
    val user = createSampleUser(username = "USER", email = "email", verified = true)
    whenever(authUserService.getAuthUserByUsername("user")).thenReturn(Optional.of(user))
    doThrow(EntityNotFoundException("message")).whenever(authUserService).disableUser(anyString(), anyString(), any())
    val responseEntity = authUserController.disableUser("user", authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
  }

  @Test
  fun amendUser_checkService() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    authUserController.amendUser("user", AmendUser("a@b.com"), request, authentication)
    verify(authUserService).amendUserEmail(
      "user",
      "a@b.com",
      "http://some.url/auth/initial-password?token=",
      "bob",
      authentication.authorities,
      EmailType.PRIMARY
    )
  }

  @Test
  fun amendUser_statusCode() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    val responseEntity = authUserController.amendUser("user", AmendUser("a@b.com"), request, authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    assertThat(responseEntity.body).isNull()
  }

  @Test
  fun amendUser_notFound() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    whenever(
      authUserService.amendUserEmail(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any(),
        eq(EmailType.PRIMARY)
      )
    ).thenThrow(EntityNotFoundException("not found"))
    val responseEntity = authUserController.amendUser("user", AmendUser("a@b.com"), request, authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
  }

  @Test
  fun amendUser_verifyException() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    whenever(
      authUserService.amendUserEmail(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any(),
        eq(EmailType.PRIMARY)
      )
    ).thenThrow(VerifyEmailException("reason"))
    val responseEntity = authUserController.amendUser("user", AmendUser("a@b.com"), request, authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(400)
    assertThat(responseEntity.body).isEqualTo(ErrorDetail("email.reason", "Email address failed validation", "email"))
  }

  @Test
  fun amendUser_groupException() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/auth/api/authuser/newusername"))
    whenever(
      authUserService.amendUserEmail(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any(),
        eq(EmailType.PRIMARY)
      )
    ).thenThrow(AuthUserGroupRelationshipException("user", "reason"))
    val responseEntity = authUserController.amendUser("user", AmendUser("a@b.com"), request, authentication)
    assertThat(responseEntity.statusCodeValue).isEqualTo(403)
    assertThat(responseEntity.body).isEqualTo(
      ErrorDetail(
        "unable to maintain user",
        "Unable to amend user, the user is not within one of your groups",
        "groups"
      )
    )
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
      val user = createSampleUser(
        id = UUID.fromString(USER_ID),
        username = "authentication",
        email = "email",
        verified = true,
        enabled = true,
        firstName = "Joe",
        lastName = "Bloggs",
        lastLoggedIn = LocalDateTime.of(2019, 1, 1, 12, 0)
      )
      return user
    }

  @Test
  fun searchForUser() {
    val unpaged = Pageable.unpaged()
    whenever(authUserService.findAuthUsers(anyString(), anyList(), anyList(), any(), anyString(), any())).thenReturn(
      PageImpl(
        listOf(
          authUser
        )
      )
    )
    authUserController.searchForUser("somename", listOf("somerole"), listOf("somegroup"), unpaged, authentication)
    verify(authUserService).findAuthUsers("somename", listOf("somerole"), listOf("somegroup"), unpaged, "bob", emptyList())
  }

  @Test
  fun `searchForUser map auth user`() {
    val unpaged = Pageable.unpaged()
    whenever(authUserService.findAuthUsers(anyString(), anyList(), anyList(), any(), anyString(), any())).thenReturn(
      PageImpl(
        listOf(
          authUser
        )
      )
    )
    val page = authUserController.searchForUser("somename", listOf("somerole"), listOf("somegroup"), unpaged, authentication).toList()
    assertThat(page).hasSize(1).containsExactlyInAnyOrder(
      AuthUser(
        userId = USER_ID,
        username = "authentication",
        email = "email",
        verified = true,
        enabled = true,
        firstName = "Joe",
        lastName = "Bloggs",
        lastLoggedIn = LocalDateTime.parse("2019-01-01T12:00")
      )
    )
  }

  companion object {
    private const val USER_ID = "07395ef9-53ec-4d6c-8bb1-0dc96cd4bd2f"
  }
}
