package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RedirectingLogoutSuccessHandlerTest {
    @Mock
    private ClientDetailsService clientDetailsService;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private RedirectingLogoutSuccessHandler redirectingLogoutSuccessHandler;

    @Before
    public void setUp() {
        redirectingLogoutSuccessHandler = new RedirectingLogoutSuccessHandler(clientDetailsService, "/path", false);
    }

    @Test
    public void onLogoutSuccess_NoClientId() throws IOException {
        redirectingLogoutSuccessHandler.onLogoutSuccess(request, response, null);
        verify(response).sendRedirect("/path/login?logout");
    }

    @Test
    public void onLogoutSuccess_ClientIdNotMatched() throws IOException {
        when(request.getParameter("client_id")).thenReturn("joe");
        redirectingLogoutSuccessHandler.onLogoutSuccess(request, response, null);
        verify(response).sendRedirect("/path/login?logout");
    }

    @Test
    public void onLogoutSuccess_RedirectUriNotMatched() throws IOException {
        when(request.getParameter("client_id")).thenReturn("joe");
        when(request.getParameter("redirect_uri")).thenReturn("http://some.where");
        when(clientDetailsService.loadClientByClientId("joe")).thenReturn(createClientDetails("http://tim.buk.tu"));
        redirectingLogoutSuccessHandler.onLogoutSuccess(request, response, null);
        verify(response).sendRedirect("/path/login?logout");
    }

    @Test
    public void onLogoutSuccess_NoRedirectUrisConfigured() throws IOException {
        when(request.getParameter("client_id")).thenReturn("joe");
        redirectingLogoutSuccessHandler.onLogoutSuccess(request, response, null);
        verify(response).sendRedirect("/path/login?logout");
    }

    @Test
    public void onLogoutSuccess_RedirectUriMatched() throws IOException {
        when(request.getParameter("client_id")).thenReturn("joe");
        when(request.getParameter("redirect_uri")).thenReturn("http://some.where");
        when(clientDetailsService.loadClientByClientId("joe")).thenReturn(createClientDetails("http://tim.buk.tu", "http://some.where"));
        redirectingLogoutSuccessHandler.onLogoutSuccess(request, response, null);
        verify(response).sendRedirect("http://some.where");
    }

    @Test
    public void onLogoutSuccess_RedirectUriMatched_SubdomainPolicyNotSet() throws IOException {
        final var subdomainHandler = new RedirectingLogoutSuccessHandler(clientDetailsService, "/path", false);

        when(request.getParameter("client_id")).thenReturn("joe");
        when(request.getParameter("redirect_uri")).thenReturn("http://some.where");
        when(clientDetailsService.loadClientByClientId("joe")).thenReturn(createClientDetails("http://tim.buk.tu", "http://where"));
        subdomainHandler.onLogoutSuccess(request, response, null);
        verify(response).sendRedirect("/path/login?logout");
    }

    @Test
    public void onLogoutSuccess_RedirectUriMatched_Subdomain() throws IOException {
        final var subdomainHandler = new RedirectingLogoutSuccessHandler(clientDetailsService, "/path", true);

        when(request.getParameter("client_id")).thenReturn("joe");
        when(request.getParameter("redirect_uri")).thenReturn("http://some.where");
        when(clientDetailsService.loadClientByClientId("joe")).thenReturn(createClientDetails("http://tim.buk.tu", "http://where"));
        subdomainHandler.onLogoutSuccess(request, response, null);
        verify(response).sendRedirect("http://some.where");
    }

    @Test
    public void onLogoutSuccess_RedirectUriMatchedWithSlash() throws IOException {
        when(request.getParameter("client_id")).thenReturn("joe");
        when(request.getParameter("redirect_uri")).thenReturn("http://some.where/");
        when(clientDetailsService.loadClientByClientId("joe")).thenReturn(createClientDetails("http://tim.buk.tu", "http://some.where"));
        redirectingLogoutSuccessHandler.onLogoutSuccess(request, response, null);
        verify(response).sendRedirect("http://some.where");
    }

    @Test
    public void onLogoutSuccess_RedirectUriMatchedWithoutSlash() throws IOException {
        when(request.getParameter("client_id")).thenReturn("joe");
        when(request.getParameter("redirect_uri")).thenReturn("http://some.where");
        when(clientDetailsService.loadClientByClientId("joe")).thenReturn(createClientDetails("http://tim.buk.tu", "http://some.where/"));
        redirectingLogoutSuccessHandler.onLogoutSuccess(request, response, null);
        verify(response).sendRedirect("http://some.where/");
    }

    private ClientDetails createClientDetails(final String... urls) {
        final var details = new BaseClientDetails();
        details.setRegisteredRedirectUri(Set.of(urls));
        details.setAuthorizedGrantTypes(List.of("authorization_code"));
        return details;
    }
}
