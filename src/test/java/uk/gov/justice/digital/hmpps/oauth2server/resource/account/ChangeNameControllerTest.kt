package uk.gov.justice.digital.hmpps.oauth2server.resource.account

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.TestingAuthenticationToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService.CreateUserException
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import java.util.Optional
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ChangeNameControllerTest {
  private val authUserService: AuthUserService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val userService: UserService = mock()
  private val request: HttpServletRequest = mock()
  private val response: HttpServletResponse = mock()
  private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler = mock()
  private val token = TestingAuthenticationToken(
    UserDetailsImpl("user", "name", setOf(), AuthSource.auth.name, "userid", "jwtId"),
    "pass"
  )

  private val controller: ChangeNameController =
    ChangeNameController(authUserService, telemetryClient, jwtAuthenticationSuccessHandler, userService)

  @Test
  fun changeNameRequest() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(
      Optional.of(
        User.builder().person(Person("first", "last")).build()
      )
    )
    val modelAndView = controller.changeNameRequest(token)
    assertThat(modelAndView.modelMap).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "firstName" to "first",
        "lastName" to "last"
      )
    )
    assertThat(modelAndView.viewName).isEqualTo("account/changeName")
  }

  @Test
  fun `changeName exception`() {
    whenever(authUserService.amendUser(anyString(), anyString(), anyString())).thenThrow(
      CreateUserException(
        "lastName",
        "someerror"
      )
    )
    val modelAndView = controller.changeName("joe", "bloggs", token, request, response)
    assertThat(modelAndView.modelMap).containsExactlyInAnyOrderEntriesOf(
      mapOf("error_lastName" to "someerror", "error" to true, "firstName" to "joe", "lastName" to "bloggs")
    )
    assertThat(modelAndView.viewName).isEqualTo("account/changeName")
  }

  @Test
  fun `changeName success`() {
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(User.of("joe")))

    val modelAndView = controller.changeName("joe", "bloggs", token, request, response)
    assertThat(modelAndView.modelMap).isEmpty()
    assertThat(modelAndView.viewName).isEqualTo("redirect:/account-details")
  }

  @Test
  fun `changeName pass username from token`() {
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(User.of("joe")))

    controller.changeName("joe", "bloggs", token, request, response)
    verify(authUserService).amendUser("user", "joe", "bloggs")
  }

  @Test
  fun `changeName call add authentication to request`() {
    val authorities = setOf(Authority("role", "name"))
    val user = User.builder().username("joe").authorities(authorities).build()
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))

    controller.changeName("joe", "bloggs", token, request, response)
    verify(jwtAuthenticationSuccessHandler).updateAuthenticationInRequest(
      eq(request),
      eq(response),
      check {
        assertThat(it.authorities).containsExactlyInAnyOrderElementsOf(authorities)
        assertThat(it.principal).isEqualTo(user)
      }
    )
  }
}
