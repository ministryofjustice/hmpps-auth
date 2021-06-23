package uk.gov.justice.digital.hmpps.oauth2server.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff
import uk.gov.justice.digital.hmpps.oauth2server.security.NomisUserService
import uk.gov.service.notify.NotificationClientApi

class ForgottenUsernameServiceTest {
  private val nomisUserService: NomisUserService = mock()
  private val authUserService: AuthUserService = mock()
  private val deliusUserService: DeliusUserService = mock()
  private val notificationClientApi: NotificationClientApi = mock()
  private val service = ForgottenUsernameService(
    deliusUserService,
    authUserService,
    nomisUserService,
    notificationClientApi,
    "emailTemplate"
  )

  @Test
  fun `verify all auth nomis and delius are called`() {
    service.forgottenUsername("a@b.com", "someurl/forgotten-username")

    verify(deliusUserService).getDeliusUsersByEmail("a@b.com")
    verify(authUserService).findAuthUsersByEmail("a@b.com")
    verify(nomisUserService).getNomisUsersByEmail("a@b.com")
  }

  @Test
  fun `when no username found for email address notify not called`() {
    service.forgottenUsername("a@b.com", "someurl/forgotten-username")
    verify(deliusUserService).getDeliusUsersByEmail("a@b.com")
    verify(authUserService).findAuthUsersByEmail("a@b.com")
    verify(nomisUserService).getNomisUsersByEmail("a@b.com")
    verify(notificationClientApi, never()).sendEmail(anyString(), anyString(), any(), anyOrNull())
  }

  @Test
  fun `username not found if auth user has not verified email address`() {
    whenever(authUserService.findAuthUsersByEmail("a@b.com")).thenReturn(
      listOf(
        UserHelper.createSampleUser(
          email = "a@b.com",
          enabled = true,
          verified = false,
        )
      )
    )
    val username = service.forgottenUsername("a@b.com", "someurl/forgotten-username")

    assertThat(username).isEmpty()
    verify(deliusUserService).getDeliusUsersByEmail("a@b.com")
    verify(authUserService).findAuthUsersByEmail("a@b.com")
    verify(nomisUserService).getNomisUsersByEmail("a@b.com")
    verify(notificationClientApi, never()).sendEmail(anyString(), anyString(), any(), anyOrNull())
  }

  @Test
  fun `one username found for auth user with verified email address`() {
    whenever(authUserService.findAuthUsersByEmail("a@b.com")).thenReturn(
      listOf(
        UserHelper.createSampleUser(
          email = "a@b.com",
          enabled = true,
          verified = true,
        )
      )
    )
    val username = service.forgottenUsername("a@b.com", "someurl/forgotten-username")

    val map = mapOf(
      "firstName" to "first",
      "username" to listOf("firstlast"),
      "signinUrl" to "someurl/",
      "single" to "yes",
      "multiple" to "no"
    )

    assertThat(username).isEqualTo(listOf("firstlast"))
    verify(deliusUserService).getDeliusUsersByEmail("a@b.com")
    verify(authUserService).findAuthUsersByEmail("a@b.com")
    verify(nomisUserService).getNomisUsersByEmail("a@b.com")
    verify(notificationClientApi).sendEmail("emailTemplate", "a@b.com", map, null)
  }

  @Test
  fun `one username found for nomis user`() {
    val nomisUser = NomisUserPersonDetails(
      "username1",
      "",
      Staff(firstName = "bob", status = "ACTIVE", lastName = "Smith", staffId = 1),
      "GEN",
      "MDI",
      listOf(),
      AccountDetail("username1", "OPEN", "GEN", null)
    )
    whenever(nomisUserService.getNomisUsersByEmail(anyString())).thenReturn(listOf(nomisUser))
    val username = service.forgottenUsername("a@b.com", "someurl/forgotten-username")

    val map = mapOf(
      "firstName" to "Bob",
      "username" to listOf("username1"),
      "signinUrl" to "someurl/",
      "single" to "yes",
      "multiple" to "no"
    )

    assertThat(username).isEqualTo(listOf("username1"))
    verify(deliusUserService).getDeliusUsersByEmail("a@b.com")
    verify(authUserService).findAuthUsersByEmail("a@b.com")
    verify(nomisUserService).getNomisUsersByEmail("a@b.com")
    verify(notificationClientApi).sendEmail("emailTemplate", "a@b.com", map, null)
  }

  @Test
  fun `one username found for Delius user`() {
    val deliusUser = DeliusUserPersonDetails("username2", "id", "user", "name", "email@email.com", true)
    whenever(deliusUserService.getDeliusUsersByEmail(anyString())).thenReturn(listOf(deliusUser))
    val username = service.forgottenUsername("a@b.com", "someurl/forgotten-username")

    val map = mapOf(
      "firstName" to "user",
      "username" to listOf("username2"),
      "signinUrl" to "someurl/",
      "single" to "yes",
      "multiple" to "no"
    )

    assertThat(username).isEqualTo(listOf("username2"))
    verify(deliusUserService).getDeliusUsersByEmail("a@b.com")
    verify(authUserService).findAuthUsersByEmail("a@b.com")
    verify(nomisUserService).getNomisUsersByEmail("a@b.com")
    verify(notificationClientApi).sendEmail("emailTemplate", "a@b.com", map, null)
  }

