package uk.gov.justice.digital.hmpps.oauth2server.utils

import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.lang.NonNull
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Order(1)
class UserMdcFilter : OncePerRequestFilter() {
  @Throws(ServletException::class, IOException::class)
  override fun doFilterInternal(
    @NonNull request: HttpServletRequest,
    @NonNull response: HttpServletResponse,
    @NonNull filterChain: FilterChain,
  ) {
    val currentUsername = getUser(request)
    try {
      currentUsername?.let { MDC.put(MdcUtility.USER_ID_HEADER, currentUsername) }
      filterChain.doFilter(request, response)
    } finally {
      currentUsername?.let { MDC.remove(MdcUtility.USER_ID_HEADER) }
    }
  }

  private fun getUser(req: HttpServletRequest): String? = req.userPrincipal?.name
}
