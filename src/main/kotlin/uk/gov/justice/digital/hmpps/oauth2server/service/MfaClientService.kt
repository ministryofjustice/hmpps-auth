@file:Suppress("DEPRECATION", "SpringJavaInjectionPointsAutowiringInspection")

package uk.gov.justice.digital.hmpps.oauth2server.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.provider.ClientDetailsService
import uk.gov.justice.digital.hmpps.oauth2server.resource.MfaAccess.all
import uk.gov.justice.digital.hmpps.oauth2server.resource.MfaAccess.untrusted

open class MfaClientService(
  private val clientDetailsService: ClientDetailsService,
  private val mfaClientNetworkService: MfaClientNetworkService,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  open fun clientNeedsMfa(clientId: String?): Boolean {
    val client = clientDetailsService.loadClientByClientId(clientId)
    val mfa = client.additionalInformation["mfa"] as? String?
    return (mfa == untrusted.name && mfaClientNetworkService.outsideApprovedNetwork()) || mfa == all.name
  }
}
