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
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.MfaPreferenceType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
@Validated
class MfaControllerAccountDetails(
  private val tokenService: TokenService,
  telemetryClient: TelemetryClient,
  mfaService: MfaService,
  @Value("\${application.smoketest.enabled}") private val smokeTestEnabled: Boolean,
) : AbstractMfaController(
  tokenService,
  telemetryClient,
  mfaService,
  smokeTestEnabled,
  "AccountDetails",
  "/account-details",
  "/account/mfa-challenge",
) {
  @GetMapping("/account/mfa-send-challenge")
  fun mfaSendChallengeAccountDetail(
    authentication: Authentication,
    @RequestParam contactType: String,
    @RequestParam passToken: String?,
  ): ModelAndView {
    passTokenInvalidForEmail(contactType, passToken)?.let {
      return ModelAndView(
        "redirect:/account-details",
        "error",
        it
      )
    }
    return mfaSendChallenge(authentication, extraModel(contactType, passToken))
  }

  private fun passTokenInvalidForEmail(contactType: String, passToken: String?): String? {
    if (contactType != "email") return null
    if (passToken.isNullOrEmpty()) return "tokeninvalid"
    val optionalErrorForToken = tokenService.checkToken(TokenType.CHANGE, passToken)
    return optionalErrorForToken.map { "token$it" }.orElse(null)
  }

  @GetMapping("/account/mfa-challenge")
  fun mfaChallengeRequestAccountDetail(
    @RequestParam contactType: String,
    @RequestParam error: String?,
    @RequestParam token: String?,
    @RequestParam passToken: String?,
    @RequestParam mfaPreference: MfaPreferenceType?,
  ): ModelAndView {
    passTokenInvalidForEmail(contactType, passToken)?.let {
      return ModelAndView(
        "redirect:/account-details",
        "error",
        it
      )
    }

    return mfaChallengeRequest(error, token, mfaPreference, extraModel(contactType, passToken))
  }

  @PostMapping("/account/mfa-challenge")
  @Throws(IOException::class, ServletException::class)
  fun mfaChallengeAccountDetail(
    @RequestParam token: String,
    @RequestParam passToken: String,
    @RequestParam mfaPreference: MfaPreferenceType,
    @RequestParam code: String,
    @RequestParam contactType: String,
    request: HttpServletRequest,
    response: HttpServletResponse,
  ): ModelAndView? = mfaChallenge(token, mfaPreference, code, extraModel(contactType, passToken)) {
    continueToChangeAccountDetails(it, contactType)
  }

  private fun continueToChangeAccountDetails(username: String, contactType: String): ModelAndView {
    // successfully passed 2fa, so generate change password token
    val token = tokenService.createToken(TokenType.ACCOUNT, username)

    return ModelAndView("redirect:/new-$contactType", "token", token)
  }

  @GetMapping("/account/mfa-resend")
  fun mfaResendRequest(
    @RequestParam contactType: String,
    @RequestParam token: String,
    @RequestParam passToken: String,
    @RequestParam mfaPreference: MfaPreferenceType
  ): ModelAndView = createMfaResendRequest(token, mfaPreference, extraModel(contactType, passToken))

  @PostMapping("/account/mfa-resend")
  fun mfaResend(
    @RequestParam contactType: String,
    @RequestParam token: String,
    @RequestParam passToken: String,
    @RequestParam mfaResendPreference: MfaPreferenceType,
  ): ModelAndView = createMfaResend(token, mfaResendPreference, extraModel(contactType, passToken))

  private fun extraModel(contactType: String, passToken: String?) =
    mapOf("contactType" to contactType, "passToken" to passToken)
}
