package uk.gov.justice.digital.hmpps.oauth2server.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetailsHelper.Companion.createSampleNomisUser
import uk.gov.justice.digital.hmpps.oauth2server.security.NomisUserService

class DelegatingUserServiceTest {
  private val nomisUserService: NomisUserService = mock()
  private val authUserService: AuthUserService = mock()
  private val deliusUserService: DeliusUserService = mock()
  private val service = DelegatingUserService(nomisUserService, authUserService, deliusUserService)

  @Test
  fun `lock account auth user`() {
    service.lockAccount(createSampleUser(username = "bob"))

    verify(authUserService).lockUser(any())
    verify(nomisUserService, never()).lockAccount(anyString())
  }

  @Test
  fun `lock account nomis user`() {
    service.lockAccount(createSampleNomisUser())

    verify(authUserService).lockUser(any())
    verify(nomisUserService).lockAccount("bob")
  }

  @Test
  fun `lock account delius user`() {
    service.lockAccount(
      DeliusUserPersonDetails(
        username = "bob",
        userId = "12345",
        firstName = "F",
        surname = "L",
        email = "a@b.com"
      )
    )

    verify(authUserService).lockUser(any())
    verify(nomisUserService, never()).lockAccount(anyString())
  }

  @Test
  fun `change password with unlock auth user`() {
    val user = createSampleUser(username = "bob")
    service.changePasswordWithUnlock(user, "pass")

    verify(authUserService).unlockUser(any())
    verify(authUserService).changePassword(user, "pass")
    verify(nomisUserService, never()).changePasswordWithUnlock(anyString(), anyString())
    verify(deliusUserService, never()).changePassword(anyString(), anyString())
  }

  @Test
  fun `change password with unlock nomis user`() {
    service.changePasswordWithUnlock(createSampleNomisUser(), "pass")

    verify(authUserService).unlockUser(any())
    verify(authUserService, never()).changePassword(any(), anyString())
    verify(nomisUserService).changePasswordWithUnlock("bob", "pass")
    verify(deliusUserService, never()).changePassword(anyString(), anyString())
  }

  @Test
  fun `change password with unlock delius user`() {
    service.changePasswordWithUnlock(
      DeliusUserPersonDetails(
        username = "bob",
        userId = "12345",
        firstName = "F",
        surname = "L",
        email = "a@b.com"
      ),
      "pass"
    )

    verify(authUserService).unlockUser(any())
    verify(authUserService, never()).changePassword(any(), anyString())
    verify(nomisUserService, never()).changePasswordWithUnlock(anyString(), anyString())
    verify(deliusUserService).changePassword("bob", "pass")
  }

  @Test
  fun `change password auth user`() {
    val user = createSampleUser(username = "bob")
    service.changePassword(user, "pass")

    verify(authUserService).changePassword(user, "pass")
    verify(nomisUserService, never()).changePassword(anyString(), anyString())
    verify(deliusUserService, never()).changePassword(anyString(), anyString())
  }

  @Test
  fun `change password nomis user`() {
    service.changePassword(createSampleNomisUser(), "pass")

    verify(authUserService, never()).changePassword(any(), anyString())
    verify(nomisUserService).changePassword("bob", "pass")
    verify(deliusUserService, never()).changePassword(anyString(), anyString())
  }

  @Test
  fun `change password delius user`() {
    service.changePassword(
      DeliusUserPersonDetails(
        username = "bob",
        userId = "12345",
        firstName = "F",
        surname = "L",
        email = "a@b.com"
      ),
      "pass"
    )

    verify(authUserService, never()).changePassword(any(), anyString())
    verify(nomisUserService, never()).changePassword(anyString(), anyString())
    verify(deliusUserService).changePassword("bob", "pass")
  }
}