  @Test
  fun `multiple username found for auth user with verified email address`() {
    whenever(authUserService.findAuthUsersByEmail("a@b.com")).thenReturn(
      listOf(
        UserHelper.createSampleUser(
          username = "user1",
          email = "a@b.com",
          enabled = true,
          verified = true,
        ),
        UserHelper.createSampleUser(
          username = "user2",
          email = "a@b.com",
          enabled = true,
          verified = true,
        )
      )
    )
    val username = service.forgottenUsername("a@b.com", "someurl/forgotten-username")

    val map = mapOf(
      "firstName" to "first",
      "username" to listOf("user1", "user2"),
      "signinUrl" to "someurl/",
      "single" to "no",
      "multiple" to "yes"
    )

    assertThat(username).isEqualTo(listOf("user1", "user2"))
    verify(deliusUserService).getDeliusUsersByEmail("a@b.com")
    verify(authUserService).findAuthUsersByEmail("a@b.com")
    verify(nomisUserService).getNomisUsersByEmail("a@b.com")
    verify(notificationClientApi).sendEmail("emailTemplate", "a@b.com", map, null)
  }

  @Test
  fun `single username found for auth user with verified and non verified email address`() {
    whenever(authUserService.findAuthUsersByEmail("a@b.com")).thenReturn(
      listOf(
        UserHelper.createSampleUser(
          username = "user1",
          email = "a@b.com",
          enabled = true,
          verified = true,
        ),
        UserHelper.createSampleUser(
          username = "user2",
          email = "a@b.com",
          enabled = true,
          verified = false,
        )
      )
    )
    val username = service.forgottenUsername("a@b.com", "someurl/forgotten-username")

    val map = mapOf(
      "firstName" to "first",
      "username" to listOf("user1"),
      "signinUrl" to "someurl/",
      "single" to "yes",
      "multiple" to "no"
    )

    assertThat(username).isEqualTo(listOf("user1"))
    verify(deliusUserService).getDeliusUsersByEmail("a@b.com")
    verify(authUserService).findAuthUsersByEmail("a@b.com")
    verify(nomisUserService).getNomisUsersByEmail("a@b.com")
    verify(notificationClientApi).sendEmail("emailTemplate", "a@b.com", map, null)
  }

  @Test
  fun `single username found for auth user with enabled and not enabled accounts`() {
    whenever(authUserService.findAuthUsersByEmail("a@b.com")).thenReturn(
      listOf(
        UserHelper.createSampleUser(
          username = "user1",
          email = "a@b.com",
          enabled = true,
          verified = true,
        ),
        UserHelper.createSampleUser(
          username = "user2",
          email = "a@b.com",
          enabled = false,
          verified = false,
        )
      )
    )
    val username = service.forgottenUsername("a@b.com", "someurl/forgotten-username")

    val map = mapOf(
      "firstName" to "first",
      "username" to listOf("user1"),
      "signinUrl" to "someurl/",
      "single" to "yes",
      "multiple" to "no"
    )

    assertThat(username).isEqualTo(listOf("user1"))
    verify(deliusUserService).getDeliusUsersByEmail("a@b.com")
    verify(authUserService).findAuthUsersByEmail("a@b.com")
    verify(nomisUserService).getNomisUsersByEmail("a@b.com")
    verify(notificationClientApi).sendEmail("emailTemplate", "a@b.com", map, null)
  }

  @Test
  fun `multiple usernames found for nomis, delius and auth user with verified email address`() {
    val authUser =
      listOf(
        UserHelper.createSampleUser(
          username = "user1",
          email = "a@b.com",
          enabled = true,
          verified = true,
        ),
        UserHelper.createSampleUser(
          username = "user2",
          email = "a@b.com",
          enabled = true,
          verified = true,
        )
      )

    val nomisUser = NomisUserPersonDetails(
      "username1",
      "",
      Staff(firstName = "bob", status = "ACTIVE", lastName = "Smith", staffId = 1),
      "GEN",
      "MDI",
      listOf(),
      AccountDetail("username1", "OPEN", "GEN", null)
    )
    val deliusUser = DeliusUserPersonDetails("username2", "id", "user", "name", "email@email.com", true)

    whenever(authUserService.findAuthUsersByEmail("a@b.com")).thenReturn(authUser)
    whenever(nomisUserService.getNomisUsersByEmail(anyString())).thenReturn(listOf(nomisUser))
    whenever(deliusUserService.getDeliusUsersByEmail(anyString())).thenReturn(listOf(deliusUser))

    val username = service.forgottenUsername("a@b.com", "someurl/forgotten-username")

    val map = mapOf(
      "firstName" to "first",
      "username" to listOf("user1", "user2", "username1", "username2"),
      "signinUrl" to "someurl/",
      "single" to "no",
      "multiple" to "yes"
    )

    assertThat(username).isEqualTo(listOf("user1", "user2", "username1", "username2"))
    verify(deliusUserService).getDeliusUsersByEmail("a@b.com")
    verify(authUserService).findAuthUsersByEmail("a@b.com")
    verify(nomisUserService).getNomisUsersByEmail("a@b.com")
    verify(notificationClientApi).sendEmail("emailTemplate", "a@b.com", map, null)
  }
}
