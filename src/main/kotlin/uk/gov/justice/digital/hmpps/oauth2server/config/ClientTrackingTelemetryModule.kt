package uk.gov.justice.digital.hmpps.oauth2server.config

import com.microsoft.applicationinsights.TelemetryConfiguration
import com.microsoft.applicationinsights.extensibility.TelemetryModule
import com.microsoft.applicationinsights.web.extensibility.modules.WebTelemetryModule
import com.microsoft.applicationinsights.web.internal.ThreadContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.apache.commons.lang3.StringUtils
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import uk.gov.justice.digital.hmpps.oauth2server.utils.IpAddressHelper
import java.text.ParseException
import java.util.Optional
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

@Configuration
class ClientTrackingTelemetryModule : WebTelemetryModule, TelemetryModule {
  override fun onBeginRequest(req: ServletRequest, res: ServletResponse) {
    val telemetryProperties = ThreadContext.getRequestTelemetryContext().httpRequestTelemetry.properties
    val httpServletRequest = req as HttpServletRequest
    addClientIdAndUser(telemetryProperties, httpServletRequest)
    addClientIpAddress(telemetryProperties, httpServletRequest)
  }

  private fun addClientIpAddress(properties: MutableMap<String, String>, req: HttpServletRequest) {
    properties["clientIpAddress"] = IpAddressHelper.retrieveIpFromRemoteAddr(req)
  }

  private fun addClientIdAndUser(properties: MutableMap<String, String>, httpServletRequest: HttpServletRequest) {
    val token = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)
    val bearer = "Bearer "
    if (StringUtils.startsWithIgnoreCase(token, bearer)) {
      try {
        val jwtBody = getClaimsFromJWT(StringUtils.substringAfter(token, bearer))
        val user = Optional.ofNullable(jwtBody.getClaim("user_name"))
        user.map { it.toString() }
          .ifPresent { u: String -> properties["username"] = u }
        properties["clientId"] = jwtBody.getClaim("client_id").toString()
      } catch (e: ParseException) {
        // Expired token which spring security will handle
      }
    }
  }

  @Throws(ParseException::class)
  private fun getClaimsFromJWT(token: String): JWTClaimsSet = SignedJWT.parse(token).jwtClaimsSet

  override fun onEndRequest(req: ServletRequest, res: ServletResponse) {}
  override fun initialize(configuration: TelemetryConfiguration) {}
}
