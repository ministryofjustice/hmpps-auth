package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationHelper
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtCookieHelper
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class ClearAllSessionsLogoutHandler(
  private val jwtCookieHelper: JwtCookieHelper,
  private val jwtAuthenticationHelper: JwtAuthenticationHelper,
  @Qualifier("tokenVerificationApiRestTemplate") private val restTemplate: RestTemplate,
  @Value("\${tokenverification.enabled:false}") private val tokenVerificationEnabled: Boolean,
) : LogoutHandler {
  override fun logout(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication?) {
    val optionalAuth =
      jwtCookieHelper.readValueFromCookie(request).flatMap { jwtAuthenticationHelper.readUserDetailsFromJwt(it) }
    optionalAuth.ifPresent {
      log.info("Logging out user {} with jwt of {}", it.username, it.jwtId)
      if (tokenVerificationEnabled) restTemplate.delete("/token?authJwtId={authJwtId}", it.jwtId)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(ClearAllSessionsLogoutHandler::class.java)
  }
}
