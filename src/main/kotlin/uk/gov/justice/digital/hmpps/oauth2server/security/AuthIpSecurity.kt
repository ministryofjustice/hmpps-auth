package uk.gov.justice.digital.hmpps.oauth2server.security

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.web.util.matcher.IpAddressMatcher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.oauth2server.utils.IpAddressHelper.retrieveIpFromRemoteAddr
import javax.servlet.http.HttpServletRequest

@Component
class AuthIpSecurity(@Value("\${application.authentication.ui.allowlist}") private val allowlist: Set<String>) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun check(request: HttpServletRequest?): Boolean {
    val remoteIp = retrieveIpFromRemoteAddr(request!!)
    val matchIp = allowlist.any { ip: String? -> IpAddressMatcher(ip).matches(remoteIp) }
    if (!matchIp) {
      log.warn("Client IP {}, is not in allowlist {}", remoteIp, allowlist)
    }
    return matchIp
  }
}
