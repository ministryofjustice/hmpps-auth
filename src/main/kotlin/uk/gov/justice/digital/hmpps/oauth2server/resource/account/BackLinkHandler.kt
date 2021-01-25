package uk.gov.justice.digital.hmpps.oauth2server.resource.account

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.common.exceptions.RedirectMismatchException
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.ClientDetailsService
import org.springframework.security.oauth2.provider.endpoint.DefaultRedirectResolver
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver
import org.springframework.stereotype.Component
import org.springframework.util.CollectionUtils
import java.net.URI

@Suppress("DEPRECATION")
@Component

class BackLinkHandler(
  private val clientDetailsService: ClientDetailsService,
  @Value("\${application.authentication.match-subdomains}") matchSubdomains: Boolean
) {

  private val redirectResolver: RedirectResolver

  init {
    val defaultRedirectResolver = DefaultRedirectResolver()
    defaultRedirectResolver.setMatchSubdomains(matchSubdomains)
    redirectResolver = defaultRedirectResolver
  }

  companion object {
    private val log = LoggerFactory.getLogger(BackLinkHandler::class.java)
  }

  fun validateRedirect(
    client: String,
    redirect: String,
  ): Boolean {

    val uri = URI(redirect)
    val url = when {
      uri.port == 443 -> "${uri.scheme}://${uri.host}"
      uri.port != -1 -> "${uri.scheme}://${uri.host}:${uri.port}"
      else -> "${uri.scheme}://${uri.host}"
    }

    // If we have asked for a redirect, check it is valid for the client
    val clientDetails = clientDetailsService.loadClientByClientId(client)

    if (clientDetails != null && !CollectionUtils.isEmpty(clientDetails.registeredRedirectUri)) {
      if (responseRedirectedOnValidRedirect(url, clientDetails)) {
        return true
      }
      // second attempt - add trailing slash
      if (responseRedirectedOnValidRedirect("$url/", clientDetails)) {
        return true
      }
    }
    return false
  }

  private fun responseRedirectedOnValidRedirect(
    redirect: String,
    clientDetails: ClientDetails
  ): Boolean {
    try {

      redirectResolver.resolveRedirect(redirect, clientDetails)
      return true
    } catch (rme: RedirectMismatchException) {
      log.info("Ignoring redirect {} as not valid for client {}, {}", redirect, clientDetails.clientId, rme.message)
    }
    return false
  }
}
