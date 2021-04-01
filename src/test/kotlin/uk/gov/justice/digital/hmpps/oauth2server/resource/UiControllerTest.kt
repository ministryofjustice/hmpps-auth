package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Client
import uk.gov.justice.digital.hmpps.oauth2server.service.ClientService

internal class UiControllerTest {
  private val clientService: ClientService = mock()
  private val controller = UiController(clientService)

  @Test
  internal fun test() {
    val clients = listOf(Client("client-1"))
    whenever(clientService.listUniqueClients()).thenReturn(clients)
    val modelAndView = controller.userIndex()
    assertThat(modelAndView.viewName).isEqualTo("ui/index")
    assertThat(modelAndView.model["clientDetails"]).isEqualTo(clients)
  }
}
