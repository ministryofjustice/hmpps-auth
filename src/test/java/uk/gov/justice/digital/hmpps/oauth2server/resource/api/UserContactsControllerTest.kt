package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService

class UserContactsControllerTest {
  private val userService: UserService = mock()
  private val userContactsController = UserContactsController(userService)

  @Test
  fun `no verified contact`() {
    whenever(userService.getUserWithContacts(anyString())).thenReturn(
      createSampleUser(secondaryEmail = "email not verified", mobile = "mobile not verified")
    )
    val response = userContactsController.contacts("bob")
    assertThat(response).isEmpty()
  }

  @Test
  fun `verified contacts`() {
    whenever(userService.getUserWithContacts(anyString())).thenReturn(
      createSampleUser(secondaryEmail = "email not verified", secondaryEmailVerified = true, mobile = "mobile not verified", mobileVerified = true)
    )
    val response = userContactsController.contacts("bob")
    assertThat(response).containsExactlyInAnyOrder(
      ContactDto("email verified", "SECONDARY_EMAIL", "Secondary email"),
      ContactDto("mobile verified", "MOBILE_PHONE", "Mobile phone")
    )
  }
}
