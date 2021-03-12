package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserFilter
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.model.EmailAddress
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail
import uk.gov.justice.digital.hmpps.oauth2server.model.UserDetail
import uk.gov.justice.digital.hmpps.oauth2server.model.UserRole
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetailsHelper.Companion.createSampleNomisUser
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class UserControllerTest {
  private val userService: UserService = mock()
  private val userController = UserController(userService)
  private val authentication = UsernamePasswordAuthenticationToken("bob", "pass", listOf())

  @Test
  fun user_userNotFound() {
    val responseEntity = userController.user("bob")
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
  fun user_nomisUserNoCaseload() {
    setupFindUserCallForNomis()
    val responseEntity = userController.user("joe")
    assertThat(responseEntity.statusCodeValue).isEqualTo(200)
    assertThat(responseEntity.body).isEqualTo(
      UserDetail(
        "principal",
        false,
        "Joe Bloggs",
        AuthSource.nomis,
        5L,
        null,
        "5"
      )
    )
  }

  @Test
  fun user_nomisUser() {
    val staffUserAccount = setupFindUserCallForNomis()
    staffUserAccount.activeCaseLoadId = "somecase"
    val responseEntity = userController.user("joe")
    assertThat(responseEntity.statusCodeValue).isEqualTo(200)
    assertThat(responseEntity.body).isEqualTo(
      UserDetail(
        "principal",
        false,
        "Joe Bloggs",
        AuthSource.nomis,
        5L,
        "somecase",
        "5"
      )
    )
  }

  @Test
  fun user_authUser() {
    setupFindUserCallForAuth()
    val responseEntity = userController.user("joe")
    assertThat(responseEntity.statusCodeValue).isEqualTo(200)
    assertThat(responseEntity.body).isEqualTo(
      UserDetail(
        "principal",
        true,
        "Joe Bloggs",
        AuthSource.auth,
        null,
        null,
        USER_ID
      )
    )
  }

  @Test
  fun me_userNotFound() {
    val principal = TestingAuthenticationToken("principal", "credentials")
    assertThat(userController.me(principal)).isEqualTo(UserDetail.fromUsername("principal"))
  }

  @Test
  fun me_nomisUserNoCaseload() {
    setupFindUserCallForNomis()
    val principal = TestingAuthenticationToken("principal", "credentials")
    assertThat(userController.me(principal)).isEqualTo(
      UserDetail(
        "principal",
        false,
        "Joe Bloggs",
        AuthSource.nomis,
        5L,
        null,
        "5"
      )
    )
  }

  @Test
  fun me_nomisUser() {
    val staffUserAccount = setupFindUserCallForNomis()
    staffUserAccount.activeCaseLoadId = "somecase"
    val principal = TestingAuthenticationToken("principal", "credentials")
    assertThat(userController.me(principal)).isEqualTo(
      UserDetail(
        "principal",
        false,
        "Joe Bloggs",
        AuthSource.nomis,
        5L,
        "somecase",
        "5"
      )
    )
  }

  @Test
  fun me_authUser() {
    setupFindUserCallForAuth()
    val principal = TestingAuthenticationToken("principal", "credentials")
    assertThat(userController.me(principal)).isEqualTo(
      UserDetail(
        "principal",
        true,
        "Joe Bloggs",
        AuthSource.auth,
        null,
        null,
        USER_ID
      )
    )
  }

  @Test
  fun myRoles() {
    val authorities = listOf(SimpleGrantedAuthority("ROLE_BOB"), SimpleGrantedAuthority("ROLE_JOE_FRED"))
    val token = UsernamePasswordAuthenticationToken("principal", "credentials", authorities)
    assertThat(userController.myRoles(token)).containsOnly(UserRole("BOB"), UserRole("JOE_FRED"))
  }

  @Test
  fun myRoles_noRoles() {
    val token = UsernamePasswordAuthenticationToken("principal", "credentials", emptyList())
    assertThat(userController.myRoles(token)).isEmpty()
  }

  @Test
  fun userEmail_found() {
    whenever(userService.getOrCreateUser(anyString())).thenReturn(
      Optional.of(
        createSampleUser(username = "JOE", verified = true, email = "someemail")
      )
    )
    val responseEntity = userController.getUserEmail("joe")
    assertThat(responseEntity.statusCodeValue).isEqualTo(200)
    assertThat(responseEntity.body).isEqualTo(EmailAddress("JOE", "someemail"))
  }

  @Test
  fun userEmail_notFound() {
    whenever(userService.getOrCreateUser(anyString())).thenReturn(Optional.empty())
    val responseEntity = userController.getUserEmail("joe")
    assertThat(responseEntity.statusCodeValue).isEqualTo(404)
    assertThat(responseEntity.body).isEqualTo(
      ErrorDetail(
        "Not Found",
        "Account for username joe not found",
        "username"
      )
    )
  }

  @Test
  fun userEmail_notVerified() {
    whenever(userService.getOrCreateUser(anyString())).thenReturn(Optional.of(createSampleUser("JOE")))
    val responseEntity = userController.getUserEmail("joe")
    assertThat(responseEntity.statusCodeValue).isEqualTo(204)
    assertThat(responseEntity.body).isNull()
  }

  private val fakeUser: User
    get() = createSampleUser(
      id = UUID.fromString(USER_ID),
      username = "authentication",
      email = "email",
      verified = true,
      enabled = true,
      firstName = "Joe",
      lastName = "Bloggs",
      lastLoggedIn = LocalDateTime.of(2019, 1, 1, 12, 0)
    )

  @Test
  fun userSearch_multipleSourceSystems() {
    val unpaged = Pageable.unpaged()
    whenever(
      userService.searchUsersInMultipleSourceSystems(anyString(), any(), anyString(), any(), any(), anyOrNull()))
      .thenReturn(PageImpl(listOf(fakeUser))
    )

    val pageOfUsers = userController.searchForUsersInMultipleSourceSystems(
      "somename",
      UserFilter.Status.ALL,
      listOf(AuthSource.auth, AuthSource.nomis),
      unpaged,
      authentication
    )

    assertThat(pageOfUsers.totalElements).isEqualTo(1)

    verify(userService).searchUsersInMultipleSourceSystems(
      "somename",
      unpaged,
      "bob",
      emptyList(),
      UserFilter.Status.ALL,
      listOf(AuthSource.auth, AuthSource.nomis),
    )
  }

  @Test
  fun userSearch_mulitpleSourcesDefaultValues() {
    val unpaged = Pageable.unpaged()
    whenever(
      userService.searchUsersInMultipleSourceSystems(anyString(), any(), anyString(), any(), any(), anyOrNull()))
      .thenReturn(PageImpl(listOf(fakeUser))
      )

    val pageOfUsers = userController.searchForUsersInMultipleSourceSystems(
      "somename",
      UserFilter.Status.ALL,
      null,
      unpaged,
      authentication
    )

    assertThat(pageOfUsers.totalElements).isEqualTo(1)

    verify(userService).searchUsersInMultipleSourceSystems(
      "somename",
      unpaged,
      "bob",
      emptyList(),
      UserFilter.Status.ALL,
      null,
    )
  }

  @Test
  fun userSearch_mulitpleSourcesWithStatusFilterActive() {
    val unpaged = Pageable.unpaged()
    whenever(
      userService.searchUsersInMultipleSourceSystems(anyString(), any(), anyString(), any(), any(), anyOrNull()))
      .thenReturn(PageImpl(listOf(fakeUser))
      )

    val pageOfUsers = userController.searchForUsersInMultipleSourceSystems(
      "somename",
      UserFilter.Status.ACTIVE,
      listOf(AuthSource.auth, AuthSource.nomis, AuthSource.delius),
      unpaged,
      authentication
    )

    assertThat(pageOfUsers.totalElements).isEqualTo(1)

    verify(userService).searchUsersInMultipleSourceSystems(
      "somename",
      unpaged,
      "bob",
      emptyList(),
      UserFilter.Status.ACTIVE,
      listOf(AuthSource.auth, AuthSource.nomis, AuthSource.delius),
    )
  }

  private fun setupFindUserCallForNomis(): NomisUserPersonDetails {
    val user = createSampleNomisUser(staff = Staff(firstName = "JOE", status = "INACTIVE", lastName = "bloggs", staffId = 5), username = "principal", accountStatus = "EXPIRED & LOCKED")
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
    return user
  }

  private fun setupFindUserCallForAuth() {
    val user = createSampleUser(
      id = UUID.fromString(USER_ID),
      username = "principal",
      email = "email",
      verified = true,
      firstName = "Joe",
      lastName = "Bloggs",
      enabled = true,
      source = AuthSource.auth,
    )
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
  }

  companion object {
    private const val USER_ID = "07395ef9-53ec-4d6c-8bb1-0dc96cd4bd2f"
  }
}
