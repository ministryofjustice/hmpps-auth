package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.TestingAuthenticationToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.resource.account.ChangeMobileController
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import java.util.*

class ChangeMobileControllerTest {
  private val userService: UserService = mock()
  private val controller = ChangeMobileController(userService)
  private val token = TestingAuthenticationToken(UserDetailsImpl("user", "name", setOf(), AuthSource.auth.name, null), "pass")

  @Nested
  inner class ChangeMobileRequest {
    @Test
    fun addMobileRequest() {
      whenever(userService.findUser(anyString())).thenReturn(Optional.of(User()))
      val view = controller.changeMobileRequest(token)
      assertThat(view.viewName).isEqualTo("account/changeMobile", "mobile", null)
      verify(userService).findUser("user")
    }

    @Test
    fun updateMobileRequest() {
      val user = User.builder().mobile("07700900321").build()
      whenever(userService.findUser(anyString())).thenReturn(Optional.of(user))
      val view = controller.changeMobileRequest(token)
      assertThat(view.viewName).isEqualTo("account/changeMobile", "mobile", "07700900321")
    }
  }
}
