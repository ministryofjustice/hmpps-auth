package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService

class MfaPreferenceControllerTest {
  private val userService: UserService = mock()
  private val mfaService: MfaService = mock()
  private val controller = MfaPreferenceController(userService, mfaService)
  private val authentication = UsernamePasswordAuthenticationToken("bob", "pass")

  @Test
  fun `mfaPreferenceRequest check view`() {
    val user =
      createSampleUser(email = "someuser", mobile = "07700900321", mfaPreference = User.MfaPreferenceType.EMAIL)
    whenever(userService.getUserWithContacts(anyString())).thenReturn(user)
    val modelAndView = controller.mfaPreferenceRequest(authentication)
    assertThat(modelAndView.viewName).isEqualTo("mfaPreference")
  }

  @Test
  fun `mfaPreferenceRequest check model`() {
    val user =
      createSampleUser(email = "someuser", mobile = "07700900321", mfaPreference = User.MfaPreferenceType.EMAIL)
    whenever(userService.getUserWithContacts(anyString())).thenReturn(user)
    val modelAndView = controller.mfaPreferenceRequest(authentication)
    assertThat(modelAndView.model).containsOnly(
      entry("current", User.MfaPreferenceType.EMAIL),
      entry("email", "someuser"),
      entry("secondaryemail", null),
      entry("text", "07700900321")
    )
  }

  @Test
  fun `mfaPreferenceRequest check model containing all preferences`() {
    val user =
      createSampleUser(email = "someuser", mobile = "07700900321", mobileVerified = true, secondaryEmail = "secondaryEmail", secondaryEmailVerified = true, mfaPreference = User.MfaPreferenceType.EMAIL)
    whenever(userService.getUserWithContacts(anyString())).thenReturn(user)
    val modelAndView = controller.mfaPreferenceRequest(authentication)
    assertThat(modelAndView.model).containsOnly(
      entry("current", User.MfaPreferenceType.EMAIL),
      entry("email", "someuser"),
      entry("secondaryemail", "secondaryEmail"),
      entry("text", "07700900321")
    )
  }

  @Test
  fun `mfaPreference successfulUpdate`() {
    val modelAndView = controller.mfaPreference(User.MfaPreferenceType.TEXT, authentication)
    assertThat(modelAndView).isEqualTo("redirect:/account-details")
  }
}
