package uk.gov.justice.digital.hmpps.oauth2server.resource;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.RedirectMismatchException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.endpoint.DefaultRedirectResolver;
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SuppressWarnings("deprecation")
@Component
@Slf4j
public class RedirectingLogoutSuccessHandler implements LogoutSuccessHandler {
    private final ClientDetailsService clientDetailsService;
    private final String servletContextPath;
    private final RedirectResolver redirectResolver;


    public RedirectingLogoutSuccessHandler(final ClientDetailsService clientDetailsService,
                                           @Value("#{servletContext.contextPath}") final String servletContextPath,
                                           @Value("${application.authentication.match-subdomains}") final boolean matchSubdomains) {
        this.clientDetailsService = clientDetailsService;
        this.servletContextPath = servletContextPath;
        final var defaultRedirectResolver = new DefaultRedirectResolver();
        defaultRedirectResolver.setMatchSubdomains(matchSubdomains);
        redirectResolver = defaultRedirectResolver;
    }

    @Override
    public void onLogoutSuccess(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) throws IOException {
        final var client = request.getParameter("client_id");
        final var redirect = request.getParameter("redirect_uri");
        final var error = request.getParameter("error");

        // If we have asked for a redirect, check it is valid for the client
        if (client != null && redirect != null) {
            final var clientDetails = clientDetailsService.loadClientByClientId(client);
            if (clientDetails != null && !CollectionUtils.isEmpty(clientDetails.getRegisteredRedirectUri())) {
                if (responseRedirectedOnValidRedirect(response, redirect, clientDetails)) return;
                // second attempt - ignore or add trailing slash
                final var redirectSlash = redirect.endsWith("/") ? redirect.substring(0, redirect.length() - 1) : redirect + "/";
                if (responseRedirectedOnValidRedirect(response, redirectSlash, clientDetails)) return;
            }
        }
        response.sendRedirect(servletContextPath + "/login?logout" + (StringUtils.isNotBlank(error) ? "&error=" + error : ""));
    }

    private boolean responseRedirectedOnValidRedirect(final HttpServletResponse response, final String redirect, final ClientDetails clientDetails) throws IOException {
        try {
            response.sendRedirect(redirectResolver.resolveRedirect(redirect, clientDetails));
            return true;
        } catch (final RedirectMismatchException rme) {
            log.info("Ignoring redirect {} as not valid for client {}, {}", redirect, clientDetails.getClientId(), rme.getMessage());
        }
        return false;
    }
}
