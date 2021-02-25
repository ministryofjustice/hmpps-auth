@file:Suppress("DEPRECATION", "SpringJavaInjectionPointsAutowiringInspection")

package uk.gov.justice.digital.hmpps.oauth2server.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.web.util.matcher.IpAddressMatcher
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oauth2server.utils.IpAddressHelper

@Service
class MfaClientNetworkService(
  @Value("\${application.authentication.mfa.allowlist}") allowlist: Set<String>,
  @Value("\${application.authentication.mfa.roles}") private val mfaRoles: Set<String>,
) {

  private val ipMatchers: List<IpAddressMatcher> = allowlist.map { IpAddressMatcher(it) }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun outsideApprovedNetwork(): Boolean {
    val ip = IpAddressHelper.retrieveIpFromRequest()
    return ipMatchers.none { it.matches(ip) }
  }

  fun needsMfa(authorities: Collection<GrantedAuthority>): Boolean =
    outsideApprovedNetwork() && authorities.map { it.authority }.any { mfaRoles.contains(it) }
}
