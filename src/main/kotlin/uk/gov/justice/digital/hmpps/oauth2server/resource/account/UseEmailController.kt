package uk.gov.justice.digital.hmpps.oauth2server.resource.account

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
@RequestMapping("/use-email")
class UseEmailController(
  private val authUserService: AuthUserService,
  private val telemetryClient: TelemetryClient,
  private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler,
  private val userService: UserService,
) {
  @GetMapping
  fun useEmailRequest(authentication: Authentication): String = "account/useEmail"

  @PostMapping
  fun useEmail(
    authentication: Authentication,
    request: HttpServletRequest,
    response: HttpServletResponse
  ): String {

    val email = authUserService.useEmailAsUsername(authentication.name)
    if (email != null) {
      telemetryClient.trackEvent(
        "ChangeUsernameToEmail",
        mapOf("username" to authentication.name, "email" to email),
        null
      )

      // have to amend the token in the session as it will contain different user details
      val userPersonDetails = userService.findMasterUserPersonDetails(email).orElseThrow()
      val successToken = UsernamePasswordAuthenticationToken(userPersonDetails, null, userPersonDetails.authorities)
      jwtAuthenticationSuccessHandler.updateAuthenticationInRequest(request, response, successToken)
    }

    return "redirect:/account-details"
  }
}
