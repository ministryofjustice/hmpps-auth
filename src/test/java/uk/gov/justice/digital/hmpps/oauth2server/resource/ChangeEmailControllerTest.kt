package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import java.util.*

class ChangeEmailControllerTest {
  private val userService: UserService = mock()
  private val tokenService: TokenService = mock()
  private val controller = ChangeEmailController(tokenService, userService)

  @Nested
  inner class NewEmailRequest {
    @Test
    fun newPasswordRequest() {
      setupGetToken()
      setupGetUserCallForProfile()
      val view = controller.newEmailRequest("token")
      assertThat(view.viewName).isEqualTo("changeEmail")
    }
  }

  private fun setupGetUserCallForProfile(): NomisUserPersonDetails {
    val user = NomisUserPersonDetails()
    user.accountDetail = AccountDetail()
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
    return user
  }

  private fun setupGetToken() {
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(User.of("someuser").createToken(UserToken.TokenType.RESET)))
  }
}
