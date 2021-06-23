package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.oauth2server.service.ForgottenUsernameService
import uk.gov.justice.digital.hmpps.oauth2server.service.NotificationClientRuntimeException
import uk.gov.service.notify.NotificationClientException
import java.util.Map.entry
import javax.servlet.http.HttpServletRequest

class ForgottenUsernameControllerTest {
  private val forgottenUsernameService: ForgottenUsernameService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val request: HttpServletRequest = mock()
  private val controller = ForgottenUsernameController(
    forgottenUsernameService,
    telemetryClient,
    true,
  )

  @Test
  fun ` forgotten Username Request`() {
    assertThat(controller.forgottenUsernameRequest()).isEqualTo("forgottenUsername")
  }

  @Test
  fun `forgotten Username Request missing`() {
    val modelAndView = controller.forgottenUsername("   ", request)
    assertThat(modelAndView.viewName).isEqualTo("forgottenUsername")
    assertThat(modelAndView.model).containsExactly(entry("error", "missing"))
    verify(telemetryClient).trackEvent(
      "ForgotUsernameRequestFailure",
      mapOf("error" to "missing email"),
      null
    )
  }

  @Test
  fun `forgotten Username Request not email`() {
    val modelAndView = controller.forgottenUsername("bob", request)
    assertThat(modelAndView.viewName).isEqualTo("forgottenUsername")
    assertThat(modelAndView.model).containsExactly(entry("error", "notEmail"))
    verify(telemetryClient).trackEvent(
      "ForgotUsernameRequestFailure",
      mapOf("error" to "not email"),
      null
    )
  }

  @Test
  fun `forgotten Username Request success Smoke With username`() {
    whenever(request.requestURL).thenReturn(StringBuffer("someurl/forgotten-username"))
    whenever(forgottenUsernameService.forgottenUsername(anyString(), anyString())).thenReturn(
      listOf("user1")
    )
    val modelAndView = controller.forgottenUsername("bob@justice.gov.uk", request)
    assertThat(modelAndView.viewName).isEqualTo("forgottenUsernameEmailSent")
    assertThat(modelAndView.model).containsExactly(entry("usernames", listOf("user1")))
    verify(telemetryClient).trackEvent(
      "ForgottenUsernameRequestSuccess",
      mapOf("email" to "bob@justice.gov.uk"),
      null
    )
  }

  @Test
  fun `forgotten Username Request success Smoke With no usernames`() {
    whenever(request.requestURL).thenReturn(StringBuffer("someurl/forgotten-username"))
    whenever(forgottenUsernameService.forgottenUsername(anyString(), anyString())).thenReturn(
      listOf()
    )
    val modelAndView = controller.forgottenUsername("bob@justice.gov.uk", request)
    assertThat(modelAndView.viewName).isEqualTo("forgottenUsernameEmailSent")
    assertThat(modelAndView.model).containsExactly(entry("usernamesMissing", true))
    verify(telemetryClient).trackEvent(
      "ForgottenUsernameRequestFailure",
      mapOf("email" to "bob@justice.gov.uk", "error" to "no usernames found"),
      null
    )
  }

  @Test
  fun `forgotten Username Request notify email failed`() {
    whenever(request.requestURL).thenReturn(StringBuffer("someurl/forgotten-username"))
    whenever(forgottenUsernameService.forgottenUsername(anyString(), anyString())).thenThrow(
      NotificationClientRuntimeException(NotificationClientException("failure message"))
    )

    val modelAndView = controller.forgottenUsername("bob@justice.gov.uk", request)
    assertThat(modelAndView.viewName).isEqualTo("forgottenUsername")
    assertThat(modelAndView.model).containsExactly(entry("error", "other"))

    verify(telemetryClient).trackEvent(
      "ForgottenUsernameRequestFailure",
      mapOf("email" to "bob@justice.gov.uk", "error" to "NotificationClientRuntimeException"),
      null
    )
  }
}
