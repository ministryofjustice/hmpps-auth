package uk.gov.justice.digital.hmpps.oauth2server.resource.account

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.common.exceptions.RedirectMismatchException
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.ClientDetailsService
import org.springframework.security.oauth2.provider.endpoint.DefaultRedirectResolver
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver
import org.springframework.stereotype.Component
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

  fun validateRedirect(client: String, redirect: String): Boolean {

    val url = with(URI(redirect)) {
      when (port) {
        -1, 80, 443 -> "$scheme://$host"
        else -> "$scheme://$host:$port"
      }
    }
    // If we have asked for a redirect, check it is valid for the client
    val clientDetails = clientDetailsService.loadClientByClientId(client)

    return clientDetails?.registeredRedirectUri?.isNotEmpty() == true &&
      (
        responseRedirectedOnValidRedirect(url, clientDetails) || responseRedirectedOnValidRedirect(
          "$url/",
          clientDetails
        )
        )
  }

  private fun responseRedirectedOnValidRedirect(redirect: String, clientDetails: ClientDetails) = try {
    redirectResolver.resolveRedirect(redirect, clientDetails)
    true
  } catch (rme: RedirectMismatchException) {
    log.info("Ignoring redirect {} as not valid for client {}, {}", redirect, clientDetails.clientId, rme.message)
    false
  }
}
