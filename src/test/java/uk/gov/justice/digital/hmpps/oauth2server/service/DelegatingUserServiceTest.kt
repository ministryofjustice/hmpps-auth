package uk.gov.justice.digital.hmpps.oauth2server.service

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.NomisUserService

class DelegatingUserServiceTest {
  private val nomisUserService: NomisUserService = mock()
  private val authUserService: AuthUserService = mock()
  private val deliusUserService: DeliusUserService = mock()
  private val service = DelegatingUserService(nomisUserService, authUserService, deliusUserService)

  @Test
  fun `lock account auth user`() {
    service.lockAccount(User.of("bob"))

    verify(authUserService).lockUser(any())
    verify(nomisUserService, never()).lockAccount(anyString())
  }

  @Test
  fun `lock account nomis user`() {
    val userPersonDetails = NomisUserPersonDetails()
    userPersonDetails.username = "bob"
    service.lockAccount(userPersonDetails)

    verify(authUserService).lockUser(any())
    verify(nomisUserService).lockAccount("bob")
  }

  @Test
  fun `lock account delius user`() {
    service.lockAccount(DeliusUserPersonDetails(username = "bob", userId = "12345", firstName = "F", surname = "L", email = "a@b.com"))

    verify(authUserService).lockUser(any())
    verify(nomisUserService, never()).lockAccount(anyString())
  }

  @Test
  fun `change password with unlock auth user`() {
    val user = User.builder().username("bob").source(AuthSource.auth).build()
    service.changePasswordWithUnlock(user, "pass")

    verify(authUserService).unlockUser(any())
    verify(authUserService).changePassword(user, "pass")
    verify(nomisUserService, never()).changePasswordWithUnlock(anyString(), anyString())
    verify(deliusUserService, never()).changePassword(anyString(), anyString())
  }

  @Test
  fun `change password with unlock nomis user`() {
    val userPersonDetails = NomisUserPersonDetails()
    userPersonDetails.username = "bob"
    service.changePasswordWithUnlock(userPersonDetails, "pass")

    verify(authUserService).unlockUser(any())
    verify(authUserService, never()).changePassword(any(), anyString())
    verify(nomisUserService).changePasswordWithUnlock("bob", "pass")
    verify(deliusUserService, never()).changePassword(anyString(), anyString())
  }

  @Test
  fun `change password with unlock delius user`() {
    service.changePasswordWithUnlock(DeliusUserPersonDetails(username = "bob", userId = "12345", firstName = "F", surname = "L", email = "a@b.com"), "pass")

    verify(authUserService).unlockUser(any())
    verify(authUserService, never()).changePassword(any(), anyString())
    verify(nomisUserService, never()).changePasswordWithUnlock(anyString(), anyString())
    verify(deliusUserService).changePassword("bob", "pass")
  }

  @Test
  fun `change password auth user`() {
    val user = User.builder().username("bob").source(AuthSource.auth).build()
    service.changePassword(user, "pass")

    verify(authUserService).changePassword(user, "pass")
    verify(nomisUserService, never()).changePassword(anyString(), anyString())
    verify(deliusUserService, never()).changePassword(anyString(), anyString())
  }

  @Test
  fun `change password nomis user`() {
    val userPersonDetails = NomisUserPersonDetails()
    userPersonDetails.username = "bob"
    service.changePassword(userPersonDetails, "pass")

    verify(authUserService, never()).changePassword(any(), anyString())
    verify(nomisUserService).changePassword("bob", "pass")
    verify(deliusUserService, never()).changePassword(anyString(), anyString())
  }

  @Test
  fun `change password delius user`() {
    service.changePassword(DeliusUserPersonDetails(username = "bob", userId = "12345", firstName = "F", surname = "L", email = "a@b.com"), "pass")

    verify(authUserService, never()).changePassword(any(), anyString())
    verify(nomisUserService, never()).changePassword(anyString(), anyString())
    verify(deliusUserService).changePassword("bob", "pass")
  }
}
