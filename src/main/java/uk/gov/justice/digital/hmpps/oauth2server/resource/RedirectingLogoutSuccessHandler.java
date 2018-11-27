package uk.gov.justice.digital.hmpps.oauth2server.resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j
public class RedirectingLogoutSuccessHandler implements LogoutSuccessHandler {
    private final ClientDetailsService clientDetailsService;
    private final String servletContextPath;


    public RedirectingLogoutSuccessHandler(final ClientDetailsService clientDetailsService,
                                           @Value("#{servletContext.contextPath}") final String servletContextPath) {
        this.clientDetailsService = clientDetailsService;
        this.servletContextPath = servletContextPath;
    }

    @Override
    public void onLogoutSuccess(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) throws IOException {
        final String client = request.getParameter("client_id");
        // If we have asked for a redirect, check it is valid for the client
        if (client != null) {
            final var clientDetails = clientDetailsService.loadClientByClientId(client);
            if (clientDetails != null && !CollectionUtils.isEmpty(clientDetails.getRegisteredRedirectUri())) {
                final var redirectUris = clientDetails.getRegisteredRedirectUri();
                final var redirect = request.getParameter("redirect_uri");
                if (redirectUris.contains(redirect)) {
                    response.sendRedirect(redirect);
                    return;
                }
                // second attempt - ignore or add trailing slash
                final var redirectSlash = redirect.endsWith("/") ? redirect.substring(0, redirect.length() - 1) : redirect + "/";
                if (redirectUris.contains(redirectSlash)) {
                    response.sendRedirect(redirectSlash);
                    return;
                }
                log.info("Ignoring redirect {} as not valid for client {}", redirect, client);
            }
        }
        response.sendRedirect(servletContextPath + "/login?logout");
    }
}
