package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.TestingAuthenticationToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.resource.account.ChangeMobileController
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyMobileService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyMobileService.VerifyMobileException
import uk.gov.service.notify.NotificationClientException

class ChangeMobileControllerTest {
  private val userService: UserService = mock()
  private val verifyMobileService: VerifyMobileService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val controller = ChangeMobileController(userService, verifyMobileService, telemetryClient, false)
  private val controllerSmokeEnabled = ChangeMobileController(userService, verifyMobileService, telemetryClient, true)
  private val token = TestingAuthenticationToken(
    UserDetailsImpl("user", "name", setOf(), AuthSource.auth.name, "userid", "jwtId"),
    "pass"
  )

  @Nested
  inner class ChangeMobileRequest {
    @Test
    fun addMobileRequest() {
      whenever(userService.getUserWithContacts(anyString())).thenReturn(User())
      val view = controller.changeMobileRequest(token)
      assertThat(view.viewName).isEqualTo("account/changeMobile", "mobile", null)
      verify(userService).getUserWithContacts("user")
    }

    @Test
    fun updateMobileRequest() {
      val user = User.builder().mobile("07700900321").build()
      whenever(userService.getUserWithContacts(anyString())).thenReturn(user)
      val view = controller.changeMobileRequest(token)
      assertThat(view.viewName).isEqualTo("account/changeMobile", "mobile", "07700900321")
    }
  }

  @Nested
  inner class ChangeMobile {
    @Test
    fun `changeMobile notification exception`() {
      whenever(userService.isSameAsCurrentVerifiedMobile(anyString(), anyString())).thenReturn(false)
      whenever(userService.getUserWithContacts(anyString())).thenReturn(createSampleUser(username = "AUTH_MOBILE"))
      whenever(verifyMobileService.changeMobileAndRequestVerification(anyString(), anyString())).thenThrow(
        NotificationClientException("something went wrong")
      )
      val modelAndView = controller.changeMobile("12345", "change", token)
      assertThat(modelAndView.viewName).isEqualTo("account/changeMobile")
      assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
        mapOf(
          "error" to "other",
          "mobile" to "12345",
          "requestType" to "change"
        )
      )
    }

    @Test
    fun `changeMobile verification exception`() {
      whenever(userService.isSameAsCurrentVerifiedMobile(anyString(), anyString())).thenReturn(false)
      whenever(userService.getUserWithContacts(anyString())).thenReturn(createSampleUser(username = "AUTH_MOBILE"))
      whenever(verifyMobileService.changeMobileAndRequestVerification(anyString(), anyString())).thenThrow(
        VerifyMobileException("something went wrong")
      )
      val modelAndView = controller.changeMobile("12345", "change", token)
      assertThat(modelAndView.viewName).isEqualTo("account/changeMobile")
      assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
        mapOf(
          "error" to "something went wrong",
          "mobile" to "12345",
          "requestType" to "change"
        )
      )
    }

    @Test
    fun `changeMobile success`() {
      whenever(userService.isSameAsCurrentVerifiedMobile(anyString(), anyString())).thenReturn(false)
      whenever(userService.getUserWithContacts(anyString())).thenReturn(createSampleUser(username = "AUTH_MOBILE"))
      whenever(verifyMobileService.changeMobileAndRequestVerification(anyString(), anyString())).thenReturn("123456")
      val mobile = "07700900321"
      val modelAndView = controller.changeMobile(mobile, "change", token)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/verify-mobile")
      assertThat(modelAndView.model).isEmpty()
      verify(verifyMobileService).changeMobileAndRequestVerification("user", mobile)
    }

    @Test
    fun `changeMobile success smoke test`() {
      whenever(userService.isSameAsCurrentVerifiedMobile(anyString(), anyString())).thenReturn(false)
      whenever(userService.getUserWithContacts(anyString())).thenReturn(createSampleUser(username = "AUTH_MOBILE"))
      whenever(verifyMobileService.changeMobileAndRequestVerification(anyString(), anyString())).thenReturn("123456")
      val mobile = "07700900321"
      val modelAndView = controllerSmokeEnabled.changeMobile(mobile, "change", token)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/verify-mobile")
      assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(mapOf("verifyCode" to "123456"))
    }

    @Test
    fun `changeMobile already verified`() {
      whenever(userService.isSameAsCurrentVerifiedMobile(anyString(), anyString())).thenReturn(true)
      val modelAndView = controller.changeMobile("07700900321", "change", token)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/verify-mobile-already")
    }
  }
}
