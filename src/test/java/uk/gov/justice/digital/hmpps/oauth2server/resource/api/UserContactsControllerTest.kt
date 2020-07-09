package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Contact
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ContactType.MOBILE_PHONE
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ContactType.SECONDARY_EMAIL
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService

class UserContactsControllerTest {
  private val userService: UserService = mock()
  private val userContactsController = UserContactsController(userService)

  @Test
  fun `no verified contact`() {
    whenever(userService.getUserWithContacts(anyString())).thenReturn(User.builder().contacts(setOf(
        Contact(SECONDARY_EMAIL, "email not verified"),
        Contact(MOBILE_PHONE, "mobile not verified")
    )).build())
    val response = userContactsController.contacts("bob")
    assertThat(response).isEmpty()
  }

  @Test
  fun `verified contacts`() {
    whenever(userService.getUserWithContacts(anyString())).thenReturn(User.builder().contacts(setOf(
        Contact(SECONDARY_EMAIL, "email verified", true),
        Contact(MOBILE_PHONE, "mobile verified", true)
    )).build())
    val response = userContactsController.contacts("bob")
    assertThat(response).containsExactlyInAnyOrder(
        ContactDto("email verified", "SECONDARY_EMAIL", "Secondary email"),
        ContactDto("mobile verified", "MOBILE_PHONE", "Mobile phone"))
  }
}
