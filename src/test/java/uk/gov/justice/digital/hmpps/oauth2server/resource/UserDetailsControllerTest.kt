package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
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
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class UserDetailsControllerTest {
  private val authUserService: AuthUserService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val userService: UserService = mock()
  private val request: HttpServletRequest = mock()
  private val response: HttpServletResponse = mock()
  private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler = mock()
  private val token = TestingAuthenticationToken(UserDetailsImpl("user", "name", setOf(), AuthSource.auth.name, null), "pass")

  private val controller: UserDetailsController = UserDetailsController(authUserService, telemetryClient, jwtAuthenticationSuccessHandler, userService)

  @Test
  fun `user details`() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(User.builder().person(Person("first", "last")).build()))
    val modelAndView = controller.userDetails(token)
    assertThat(modelAndView.modelMap).containsExactlyInAnyOrderEntriesOf(mapOf("firstName" to "first", "lastName" to "last"))
    assertThat(modelAndView.viewName).isEqualTo("userDetails")
  }

  @Test
  fun `changeDetails no first name or last name`() {
    val modelAndView = controller.changeDetails(null, null, token, request, response)
    assertThat(modelAndView.modelMap).containsExactlyInAnyOrderEntriesOf(
        mapOf("error_firstName" to "required", "error_lastName" to "required", "error" to true, "firstName" to null, "lastName" to null))
    assertThat(modelAndView.viewName).isEqualTo("userDetails")
  }

  @Test
  fun `changeDetails blank first name and last name`() {
    val modelAndView = controller.changeDetails("  ", "   ", token, request, response)
    assertThat(modelAndView.modelMap).containsExactlyInAnyOrderEntriesOf(
        mapOf("error_firstName" to "required", "error_lastName" to "required", "error" to true, "firstName" to "  ", "lastName" to "   "))
  }

  @Test
  fun `changeDetails blank first name`() {
    val modelAndView = controller.changeDetails("  ", "bloggs", token, request, response)
    assertThat(modelAndView.modelMap).containsExactlyInAnyOrderEntriesOf(
        mapOf("error_firstName" to "required", "error" to true, "firstName" to "  ", "lastName" to "bloggs"))
  }

  @Test
  fun `changeDetails blank last name`() {
    val modelAndView = controller.changeDetails("joe", "   ", token, request, response)
    assertThat(modelAndView.modelMap).containsExactlyInAnyOrderEntriesOf(
        mapOf("error_lastName" to "required", "error" to true, "firstName" to "joe", "lastName" to "   "))
  }

  @Test
  fun `changeDetails exception`() {
    whenever(authUserService.amendUser(anyString(), anyString(), anyString())).thenThrow(CreateUserException("lastName", "someerror"))
    val modelAndView = controller.changeDetails("joe", "bloggs", token, request, response)
    assertThat(modelAndView.modelMap).containsExactlyInAnyOrderEntriesOf(
        mapOf("error_lastName" to "someerror", "error" to true, "firstName" to "joe", "lastName" to "bloggs"))
    assertThat(modelAndView.viewName).isEqualTo("userDetails")
  }

  @Test
  fun `changeDetails success`() {
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(User.of("joe")))

    val modelAndView = controller.changeDetails("joe", "bloggs", token, request, response)
    assertThat(modelAndView.modelMap).isEmpty()
    assertThat(modelAndView.viewName).isEqualTo("redirect:/")
  }

  @Test
  fun `changeDetails pass username from token`() {
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(User.of("joe")))

    controller.changeDetails("joe", "bloggs", token, request, response)
    verify(authUserService).amendUser("user", "joe", "bloggs")
  }

  @Test
  fun `changeDetails call add authentication to request`() {
    val authorities = setOf(Authority("role", "name"))
    val user = User.builder().username("joe").authorities(authorities).build()
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))

    controller.changeDetails("joe", "bloggs", token, request, response)
    verify(jwtAuthenticationSuccessHandler).addAuthenticationToRequest(eq(request), eq(response), check {
      assertThat(it.authorities).containsExactlyInAnyOrderElementsOf(authorities)
      assertThat(it.principal).isEqualTo(user)
    })
  }
}
