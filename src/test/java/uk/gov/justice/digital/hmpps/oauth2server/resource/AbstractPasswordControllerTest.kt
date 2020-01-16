package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.data.MapEntry
import org.assertj.core.util.Arrays
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.springframework.security.authentication.LockedException
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.PasswordValidationFailureException
import uk.gov.justice.digital.hmpps.oauth2server.security.ReusedPasswordException
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import java.util.*
import java.util.Map.entry

class AbstractPasswordControllerTest {
  private val resetPasswordService: ResetPasswordService = mock()
  private val tokenService: TokenService = mock()
  private val userService: UserService = mock()
  private val verifyEmailService: VerifyEmailService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private lateinit var controller: ResetPasswordController
  @Before
  fun setUp() {
    controller = ResetPasswordController(resetPasswordService, tokenService, userService, verifyEmailService, telemetryClient, true, setOf("password1"))
  }

  @Test
  fun setPassword_Success() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile("TAG_ADMIN")
    val modelAndView = controller.setPassword("d", "password123456", "password123456", null)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/reset-password-success")
  }

  @Test
  fun setPassword_Success_Telemetry() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile("TAG_ADMIN")
    controller.setPassword("d", "password123456", "password123456", null)
    verify(telemetryClient).trackEvent(eq("ResetPasswordSuccess"), check {
      assertThat(it).containsExactly(entry("username", "user"))
    }, isNull())
  }

  @Test
  fun setPassword_InitialSuccess_Telemetry() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile("TAG_ADMIN")
    controller.setPassword("d", "password123456", "password123456", true)
    verify(telemetryClient).trackEvent(eq("InitialPasswordSuccess"), check {
      assertThat(it).containsExactly(entry("username", "user"))
    }, isNull())
  }

  @Test
  fun setPassword_Failure() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"))
    val modelAndView = controller.setPassword("sometoken", "new", "confirm", null)
    assertThat(modelAndView.viewName).isEqualTo("resetPassword")
    assertThat(modelAndView.model).containsOnly(entry("error", "expired"))
  }

  @Test
  fun setPassword_NotAlphanumeric() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("d", "@fewfewfew1", "@fewfewfew1", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsOnly(entry("token", "d"), entry("isAdmin", false), entry("error", true), listEntry("errornew", "alphanumeric"))
  }

  @Test
  fun setPassword_NotAlphanumeric_Telemetry() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    controller.setPassword("d", "@fewfewfew1", "@fewfewfew1", null)
    verify(telemetryClient).trackEvent(eq("ResetPasswordFailure"), check {
      assertThat(it).containsOnly(entry("username", "user"), entry("reason", "{errornew=[alphanumeric]}"))
    }, isNull())
  }

  @Test
  fun setPassword_NewBlank() {
    setupCheckAndGetTokenValid()
    val modelAndView = controller.setPassword("d", "", "", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsOnly(entry("token", "d"), entry("isAdmin", false), entry("error", true), listEntry("errornew", "newmissing"), listEntry("errorconfirm", "confirmmissing"))
  }

  @Test
  fun setPassword_ConfirmNewBlank() {
    setupCheckAndGetTokenValid()
    val modelAndView = controller.setPassword("d", "a", "", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsOnly(entry("token", "d"), entry("isAdmin", false), entry("error", true), listEntry("errorconfirm", "confirmmissing"))
  }

  @Test
  fun setPassword_Length() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("d", "qwerqw12", "qwerqw12", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsOnly(entry("token", "d"), entry("isAdmin", false), entry("error", true), listEntry("errornew", "length9"))
  }

  @Test
  fun setPassword_LengthAdmin() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile("TAG_ADMIN")
    val modelAndView = controller.setPassword("d", "qwerqwerqwe12", "qwerqwerqwe12", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsOnly(entry("token", "d"), entry("isAdmin", true), entry("error", true), listEntry("errornew", "length14"))
  }

  @Test
  fun setPassword_TooLong() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("d", "abcdefghij123456789012345678901", "abcdefghij123456789012345678901", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsOnly(entry("token", "d"), entry("isAdmin", false), entry("error", true), listEntry("errornew", "long"))
  }

  @Test
  fun setPassword_Blacklist() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("token", "passWORD1", "passWORD1", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsOnly(entry("token", "token"), entry("isAdmin", false), entry("error", true), listEntry("errornew", "blacklist"))
  }

  @Test
  fun setPassword_ContainsUsername() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("token", "someuser12", "someuser12", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsOnly(entry("token", "token"), entry("isAdmin", false), entry("error", true), listEntry("errornew", "username"))
  }

  @Test
  fun setPassword_FourDistinct() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("d", "as1as1as1", "as1as1as1", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsOnly(entry("token", "d"), entry("isAdmin", false), entry("error", true), listEntry("errornew", "four"))
  }

  @Test
  fun setPassword_MissingDigits() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("daaa", "asdasdasdb", "asdasdasdb", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsOnly(entry("token", "daaa"), entry("isAdmin", false), entry("error", true), listEntry("errornew", "nodigits"))
  }

  @Test
  fun setPassword_OnlyDigits() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("d", "1231231234", "1231231234", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsOnly(entry("token", "d"), entry("isAdmin", false), entry("error", true), listEntry("errornew", "alldigits"))
  }

  @Test
  fun setPassword_MultipleFailures() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("user", "password", "new", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsOnly(entry("token", "user"), entry("isAdmin", false), entry("error", true), listEntry("errorconfirm", "mismatch"), listEntry("errornew", "nodigits", "length9"))
  }

  @Test
  fun setPassword_Mismatch() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("user", "password2", "new", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsOnly(entry("token", "user"), entry("isAdmin", false), entry("error", true), listEntry("errorconfirm", "mismatch"))
  }

  @Test
  fun setPassword_ValidationFailure() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    doThrow(PasswordValidationFailureException()).whenever(resetPasswordService).setPassword(anyString(), anyString())
    val modelAndView = controller.setPassword("user", "password2", "password2", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsOnly(entry("token", "user"), entry("isAdmin", false), entry("error", true), entry("errornew", "validation"))
  }

  @Test
  fun setPassword_OtherException() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val exception = RuntimeException()
    doThrow(exception).whenever(resetPasswordService).setPassword(anyString(), anyString())
    assertThatThrownBy { controller.setPassword("user", "password2", "password2", null) }.isEqualTo(exception)
  }

  @Test
  fun setPassword_ReusedPassword() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    doThrow(ReusedPasswordException()).whenever(resetPasswordService).setPassword(anyString(), anyString())
    val modelAndView = controller.setPassword("user", "password2", "password2", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsOnly(entry("token", "user"), entry("isAdmin", false), entry("error", true), entry("errornew", "reused"))
  }

  @Test
  fun setPassword_LockedAccount() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    doThrow(LockedException("wrong")).whenever(resetPasswordService).setPassword(anyString(), anyString())
    val modelAndView = controller.setPassword("user", "password2", "password2", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsOnly(entry("token", "user"), entry("isAdmin", false), entry("error", true), entry("errornew", "state"))
  }

  private fun setupCheckAndGetTokenValid() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(User.of("user").createToken(UserToken.TokenType.RESET)))
  }

  private fun setupGetUserCallForProfile(profile: String?) {
    val user = NomisUserPersonDetails()
    val detail = AccountDetail()
    detail.profile = profile
    user.accountDetail = detail
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
  }

  private fun listEntry(key: String, vararg values: Any): MapEntry<String, List<Any>> {
    return MapEntry.entry(key, Arrays.asList(values))
  }
}
