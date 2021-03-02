@file:Suppress("DEPRECATION", "SpringJavaInjectionPointsAutowiringInspection")

package uk.gov.justice.digital.hmpps.oauth2server.service

import org.springframework.security.oauth2.provider.ClientDetailsService
import uk.gov.justice.digital.hmpps.oauth2server.resource.MfaAccess.all
import uk.gov.justice.digital.hmpps.oauth2server.resource.MfaAccess.untrusted

open class MfaClientService(
  private val clientDetailsService: ClientDetailsService,
  private val mfaClientNetworkService: MfaClientNetworkService,
) {

  open fun clientNeedsMfa(clientId: String?): Boolean {
    val client = clientDetailsService.loadClientByClientId(clientId)
    val mfa = client.additionalInformation["mfa"] as? String?
    return (mfa == untrusted.name && mfaClientNetworkService.outsideApprovedNetwork()) || mfa == all.name
  }
}
