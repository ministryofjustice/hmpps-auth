@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.oauth2server.resource.account

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.TestingAuthenticationToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import java.util.Optional
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class UseEmailControllerTest {
  private val userService: UserService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val authUserService: AuthUserService = mock()
  private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler = mock()
  private val request: HttpServletRequest = mock()
  private val response: HttpServletResponse = mock()
  private val useEmailController =
    UseEmailController(authUserService, telemetryClient, jwtAuthenticationSuccessHandler, userService)
  private val token = TestingAuthenticationToken(
    UserDetailsImpl("user", "name", setOf(), AuthSource.auth.name, "userid", "jwtId"),
    "pass"
  )

  @Test
  fun useEmailRequest() {
    val view = useEmailController.useEmailRequest(token)

    assertThat(view).isEqualTo("account/useEmail")
  }

  @Nested
  inner class useEmail {
    @Test
    fun `use email no action if not changed`() {
      val view = useEmailController.useEmail(token, request, response)

      verifyZeroInteractions(telemetryClient)
      assertThat(view).isEqualTo("redirect:/account-details")
    }

    @Test
    fun `use email updates jwt`() {
      val authUser = createSampleUser("build", email = "anemail@somewhere.com")
      whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(authUser))

      whenever(authUserService.useEmailAsUsername(anyString())).thenReturn("some@email")
      useEmailController.useEmail(token, request, response)

      verify(jwtAuthenticationSuccessHandler).updateAuthenticationInRequest(
        eq(request),
        eq(response),
        check {
          assertThat(it.principal).isEqualTo(authUser)
        }
      )
    }

    @Test
    fun `use email track event`() {
      val authUser = createSampleUser("build", email = "anemail@somewhere.com")
      whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(authUser))

      whenever(authUserService.useEmailAsUsername(anyString())).thenReturn("some@email")
      useEmailController.useEmail(token, request, response)

      verify(telemetryClient).trackEvent(
        "ChangeUsernameToEmail",
        mapOf("username" to "user", "email" to "some@email"),
        null
      )
    }
  }
}
