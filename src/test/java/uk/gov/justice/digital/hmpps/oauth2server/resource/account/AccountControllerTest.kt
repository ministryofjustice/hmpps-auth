package uk.gov.justice.digital.hmpps.oauth2server.resource.account

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.TestingAuthenticationToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import java.util.*

class AccountControllerTest {
  private val userService: UserService = mock()
  private val accountController = AccountController(userService)
  private val token = TestingAuthenticationToken(UserDetailsImpl("user", "name", setOf(), AuthSource.auth.name, "userid", "jwtId"), "pass")

  @Test
  fun `account details`() {
    val user = User.of("master")
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
    val authUser = User.of("build")
    whenever(userService.getUserWithContacts(anyString())).thenReturn(authUser)

    val modelAndView = accountController.accountDetails(token)

    assertThat(modelAndView.viewName).isEqualTo("account/accountDetails")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(mapOf("user" to user, "authUser" to authUser, "mfaPreferenceVerified" to false))
    verify(userService).findMasterUserPersonDetails("user")
    verify(userService).getUserWithContacts("user")
  }
}
