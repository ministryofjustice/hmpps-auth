package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.MapEntry
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.springframework.security.authentication.AccountExpiredException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.ChangePasswordService
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.PasswordValidationFailureException
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import java.io.IOException
import java.util.*
import java.util.Map.entry
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ChangePasswordControllerTest {
  private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler = mock()
  private val authenticationManager: AuthenticationManager = mock()
  private val changePasswordService: ChangePasswordService = mock()
  private val userService: UserService = mock()
  private val tokenService: TokenService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val request: HttpServletRequest = mock()
  private val response: HttpServletResponse = mock()
  private val controller = ChangePasswordController(jwtAuthenticationSuccessHandler,
      authenticationManager, changePasswordService, tokenService, userService, telemetryClient, setOf("password1"))

  @Test
  fun changePasswordRequest() {
    val view = controller.changePasswordRequest("token")
    assertThat(view.viewName).isEqualTo("changePassword")
  }

  @Test
  fun changePasswordRequest_adminUser() {
    setupCheckAndGetTokenValid()
    val user = setupGetUserCallForProfile()
    user.accountDetail.profile = "TAG_ADMIN"
    val model = controller.changePasswordRequest("token")
    assertThat(model.model["isAdmin"]).isEqualTo(true)
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
    val model = controller.changePasswordRequest("token")
    assertThat(model.model["isAdmin"]).isEqualTo(false)
  }

  @Test
  @Throws(IOException::class, ServletException::class)
  fun changePasswordRequest_NotAlphanumeric() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile()
    val modelAndView = controller.changePassword("d", "@fewfewfew1", "@fewfewfew1", request, response)
    assertThat(modelAndView.viewName).isEqualTo("changePassword")
    assertThat(modelAndView.model).containsOnly(entry("token", "d"), entry("error", true), listEntry("errornew", "alphanumeric"), entry("isAdmin", false))
  }

  @Test
  @Throws(Exception::class)
  fun changePassword_ValidationFailure() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile()
    doThrow(PasswordValidationFailureException()).whenever(changePasswordService).setPassword(anyString(), anyString())
    val redirect = controller.changePassword("user", "password2", "password2", request, response)
    assertThat(redirect.viewName).isEqualTo("changePassword")
    assertThat(redirect.model).containsOnly(entry("token", "user"), entry("error", true), entry("errornew", "validation"), entry("isAdmin", false))
  }

  @Test
  fun changePassword_OtherException() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile()
    val exception = RuntimeException()
    doThrow(exception).whenever(changePasswordService).setPassword(anyString(), anyString())
    Assertions.assertThatThrownBy { controller.changePassword("user", "password2", "password2", request, response) }.isEqualTo(exception)
  }

  @Test
  @Throws(Exception::class)
  fun changePassword_Success() {
    val token = UsernamePasswordAuthenticationToken("bob", "pass")
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile()
    whenever(authenticationManager.authenticate(any())).thenReturn(token)
    val redirect = controller.changePassword("user", "password2", "password2", request, response)
    assertThat(redirect).isNull()
    val authCapture = ArgumentCaptor.forClass(Authentication::class.java)
    verify(authenticationManager).authenticate(authCapture.capture())
    val value = authCapture.value
    assertThat(value.principal).isEqualTo("USER")
    assertThat(value.credentials).isEqualTo("password2")
    verify(changePasswordService).setPassword("user", "password2")
    verify(jwtAuthenticationSuccessHandler).onAuthenticationSuccess(request, response, token)
  }

  @Test
  @Throws(Exception::class)
  fun changePassword_Success_Telemetry() {
    val token = UsernamePasswordAuthenticationToken("bob", "pass")
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile()
    whenever(authenticationManager.authenticate(any())).thenReturn(token)
    controller.changePassword("user", "password2", "password2", request, response)
    verify(telemetryClient).trackEvent(eq("ChangePasswordSuccess"), check {
      assertThat(it).containsExactly(entry("username", "user"))
    }, isNull())
  }

  @Test
  @Throws(Exception::class)
  fun changePassword_AuthenticateSuccess_Telemetry() {
    val token = UsernamePasswordAuthenticationToken("bob", "pass")
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile()
    whenever(authenticationManager.authenticate(any())).thenReturn(token)
    controller.changePassword("user", "password2", "password2", request, response)
    verify(telemetryClient).trackEvent(eq("ChangePasswordAuthenticateSuccess"), check {
      assertThat(it).containsExactly(entry("username", "user"))
    }, isNull())
  }

  @Test
  @Throws(Exception::class)
  fun changePassword_SuccessAccountExpired() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile()
    whenever(authenticationManager.authenticate(any())).thenThrow(AccountExpiredException("msg"))
    val redirect = controller.changePassword("user", "password2", "password2", request, response)
    assertThat(redirect.viewName).isEqualTo("redirect:/login?error=changepassword")
    val authCapture = ArgumentCaptor.forClass(Authentication::class.java)
    verify(authenticationManager).authenticate(authCapture.capture())
    val value = authCapture.value
    assertThat(value.principal).isEqualTo("USER")
    assertThat(value.credentials).isEqualTo("password2")
    verify(changePasswordService).setPassword("user", "password2")
  }

  @Test
  @Throws(Exception::class)
  fun changePassword_SuccessAccountExpired_TelemetryFailure() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile()
    whenever(authenticationManager.authenticate(any())).thenThrow(AccountExpiredException("msg"))
    controller.changePassword("user", "password2", "password2", request, response)
    verify(telemetryClient).trackEvent(eq("ChangePasswordAuthenticateFailure"), check {
      assertThat(it).containsOnly(entry("username", "user"), entry("reason", "AccountExpiredException"))
    }, isNull())
  }

  @Test
  @Throws(Exception::class)
  fun changePassword_SuccessAccountExpired_TelemetrySuccess() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile()
    whenever(authenticationManager.authenticate(any())).thenThrow(AccountExpiredException("msg"))
    controller.changePassword("user", "password2", "password2", request, response)
    verify(telemetryClient).trackEvent(eq("ChangePasswordSuccess"), check {
      assertThat(it).containsExactly(entry("username", "user"))
    }, isNull())
  }

  private fun setupGetUserCallForProfile(): NomisUserPersonDetails {
    val user = NomisUserPersonDetails()
    user.accountDetail = AccountDetail()
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
    return user
  }

  private fun setupCheckAndGetTokenValid() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(User.of("user").createToken(UserToken.TokenType.RESET)))
  }

  private fun listEntry(key: String, vararg values: String): MapEntry<String, List<String>> {
    return MapEntry.entry(key, values.asList())
  }
}
