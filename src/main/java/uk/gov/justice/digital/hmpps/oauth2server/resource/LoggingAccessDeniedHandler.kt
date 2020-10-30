package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.io.IOException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class LoggingAccessDeniedHandler : AccessDeniedHandler {

  companion object {
    private val log = LoggerFactory.getLogger(LoggingAccessDeniedHandler::class.java)
  }

  @Throws(IOException::class)
  override fun handle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    ex: AccessDeniedException
  ) {
    val auth = SecurityContextHolder.getContext().authentication
    if (auth != null) {
      log.info(
        auth.name +
          " was trying to access protected resource: " +
          request.requestURI
      )
    }
    response.sendRedirect(request.contextPath + "/access-denied")
  }
}
