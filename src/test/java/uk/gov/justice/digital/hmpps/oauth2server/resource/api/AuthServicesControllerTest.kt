package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service
import uk.gov.justice.digital.hmpps.oauth2server.service.AuthServicesService

class AuthServicesControllerTest {
  private val authServicesService: AuthServicesService = mock()
  private val controller = AuthServicesController(authServicesService)

  @Test
  fun `test services returns all enabled services`() {
    val auth1 = Service("code1", "name1", "description1", "roles1", "url1", true, "email1")
    val auth2 = Service("code2", "name2", "description2", "roles2", "url2", true, "email2")
    val disabled = Service("code", "name", "description", "roles", "url", false, "email")
    whenever(authServicesService.list()).thenReturn(listOf(auth1, auth2, disabled))
    val response = controller.services()
    assertThat(response).containsOnly(AuthService(auth1), AuthService(auth2))
  }
}
