package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service
import uk.gov.justice.digital.hmpps.oauth2server.service.AuthServicesService

class ServicesControllerTest {
  private val authServicesService: AuthServicesService = mock()
  private val servicesController: ServicesController = ServicesController(authServicesService)

  @Test
  fun `calls service list`() {
    val services = mutableListOf(Service())
    whenever(authServicesService.list()).thenReturn(services)
    val userIndex = servicesController.userIndex()
    assertThat(userIndex.viewName).isEqualTo("ui/services")
    assertThat(userIndex.model).containsExactlyEntriesOf(mapOf("serviceDetails" to services))
    verify(authServicesService).list()
  }
}
