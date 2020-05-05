package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.check
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.isNull
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.security.authentication.AccountExpiredException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.ChangePasswordService
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.PasswordValidationFailureException
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.security.UserStateAuthenticationFailureHandler
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import java.util.*
import java.util.Map.entry
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ChangePasswordControllerTest {
  private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler = mock()
  private val userStateAuthenticationFailureHandler: UserStateAuthenticationFailureHandler = mock()
  private val authenticationManager: AuthenticationManager = mock()
  private val changePasswordService: ChangePasswordService = mock()
  private val userService: UserService = mock()
  private val tokenService: TokenService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val request: HttpServletRequest = mock()
  private val response: HttpServletResponse = mock()
  private val controller = ChangePasswordController(jwtAuthenticationSuccessHandler, userStateAuthenticationFailureHandler,
      authenticationManager, changePasswordService, tokenService, userService, telemetryClient, setOf("password1"))

  @Nested
  inner class NewPasswordRequest {
    @Test
    fun newPasswordRequest() {
      setupGetToken()
      setupGetUserCallForProfile()
      val view = controller.newPasswordRequest("token")
      assertThat(view.viewName).isEqualTo("changePassword")
    }

    @Test
    fun `newPasswordRequest expired route`() {
      setupGetToken()
      setupGetUserCallForProfile()
      val model = controller.newPasswordRequest("token")
      assertThat(model.model).doesNotContainKey("expired")
    }

    @Test
    fun `newPasswordRequest no token`() {
      val model = controller.newPasswordRequest("  ")
      assertThat(view.viewName).isEqualTo("redirect:/")
      verifyZeroInteractions(tokenService)
    }
  }

  @Nested
  inner class ChangePasswordRequest {
    @Test
    fun changePasswordRequest() {
      setupGetToken()
      setupGetUserCallForProfile()
      val view = controller.changePasswordRequest("token")
      assertThat(view.viewName).isEqualTo("changePassword")
    }

    @Test
    fun changePasswordRequest_adminUser() {
      setupCheckAndGetTokenValid()
      setupGetUserCallForProfile()
      val user = setupGetUserCallForProfile()
      user.accountDetail.profile = "TAG_ADMIN"
      val model = controller.changePasswordRequest("token")
      assertThat(model.model["isAdmin"]).isEqualTo(true)
    }

    @Test
    fun `changePasswordRequest expired route`() {
      setupGetToken()
      setupGetUserCallForProfile()
      val model = controller.changePasswordRequest("token")
      assertThat(model.model["expired"]).isEqualTo(true)
    }

    @Test
    fun changePasswordRequest_generalUser() {
      setupCheckAndGetTokenValid()
      setupGetUserCallForProfile()
      val model = controller.changePasswordRequest("token")
      assertThat(model.model["isAdmin"]).isEqualTo(false)
    }

    @Test
    fun changePasswordRequest_tokenInvalid() {
      setupGetToken()
      setupGetUserCallForProfile()
      val model = controller.changePasswordRequest("token")
      assertThat(model.model["isAdmin"]).isEqualTo(false)
    }

    @Test
    fun `changePasswordRequest no token`() {
      val model = controller.changePasswordRequest("  ")
      assertThat(view.viewName).isEqualTo("redirect:/")
      verifyZeroInteractions(tokenService)
    }
  }

  @Nested
  inner class ChangePassword {
    @Test
    fun changePassword_NotAlphanumeric() {
      setupCheckAndGetTokenValid()
      setupGetUserCallForProfile()
      val modelAndView = controller.changePassword("d", "@fewfewfew1", "@fewfewfew1", request, response, true)
      assertThat(modelAndView!!.viewName).isEqualTo("changePassword")
      assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(mapOf("token" to "d", "isAdmin" to false, "error" to true, "errornew" to listOf("alphanumeric"), "expired" to true, "username" to "someuser"))
    }

    @Test
    fun changePassword_NotExpired() {
      setupCheckAndGetTokenValid()
      setupGetUserCallForProfile()
      val modelAndView = controller.changePassword("d", "@fewfewfew1", "@fewfewfew1", request, response, false)
      assertThat(modelAndView!!.model).containsEntry("expired", false)
    }

    @Test
    fun changePassword_ExpiredNotSet() {
      setupCheckAndGetTokenValid()
      setupGetUserCallForProfile()
      val modelAndView = controller.changePassword("d", "@fewfewfew1", "@fewfewfew1", request, response, null)
      assertThat(modelAndView!!.model).containsEntry("expired", null)
    }

    @Test
    fun changePassword_ValidationFailure() {
      setupCheckAndGetTokenValid()
      setupGetUserCallForProfile()
      doThrow(PasswordValidationFailureException()).whenever(changePasswordService).setPassword(anyString(), anyString())
      val redirect = controller.changePassword("user", "password2", "password2", request, response, true)
      assertThat(redirect!!.viewName).isEqualTo("changePassword")
      assertThat(redirect.model).containsExactlyInAnyOrderEntriesOf(mapOf("token" to "user", "isAdmin" to false, "error" to true, "errornew" to "validation", "expired" to true, "username" to "someuser"))
    }

    @Test
    fun changePassword_OtherException() {
      setupCheckAndGetTokenValid()
      setupGetUserCallForProfile()
      val exception = RuntimeException()
      doThrow(exception).whenever(changePasswordService).setPassword(anyString(), anyString())
      assertThatThrownBy { controller.changePassword("user", "password2", "password2", request, response, true) }.isEqualTo(exception)
    }

    @Test
    fun changePassword_Success() {
      val token = UsernamePasswordAuthenticationToken("bob", "pass")
      setupCheckAndGetTokenValid()
      setupGetUserCallForProfile()
      whenever(authenticationManager.authenticate(any())).thenReturn(token)
      val redirect = controller.changePassword("user", "password2", "password2", request, response, true)
      assertThat(redirect).isNull()
      verify(authenticationManager).authenticate(check {
        assertThat(it.principal).isEqualTo("SOMEUSER")
        assertThat(it.credentials).isEqualTo("password2")
      })
      verify(changePasswordService).setPassword("user", "password2")
      verify(jwtAuthenticationSuccessHandler).onAuthenticationSuccess(request, response, token)
    }

    @Test
    fun changePassword_Success_Telemetry() {
      val token = UsernamePasswordAuthenticationToken("bob", "pass")
      setupCheckAndGetTokenValid()
      setupGetUserCallForProfile()
      whenever(authenticationManager.authenticate(any())).thenReturn(token)
      controller.changePassword("user", "password2", "password2", request, response, true)
      verify(telemetryClient).trackEvent(eq("ChangePasswordSuccess"), check {
        assertThat(it).containsExactly(entry("username", "someuser"))
      }, isNull())
    }

    @Test
    fun changePassword_AuthenticateSuccess_Telemetry() {
      val token = UsernamePasswordAuthenticationToken("bob", "pass")
      setupCheckAndGetTokenValid()
      setupGetUserCallForProfile()
      whenever(authenticationManager.authenticate(any())).thenReturn(token)
      controller.changePassword("user", "password2", "password2", request, response, true)
      verify(telemetryClient).trackEvent(eq("ChangePasswordAuthenticateSuccess"), check {
        assertThat(it).containsExactly(entry("username", "someuser"))
      }, isNull())
    }

    @Test
    fun changePassword_SuccessAccountExpired() {
      setupCheckAndGetTokenValid()
      setupGetUserCallForProfile()
      val e = AccountExpiredException("msg")
      whenever(authenticationManager.authenticate(any())).thenThrow(e)
      val redirect = controller.changePassword("user", "password2", "password2", request, response, true)
      assertThat(redirect).isNull()
      verify(userStateAuthenticationFailureHandler).onAuthenticationFailureForUsername(request, response, e, "someuser")
      verify(authenticationManager).authenticate(check {
        assertThat(it.principal).isEqualTo("SOMEUSER")
        assertThat(it.credentials).isEqualTo("password2")
      })
      verify(changePasswordService).setPassword("user", "password2")
    }


    @Test
    fun changePassword_SuccessAccountNotExpired() {
      setupCheckAndGetTokenValid()
      setupGetUserCallForProfile()
      val redirect = controller.changePassword("user", "password2", "password2", request, response, null)
      assertThat(redirect!!.viewName).isEqualTo("redirect:/change-password-success")
      verify(changePasswordService).setPassword("user", "password2")
      verifyNoInteractions(userStateAuthenticationFailureHandler, authenticationManager)
    }

    @Test
    fun changePassword_SuccessAccountExpired_TelemetrySuccess() {
      setupCheckAndGetTokenValid()
      setupGetUserCallForProfile()
      whenever(authenticationManager.authenticate(any())).thenThrow(AccountExpiredException("msg"))
      controller.changePassword("user", "password2", "password2", request, response, true)
      verify(telemetryClient).trackEvent(eq("ChangePasswordSuccess"), check {
        assertThat(it).containsExactly(entry("username", "someuser"))
      }, isNull())
    }
  }

  @Nested
  inner class ChangePasswordSuccess {

    @Test
    fun `changePasswordSuccess view`() {
      val view = controller.changePasswordSuccess()
      assertThat(view).isEqualTo("changePasswordSuccess")
    }
  }

  private fun setupGetUserCallForProfile(): NomisUserPersonDetails {
    val user = NomisUserPersonDetails()
    user.accountDetail = AccountDetail()
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
    return user
  }

  private fun setupCheckAndGetTokenValid() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(User.of("someuser").createToken(UserToken.TokenType.RESET)))
  }

  private fun setupGetToken() {
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(User.of("someuser").createToken(UserToken.TokenType.RESET)))
  }
}
