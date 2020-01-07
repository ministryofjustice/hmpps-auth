package uk.gov.justice.digital.hmpps.oauth2server.security

import org.apache.commons.lang3.StringUtils
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider.MfaRequiredException
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import java.io.IOException
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class UserStateAuthenticationFailureHandler(private val tokenService: TokenService,
                                            private val mfaService: MfaService) : SimpleUrlAuthenticationFailureHandler(FAILURE_URL) {
  companion object {
    private const val FAILURE_URL = "/login"
  }

  init {
    isAllowSessionCreation = false
  }

  @Throws(IOException::class)
  override fun onAuthenticationFailure(request: HttpServletRequest, response: HttpServletResponse,
                                       exception: AuthenticationException) {
    val builder = StringJoiner("&error=", "?error=", "")

    when (exception) {
      is LockedException -> builder.add("locked")
      is CredentialsExpiredException -> {
        // special handling for expired users and feature switch turned on
        val username = StringUtils.trim(request.getParameter("username").toUpperCase())
        val token = tokenService.createToken(TokenType.CHANGE, username)
        redirectStrategy.sendRedirect(request, response, "/change-password?token=$token")
        return
      }
      is MfaRequiredException -> {
        // need to break out to perform mfa for the user
        val username = StringUtils.trim(request.getParameter("username").toUpperCase())
        val token = mfaService.createTokenAndSendEmail(username)
        redirectStrategy.sendRedirect(request, response, "/mfa-challenge?token=$token")
        return
      }
      is MissingCredentialsException -> {
        if (StringUtils.isBlank(request.getParameter("username"))) {
          builder.add("missinguser")
        }
        if (StringUtils.isBlank(request.getParameter("password"))) {
          builder.add("missingpass")
        }
      }
      is DeliusAuthenticationServiceException -> builder.add("invalid").add("deliusdown")
      else -> builder.add("invalid")
    }

    val redirectUrl = FAILURE_URL + builder.toString()
    redirectStrategy.sendRedirect(request, response, redirectUrl)
  }
}
