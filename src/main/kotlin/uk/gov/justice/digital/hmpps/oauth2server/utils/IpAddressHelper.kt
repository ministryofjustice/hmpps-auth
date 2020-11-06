package uk.gov.justice.digital.hmpps.oauth2server.utils

import org.apache.commons.lang3.StringUtils
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServletRequest

/**
 * Azure provides ip addresses with a port, which we need to strip out before using.  Don't want to nobble IP6 addresses
 * either though, so need to count to see how many colons are in the remote address first.
 */
object IpAddressHelper {
  @JvmStatic
  fun retrieveIpFromRemoteAddr(request: HttpServletRequest): String {
    val remoteAddr = request.remoteAddr
    val colonCount = remoteAddr.chars().filter { ch: Int -> ch == ':'.toInt() }.count()
    return if (colonCount == 1L) StringUtils.split(remoteAddr, ":")[0] else remoteAddr
  }

  /**
   * Used by [uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordServiceImpl] for throttling
   */
  fun retrieveIpFromRequest(): String {
    val requestAttributes = RequestContextHolder.currentRequestAttributes()
    return retrieveIpFromRemoteAddr((requestAttributes as ServletRequestAttributes).request)
  }
}
