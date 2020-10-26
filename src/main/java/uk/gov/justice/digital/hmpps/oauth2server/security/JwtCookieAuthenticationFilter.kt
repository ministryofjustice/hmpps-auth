package uk.gov.justice.digital.hmpps.oauth2server.security

import io.jsonwebtoken.JwtException
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
class JwtCookieAuthenticationFilter(
  private val jwtCookieHelper: JwtCookieHelper,
  private val jwtAuthenticationHelper: JwtAuthenticationHelper
) : OncePerRequestFilter() {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Throws(ServletException::class, IOException::class)
  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    val jwt = jwtCookieHelper.readValueFromCookie(request)
    try {
      jwt.flatMap { jwtAuthenticationHelper.readAuthenticationFromJwt(it) }
        .ifPresent { SecurityContextHolder.getContext().authentication = it }
    } catch (e: JwtException) {
      log.info("Unable to read authentication from JWT", e)
    } catch (e: Exception) {
      // filter errors don't get logged by spring boot, so log here
      log.error("Failed to read authentication due to {}", e.javaClass.name, e)
      throw e
    }
    filterChain.doFilter(request, response)
  }
}
