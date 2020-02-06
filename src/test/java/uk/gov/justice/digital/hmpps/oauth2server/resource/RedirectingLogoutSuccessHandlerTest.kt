@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.ClientDetailsService
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class RedirectingLogoutSuccessHandlerTest {
  private val clientDetailsService: ClientDetailsService = mock()
  private val request: HttpServletRequest = mock()
  private val response: HttpServletResponse = mock()
  private val redirectingLogoutSuccessHandler = RedirectingLogoutSuccessHandler(clientDetailsService, "/path", false)

  @Test
  fun onLogoutSuccess_NoClientId() {
    redirectingLogoutSuccessHandler.onLogoutSuccess(request, response, null)
    verify(response).sendRedirect("/path/login?logout")
  }

  @Test
  fun onLogoutSuccess_ClientIdNotMatched() {
    whenever(request.getParameter("client_id")).thenReturn("joe")
    redirectingLogoutSuccessHandler.onLogoutSuccess(request, response, null)
    verify(response).sendRedirect("/path/login?logout")
  }

  @Test
  fun onLogoutSuccess_RedirectUriNotMatched() {
    whenever(request.getParameter("client_id")).thenReturn("joe")
    whenever(request.getParameter("redirect_uri")).thenReturn("http://some.where")
    whenever(clientDetailsService.loadClientByClientId("joe")).thenReturn(createClientDetails("http://tim.buk.tu"))
    redirectingLogoutSuccessHandler.onLogoutSuccess(request, response, null)
    verify(response).sendRedirect("/path/login?logout")
  }

  @Test
  fun onLogoutSuccess_NoRedirectUrisConfigured() {
    whenever(request.getParameter("client_id")).thenReturn("joe")
    redirectingLogoutSuccessHandler.onLogoutSuccess(request, response, null)
    verify(response).sendRedirect("/path/login?logout")
  }

  @Test
  fun onLogoutSuccess_RedirectUriMatched() {
    whenever(request.getParameter("client_id")).thenReturn("joe")
    whenever(request.getParameter("redirect_uri")).thenReturn("http://some.where")
    whenever(clientDetailsService.loadClientByClientId("joe")).thenReturn(createClientDetails("http://tim.buk.tu", "http://some.where"))
    redirectingLogoutSuccessHandler.onLogoutSuccess(request, response, null)
    verify(response).sendRedirect("http://some.where")
  }

  @Test
  fun onLogoutSuccess_RedirectUriMatched_SubdomainPolicyNotSet() {
    val subdomainHandler = RedirectingLogoutSuccessHandler(clientDetailsService, "/path", false)
    whenever(request.getParameter("client_id")).thenReturn("joe")
    whenever(request.getParameter("redirect_uri")).thenReturn("http://some.where")
    whenever(clientDetailsService.loadClientByClientId("joe")).thenReturn(createClientDetails("http://tim.buk.tu", "http://where"))
    subdomainHandler.onLogoutSuccess(request, response, null)
    verify(response).sendRedirect("/path/login?logout")
  }

  @Test
  fun onLogoutSuccess_ErrorSet() {
    val subdomainHandler = RedirectingLogoutSuccessHandler(clientDetailsService, "/path", false)
    whenever(request.getParameter("client_id")).thenReturn("joe")
    whenever(request.getParameter("error")).thenReturn("somevalue")
    subdomainHandler.onLogoutSuccess(request, response, null)
    verify(response).sendRedirect("/path/login?logout&error=somevalue")
  }

  @Test
  fun onLogoutSuccess_RedirectUriMatched_Subdomain() {
    val subdomainHandler = RedirectingLogoutSuccessHandler(clientDetailsService, "/path", true)
    whenever(request.getParameter("client_id")).thenReturn("joe")
    whenever(request.getParameter("redirect_uri")).thenReturn("http://some.where")
    whenever(clientDetailsService.loadClientByClientId("joe")).thenReturn(createClientDetails("http://tim.buk.tu", "http://where"))
    subdomainHandler.onLogoutSuccess(request, response, null)
    verify(response).sendRedirect("http://some.where")
  }

  @Test
  fun onLogoutSuccess_RedirectUriMatchedWithSlash() {
    whenever(request.getParameter("client_id")).thenReturn("joe")
    whenever(request.getParameter("redirect_uri")).thenReturn("http://some.where/")
    whenever(clientDetailsService.loadClientByClientId("joe")).thenReturn(createClientDetails("http://tim.buk.tu", "http://some.where"))
    redirectingLogoutSuccessHandler.onLogoutSuccess(request, response, null)
    verify(response).sendRedirect("http://some.where")
  }

  @Test
  fun onLogoutSuccess_RedirectUriMatchedWithoutSlash() {
    whenever(request.getParameter("client_id")).thenReturn("joe")
    whenever(request.getParameter("redirect_uri")).thenReturn("http://some.where")
    whenever(clientDetailsService.loadClientByClientId("joe")).thenReturn(createClientDetails("http://tim.buk.tu", "http://some.where/"))
    redirectingLogoutSuccessHandler.onLogoutSuccess(request, response, null)
    verify(response).sendRedirect("http://some.where/")
  }

  private fun createClientDetails(vararg urls: String): ClientDetails {
    val details = BaseClientDetails()
    details.registeredRedirectUri = setOf(*urls)
    details.setAuthorizedGrantTypes(listOf("authorization_code"))
    return details
  }
}
