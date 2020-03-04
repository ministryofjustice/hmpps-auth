package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService
import java.util.*

class MfaPreferenceControllerTest {

  private val userService: UserService = mock()
  private val mfaService: MfaService = mock()
  private val controller = MfaPreferenceController(userService, mfaService)
  private val authentication = UsernamePasswordAuthenticationToken("bob", "pass")

  @Test
  fun `mfaPreferenceRequest check view`() {
    val user = User.builder().mobile("07700900321").email("someuser").mfaPreference(User.MfaPreferenceType.EMAIL).build()
    whenever(userService.findUser(anyString())).thenReturn(Optional.of(user))
    val modelAndView = controller.mfaPreferenceRequest(authentication)
    assertThat(modelAndView.viewName).isEqualTo("mfaPreference")
  }

  @Test
  fun `mfaPreferenceRequest check model`() {
    val user = User.builder().mobile("07700900321").email("someuser").mfaPreference(User.MfaPreferenceType.EMAIL).build()
    whenever(userService.findUser(anyString())).thenReturn(Optional.of(user))
    val modelAndView = controller.mfaPreferenceRequest(authentication)
    assertThat(modelAndView.model).containsOnly(entry("current", User.MfaPreferenceType.EMAIL), entry("email", "someuser"), entry("text", "07700900321"))
  }

  @Test
  fun `mfaPreference successfulUpdate`() {
    val modelAndView = controller.mfaPreference(User.MfaPreferenceType.TEXT, authentication)
    assertThat(modelAndView).isEqualTo("mfaPreferenceConfirm")
  }
}
