package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.common.exceptions.RedirectMismatchException
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.ClientDetailsService
import org.springframework.security.oauth2.provider.endpoint.DefaultRedirectResolver
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.util.CollectionUtils
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Suppress("DEPRECATION")
@Component

class RedirectingLogoutSuccessHandler(
  private val clientDetailsService: ClientDetailsService,
  @param:Value("#{servletContext.contextPath}") private val servletContextPath: String,
  @Value("\${application.authentication.match-subdomains}") matchSubdomains: Boolean
) : LogoutSuccessHandler {

  private val redirectResolver: RedirectResolver

  init {
    val defaultRedirectResolver = DefaultRedirectResolver()
    defaultRedirectResolver.setMatchSubdomains(matchSubdomains)
    redirectResolver = defaultRedirectResolver
  }

  companion object {
    private val log = LoggerFactory.getLogger(RedirectingLogoutSuccessHandler::class.java)
  }

  override fun onLogoutSuccess(
    request: HttpServletRequest,
    response: HttpServletResponse,
    authentication: Authentication?
  ) {
    val client = request.getParameter("client_id")
    val redirect = request.getParameter("redirect_uri")
    val error = request.getParameter("error")

    // If we have asked for a redirect, check it is valid for the client
    if (client != null && redirect != null) {
      val clientDetails = clientDetailsService.loadClientByClientId(client)
      if (clientDetails != null && !CollectionUtils.isEmpty(clientDetails.registeredRedirectUri)) {
        if (responseRedirectedOnValidRedirect(response, redirect, clientDetails)) {
          return
        }
        // second attempt - ignore or add trailing slash
        val redirectSlash = if (redirect.endsWith("/")) redirect.substring(0, redirect.length - 1) else "$redirect/"
        if (responseRedirectedOnValidRedirect(response, redirectSlash, clientDetails)) {
          return
        }
      }
    }
    response.sendRedirect(servletContextPath + "/login?logout" + if (StringUtils.isNotBlank(error)) "&error=$error" else "")
  }

  private fun responseRedirectedOnValidRedirect(
    response: HttpServletResponse,
    redirect: String,
    clientDetails: ClientDetails
  ): Boolean {
    try {
      response.sendRedirect(redirectResolver.resolveRedirect(redirect, clientDetails))
      return true
    } catch (rme: RedirectMismatchException) {
      log.info("Ignoring redirect {} as not valid for client {}, {}", redirect, clientDetails.clientId, rme.message)
    }
    return false
  }
}
