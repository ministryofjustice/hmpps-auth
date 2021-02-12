package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.springframework.security.authentication.LockedException
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetailsHelper.Companion.createSampleNomisUser
import uk.gov.justice.digital.hmpps.oauth2server.security.PasswordValidationFailureException
import uk.gov.justice.digital.hmpps.oauth2server.security.ReusedPasswordException
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import java.util.Map.entry
import java.util.Optional

class AbstractPasswordControllerTest {
  private val resetPasswordService: ResetPasswordService = mock()
  private val tokenService: TokenService = mock()
  private val userService: UserService = mock()
  private val verifyEmailService: VerifyEmailService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val controller = ResetPasswordController(
    resetPasswordService,
    tokenService,
    userService,
    verifyEmailService,
    telemetryClient,
    true,
    setOf("password1")
  )

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
    verify(telemetryClient).trackEvent(
      eq("ResetPasswordSuccess"),
      check {
        assertThat(it).containsExactly(entry("username", "someuser"))
      },
      isNull()
    )
  }

  @Test
  fun setPassword_InitialSuccess_Telemetry() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile("TAG_ADMIN")
    controller.setPassword("d", "password123456", "password123456", true)
    verify(telemetryClient).trackEvent(
      eq("InitialPasswordSuccess"),
      check {
        assertThat(it).containsExactly(entry("username", "someuser"))
      },
      isNull()
    )
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
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "token" to "d",
        "isAdmin" to false,
        "error" to true,
        "errornew" to listOf("alphanumeric"),
        "username" to "someuser"
      )
    )
  }

  @Test
  fun setPassword_NotAlphanumeric_Telemetry() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    controller.setPassword("d", "@fewfewfew1", "@fewfewfew1", null)
    verify(telemetryClient).trackEvent(
      eq("ResetPasswordFailure"),
      check {
        assertThat(it).containsOnly(entry("username", "someuser"), entry("reason", "{errornew=[alphanumeric]}"))
      },
      isNull()
    )
  }

  @Test
  fun setPassword_NewBlank() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("d", "", "", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "token" to "d",
        "isAdmin" to false,
        "error" to true,
        "errornew" to listOf("newmissing"),
        "errorconfirm" to listOf("confirmmissing"),
        "username" to "someuser"
      )
    )
  }

  @Test
  fun setPassword_ConfirmNewBlank() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("d", "a", "", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "token" to "d",
        "isAdmin" to false,
        "error" to true,
        "errorconfirm" to listOf("confirmmissing"),
        "username" to "someuser"
      )
    )
  }

  @Test
  fun setPassword_Length() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("d", "qwerqw12", "qwerqw12", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "token" to "d",
        "isAdmin" to false,
        "error" to true,
        "errornew" to listOf("length9"),
        "username" to "someuser"
      )
    )
  }

  @Test
  fun setPassword_LengthAdmin() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile("TAG_ADMIN")
    val modelAndView = controller.setPassword("d", "qwerqwerqwe12", "qwerqwerqwe12", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "token" to "d",
        "isAdmin" to true,
        "error" to true,
        "errornew" to listOf("length14"),
        "username" to "someuser"
      )
    )
  }

  @Test
  fun setPassword_TooLong() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView =
      controller.setPassword("d", "abcdefghij123456789012345678901", "abcdefghij123456789012345678901", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "token" to "d",
        "isAdmin" to false,
        "error" to true,
        "errornew" to listOf("long"),
        "username" to "someuser"
      )
    )
  }

  @Test
  fun setPassword_Denylist() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("token", "passWORD1", "passWORD1", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "token" to "token",
        "isAdmin" to false,
        "error" to true,
        "errornew" to listOf("denylist"),
        "username" to "someuser"
      )
    )
  }

  @Test
  fun setPassword_ContainsUsername() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("token", "someuser12", "someuser12", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "token" to "token",
        "isAdmin" to false,
        "error" to true,
        "errornew" to listOf("username"),
        "username" to "someuser"
      )
    )
  }

  @Test
  fun setPassword_FourDistinct() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("d", "as1as1as1", "as1as1as1", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "token" to "d",
        "isAdmin" to false,
        "error" to true,
        "errornew" to listOf("four"),
        "username" to "someuser"
      )
    )
  }

  @Test
  fun setPassword_MissingDigits() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("daaa", "asdasdasdb", "asdasdasdb", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "token" to "daaa",
        "isAdmin" to false,
        "error" to true,
        "errornew" to listOf("nodigits"),
        "username" to "someuser"
      )
    )
  }

  @Test
  fun setPassword_OnlyDigits() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("d", "1231231234", "1231231234", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "token" to "d",
        "isAdmin" to false,
        "error" to true,
        "errornew" to listOf("alldigits"),
        "username" to "someuser"
      )
    )
  }

  @Test
  fun setPassword_MultipleFailures() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("user", "password", "new", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "token" to "user",
        "isAdmin" to false,
        "error" to true,
        "errorconfirm" to listOf("mismatch"),
        "errornew" to listOf("nodigits", "length9"),
        "username" to "someuser"
      )
    )
  }

  @Test
  fun setPassword_Mismatch() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    val modelAndView = controller.setPassword("user", "password2", "new", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "token" to "user",
        "isAdmin" to false,
        "error" to true,
        "errorconfirm" to listOf("mismatch"),
        "username" to "someuser"
      )
    )
  }

  @Test
  fun setPassword_ValidationFailure() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    doThrow(PasswordValidationFailureException()).whenever(resetPasswordService).setPassword(anyString(), anyString())
    val modelAndView = controller.setPassword("user", "password2", "password2", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "token" to "user",
        "isAdmin" to false,
        "error" to true,
        "errornew" to "validation",
        "username" to "someuser"
      )
    )
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
  fun initialPassword_ReusedPassword() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    doThrow(ReusedPasswordException()).whenever(resetPasswordService).setPassword(anyString(), anyString())
    val modelAndView = controller.setPassword("user", "password2", "password2", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "token" to "user",
        "isAdmin" to false,
        "error" to true,
        "errornew" to "reused",
        "username" to "someuser"
      )
    )
  }

  @Test
  fun initialPassword_LockedAccount() {
    setupCheckAndGetTokenValid()
    setupGetUserCallForProfile(null)
    doThrow(LockedException("wrong")).whenever(resetPasswordService).setPassword(anyString(), anyString())
    val modelAndView = controller.setPassword("user", "password2", "password2", null)
    assertThat(modelAndView.viewName).isEqualTo("setPassword")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "token" to "user",
        "isAdmin" to false,
        "error" to true,
        "errornew" to "state",
        "username" to "someuser"
      )
    )
  }

  private fun setupCheckAndGetTokenValid() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
    whenever(tokenService.getToken(any(), anyString())).thenReturn(
      Optional.of(
        createSampleUser(username = "someuser").createToken(UserToken.TokenType.RESET)
      )
    )
  }

  private fun setupGetUserCallForProfile(profile: String?) {
    val user = if (profile.isNullOrBlank()) createSampleNomisUser() else createSampleNomisUser(profile)
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
  }
}
