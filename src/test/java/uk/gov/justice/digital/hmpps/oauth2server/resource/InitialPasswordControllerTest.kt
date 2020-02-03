package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import java.util.*

class InitialPasswordControllerTest {
  private val resetPasswordService: ResetPasswordService = mock()
  private val tokenService: TokenService = mock()
  private val userService: UserService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val controller = InitialPasswordController(resetPasswordService, tokenService, userService, telemetryClient, setOf("password1"))

  @Nested
  inner class InitialPasswordSuccess {
    @Test
    fun initialPasswordSuccess_checkView() {
      val modelAndView = controller.initialPasswordSuccess()
      assertThat(modelAndView).isEqualTo("initialPasswordSuccess")
    }
  }

  @Nested
  inner class InitialPassword {
    @Test
    fun initialPassword_checkView() {
      setupGetUserCallForProfile()
      setupCheckAndGetTokenValid()
      val modelAndView = controller.initialPassword("token")
      assertThat(modelAndView.viewName).isEqualTo("setPassword")
    }

    @Test
    fun initialPassword_checkModel() {
      setupCheckAndGetTokenValid()
      setupGetUserCallForProfile()
      val modelAndView = controller.initialPassword("sometoken")
      assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(mapOf("token" to "sometoken", "isAdmin" to false, "initial" to true, "username" to "someuser"))
    }

    @Test
    fun initialPassword_checkModelAdminUser() {
      val user = setupGetUserCallForProfile()
      user.accountDetail.profile = "TAG_ADMIN"
      setupCheckAndGetTokenValid()
      val modelAndView = controller.initialPassword("sometoken")
      assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(mapOf("token" to "sometoken", "isAdmin" to true, "initial" to true, "username" to "someuser"))
    }

    @Test
    fun initialPassword_FailureCheckView() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"))
      val modelAndView = controller.initialPassword("token")
      assertThat(modelAndView.viewName).isEqualTo("resetPassword")
    }

    @Test
    fun initialPassword_FailureCheckModel() {
      whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"))
      val modelAndView = controller.initialPassword("sometoken")
      assertThat(modelAndView.model).containsOnly(entry("error", "expired"))
    }
  }

  private fun setupCheckAndGetTokenValid() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty())
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(User.of("someuser").createToken(UserToken.TokenType.RESET)))
  }

  private fun setupGetUserCallForProfile(): NomisUserPersonDetails {
    val user = NomisUserPersonDetails()
    user.accountDetail = AccountDetail()
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
    return user
  }
}
