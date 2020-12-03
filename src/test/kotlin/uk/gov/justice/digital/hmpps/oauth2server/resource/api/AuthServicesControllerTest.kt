package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service
import uk.gov.justice.digital.hmpps.oauth2server.service.AuthServicesService

class AuthServicesControllerTest {
  private val authServicesService: AuthServicesService = mock()
  private val authentication: Authentication = mock()
  private val controller = AuthServicesController(authServicesService)

  @Test
  fun `test services returns all  services`() {
    val auth1 = Service("code1", "name1", "description1", "roles1", "url1", true, "email1")
    val auth2 = Service("code2", "name2", "description2", "roles2", "url2", true, "email2")
    whenever(authServicesService.listEnabled()).thenReturn(listOf(auth1, auth2))
    val response = controller.services()
    assertThat(response).containsExactly(AuthService(auth1), AuthService(auth2))
  }

  @Test
  fun `test services returns all my services`() {
    val auth1 = Service("code1", "name1", "description1", "roles1", "url1", true, "email1")
    val auth2 = Service("code2", "name2", "description2", "roles2", "url2", true, "email2")
    whenever(authServicesService.listEnabled(any())).thenReturn(listOf(auth1, auth2))
    val authorities = listOf(SimpleGrantedAuthority("role"))
    whenever(authentication.authorities).thenReturn(authorities)
    val response = controller.myServices(authentication)
    assertThat(response).containsExactly(AuthService(auth1), AuthService(auth2))
    verify(authServicesService).listEnabled(authorities)
  }
}
