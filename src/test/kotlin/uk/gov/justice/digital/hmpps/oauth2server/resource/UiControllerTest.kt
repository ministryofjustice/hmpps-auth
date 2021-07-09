package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ClientType
import uk.gov.justice.digital.hmpps.oauth2server.service.ClientService
import uk.gov.justice.digital.hmpps.oauth2server.service.ClientSummary
import uk.gov.justice.digital.hmpps.oauth2server.service.SortBy.count

internal class UiControllerTest {
  private val clientService: ClientService = mock()
  private val controller = UiController(clientService)

  @Test
  internal fun test() {
    val clients = listOf(
      ClientSummary(
        baseClientId = "client-1",
        grantTypes = "bob",
        roles = "role",
        count = 5,
        clientType = ClientType.PERSONAL,
        teamName = "name",
        lastAccessed = null,
        secretUpdated = null
      )
    )
    whenever(clientService.listUniqueClients(any())).thenReturn(clients)
    val modelAndView = controller.userIndex(count)
    assertThat(modelAndView.viewName).isEqualTo("ui/index")
    assertThat(modelAndView.model["clientDetails"]).isEqualTo(clients)

    verify(clientService).listUniqueClients(count)
  }
}
