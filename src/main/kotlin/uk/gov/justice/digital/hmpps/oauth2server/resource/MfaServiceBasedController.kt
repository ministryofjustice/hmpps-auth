@file:Suppress("SpringJavaInjectionPointsAutowiringInspection", "SpringMVCViewInspection")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.SessionAttributes
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.MfaPreferenceType
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
@SessionAttributes(
  "authorizationRequest",
  "org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint.ORIGINAL_AUTHORIZATION_REQUEST"
)
@Validated
class MfaServiceBasedController(
  private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler,
  private val tokenService: TokenService,
  telemetryClient: TelemetryClient,
  mfaService: MfaService,
  @Value("\${application.smoketest.enabled}") private val smokeTestEnabled: Boolean,
) : AbstractMfaController(
  tokenService,
  telemetryClient,
  mfaService,
  smokeTestEnabled,
  "ServiceBased",
  "/",
  "/service-mfa-challenge",
  "redirect:/service-mfa-challenge-error",
) {
  @GetMapping("/service-mfa-challenge")
  fun mfaChallengeRequestServiceBased(
    authentication: Authentication,
    @RequestParam user_oauth_approval: String?,
  ): ModelAndView = mfaChallengeRequest(authentication, extraModel(user_oauth_approval))

  @GetMapping("/service-mfa-challenge-error")
  fun mfaChallengeRequestServiceBasedError(
    @RequestParam error: String?,
    @RequestParam token: String?,
    @RequestParam mfaPreference: MfaPreferenceType?,
    @RequestParam user_oauth_approval: String?,
  ): ModelAndView = mfaChallengeRequestError(error, token, mfaPreference, extraModel(user_oauth_approval))

  @PostMapping("/service-mfa-challenge")
  @Throws(IOException::class, ServletException::class)
  fun mfaChallengeServiceBased(
    @RequestParam token: String,
    @RequestParam mfaPreference: MfaPreferenceType,
    @RequestParam code: String,
    @RequestParam user_oauth_approval: String?,
    request: HttpServletRequest,
    response: HttpServletResponse,
    authentication: Authentication,
  ): ModelAndView? = mfaChallenge(token, mfaPreference, code, extraModel(user_oauth_approval)) {
    jwtAuthenticationSuccessHandler.updateMfaInRequest(request, response, authentication)

    ModelAndView("forward:/oauth/authorize")
  }

  @GetMapping("/service-mfa-resend")
  fun mfaResendRequest(
    @RequestParam token: String,
    @RequestParam user_oauth_approval: String?,
    @RequestParam mfaPreference: MfaPreferenceType
  ): ModelAndView = createMfaResendRequest(token, mfaPreference, extraModel(user_oauth_approval))

  @PostMapping("/service-mfa-resend")
  fun mfaResend(
    @RequestParam token: String,
    @RequestParam user_oauth_approval: String?,
    @RequestParam mfaResendPreference: MfaPreferenceType,
  ): ModelAndView = createMfaResend(token, mfaResendPreference, extraModel(user_oauth_approval))

  private fun extraModel(user_oauth_approval: String?) = mapOf("user_oauth_approval" to user_oauth_approval)
}
