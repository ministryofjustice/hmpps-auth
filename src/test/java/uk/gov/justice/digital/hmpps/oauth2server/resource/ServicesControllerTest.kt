package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.TestingAuthenticationToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.auth
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.service.AuthServicesService

class ServicesControllerTest {
  private val authServicesService: AuthServicesService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val controller = ServicesController(authServicesService, telemetryClient)
  private val authentication =
    TestingAuthenticationToken(UserDetailsImpl("user", "name", setOf(), auth.name, "userid", "jwtId"), "pass")

  @Nested
  inner class ListRequest {
    @Test
    fun `calls service list`() {
      val services = mutableListOf(Service())
      whenever(authServicesService.list()).thenReturn(services)
      val userIndex = controller.userIndex()
      assertThat(userIndex.viewName).isEqualTo("ui/services")
      assertThat(userIndex.model).containsExactlyEntriesOf(mapOf("serviceDetails" to services))
      verify(authServicesService).list()
    }
  }

  @Nested
  inner class EditFormRequest {
    @Test
    fun `show edit form request view add service`() {
      val view = controller.showEditForm(null)

      assertThat(view.viewName).isEqualTo("ui/service")
      assertThat(view.model).containsExactlyEntriesOf(mapOf("service" to Service()))

      verifyZeroInteractions(authServicesService)
    }

    @Test
    fun `show edit form request view edit service`() {
      val service = Service()
      service.code = "somecode"
      whenever(authServicesService.getService(anyString())).thenReturn(service)
      val view = controller.showEditForm("code")

      assertThat(view.viewName).isEqualTo("ui/service")
      assertThat(view.model).containsExactlyEntriesOf(mapOf("service" to service))

      verify(authServicesService).getService("code")
    }
  }

  @Nested
  inner class EditService {
    @Test
    fun `edit service - add service`() {
      val service = Service()
      service.code = "newcode"
      val url = controller.editService(authentication, service, true)
      assertThat(url).isEqualTo("redirect:/ui/services")
      verify(authServicesService).addService(service)
      verify(telemetryClient).trackEvent(
        "AuthServiceDetailsAdd",
        mapOf("username" to "user", "code" to "newcode"),
        null
      )
    }

    @Test
    fun `edit service - edit service`() {
      val service = Service()
      service.code = "editcode"
      val url = controller.editService(authentication, service)
      assertThat(url).isEqualTo("redirect:/ui/services")
      verify(authServicesService).updateService(service)
      verify(telemetryClient).trackEvent(
        "AuthServiceDetailsUpdate",
        mapOf("username" to "user", "code" to "editcode"),
        null
      )
    }
  }

  @Nested
  inner class DeleteService {
    @Test
    fun `delete service`() {
      val url = controller.deleteService(authentication, "code")
      assertThat(url).isEqualTo("redirect:/ui/services")
      verify(authServicesService).removeService("code")
      verify(telemetryClient).trackEvent(
        "AuthServiceDetailsDeleted",
        mapOf("username" to "user", "code" to "code"),
        null
      )
    }
  }
}
