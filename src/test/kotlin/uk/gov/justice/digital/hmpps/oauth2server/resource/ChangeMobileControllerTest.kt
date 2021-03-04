package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.TestingAuthenticationToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.resource.account.ChangeMobileController
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyMobileService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyMobileService.VerifyMobileException
import uk.gov.service.notify.NotificationClientException
import java.util.Optional
import java.util.UUID

class ChangeMobileControllerTest {
  private val userService: UserService = mock()
  private val verifyMobileService: VerifyMobileService = mock()
  private val tokenService: TokenService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val controller =
    ChangeMobileController(userService, verifyMobileService, tokenService, telemetryClient, false)
  private val controllerSmokeEnabled =
    ChangeMobileController(userService, verifyMobileService, tokenService, telemetryClient, true)
  private val authentication = TestingAuthenticationToken(
    UserDetailsImpl("user", "name", setOf(), AuthSource.auth.name, "userid", "jwtId"),
    "pass"
  )
  private val token = UUID.randomUUID().toString()

  @Nested
  inner class ChangeMobileRequest {
    @Test
    fun addMobileRequest_validToken() {
      whenever(userService.getUserWithContacts(anyString())).thenReturn(createSampleUser(username = "user"))
      val view = controller.changeMobileRequest("token", authentication)
      assertThat(view.viewName).isEqualTo("account/changeMobile", "mobile", null)
      verify(userService).getUserWithContacts("user")
    }

    @Test
    fun addMobileRequest_expiredToken() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"))
      val view = controller.changeMobileRequest("token", authentication)
      assertThat(view.viewName).isEqualTo("redirect:/account-details?error=tokenexpired")
    }

    @Test
    fun addMobileRequest_invalidToken() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
      val view = controller.changeMobileRequest("token", authentication)
      assertThat(view.viewName).isEqualTo("redirect:/account-details?error=tokeninvalid")
    }

    @Test
    fun updateMobileRequest_validToken() {
      whenever(userService.getUserWithContacts(anyString())).thenReturn(createSampleUser(mobile = "07700900321"))
      val view = controller.changeMobileRequest("token", authentication)
      assertThat(view.viewName).isEqualTo("account/changeMobile", "mobile", "07700900321")
    }

    @Test
    fun updateMobileRequest_expiredToken() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"))
      val view = controller.changeMobileRequest("token", authentication)
      assertThat(view.viewName).isEqualTo("redirect:/account-details?error=tokenexpired")
    }

    @Test
    fun updateMobileRequest_invalidToken() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
      val view = controller.changeMobileRequest("token", authentication)
      assertThat(view.viewName).isEqualTo("redirect:/account-details?error=tokeninvalid")
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
      val modelAndView = controller.changeMobile(token, "12345", "change", authentication)
      assertThat(modelAndView.viewName).isEqualTo("account/changeMobile")
      assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
        mapOf(
          "error" to "other",
          "mobile" to "12345",
          "requestType" to "change"
        )
      )
      verify(tokenService, never()).removeToken(UserToken.TokenType.ACCOUNT, token)
    }

    @Test
    fun `changeMobile verification exception`() {
      whenever(userService.isSameAsCurrentVerifiedMobile(anyString(), anyString())).thenReturn(false)
      whenever(userService.getUserWithContacts(anyString())).thenReturn(createSampleUser(username = "AUTH_MOBILE"))
      whenever(verifyMobileService.changeMobileAndRequestVerification(anyString(), anyString())).thenThrow(
        VerifyMobileException("something went wrong")
      )
      val modelAndView = controller.changeMobile(token, "12345", "change", authentication)
      assertThat(modelAndView.viewName).isEqualTo("account/changeMobile")
      assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
        mapOf(
          "error" to "something went wrong",
          "mobile" to "12345",
          "requestType" to "change"
        )
      )
      verify(tokenService, never()).removeToken(UserToken.TokenType.ACCOUNT, token)
    }

    @Test
    fun `changeMobile success`() {
      whenever(userService.isSameAsCurrentVerifiedMobile(anyString(), anyString())).thenReturn(false)
      whenever(userService.getUserWithContacts(anyString())).thenReturn(createSampleUser(username = "AUTH_MOBILE"))
      whenever(verifyMobileService.changeMobileAndRequestVerification(anyString(), anyString())).thenReturn("123456")
      val mobile = "07700900321"
      val modelAndView = controller.changeMobile(token, mobile, "change", authentication)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/verify-mobile")
      assertThat(modelAndView.model).isEmpty()
      verify(verifyMobileService).changeMobileAndRequestVerification("user", mobile)
      verify(tokenService, times(1)).removeToken(UserToken.TokenType.ACCOUNT, token)
    }

    @Test
    fun `changeMobile success smoke test`() {
      whenever(userService.isSameAsCurrentVerifiedMobile(anyString(), anyString())).thenReturn(false)
      whenever(userService.getUserWithContacts(anyString())).thenReturn(createSampleUser(username = "AUTH_MOBILE"))
      whenever(verifyMobileService.changeMobileAndRequestVerification(anyString(), anyString())).thenReturn("123456")
      val mobile = "07700900321"
      val modelAndView = controllerSmokeEnabled.changeMobile(token, mobile, "change", authentication)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/verify-mobile")
      assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(mapOf("verifyCode" to "123456"))
      verify(tokenService, times(1)).removeToken(UserToken.TokenType.ACCOUNT, token)
    }

    @Test
    fun `changeMobile already verified`() {
      whenever(userService.isSameAsCurrentVerifiedMobile(anyString(), anyString())).thenReturn(true)
      val modelAndView = controller.changeMobile(token, "07700900321", "change", authentication)
      assertThat(modelAndView.viewName).isEqualTo("redirect:/verify-mobile-already")
      verify(tokenService, times(1)).removeToken(UserToken.TokenType.ACCOUNT, token)
    }
  }
}
