package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.web.util.UriComponentsBuilder
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class RedirectingLoginUrlAuthenticationEntryPoint(loginFormUrl: String) :
  LoginUrlAuthenticationEntryPoint(loginFormUrl) {

  override fun buildRedirectUrlToLoginPage(
    request: HttpServletRequest,
    response: HttpServletResponse,
    authException: AuthenticationException?
  ): String {
    val url = super.buildRedirectUrlToLoginPage(request, response, authException)
    val builder = UriComponentsBuilder.fromUriString(url)
    val parameter = request.getParameter("redirect_uri")
    if (!parameter.isNullOrBlank()) builder.queryParam("redirect_uri", parameter)
    return builder.toUriString()
  }
}
