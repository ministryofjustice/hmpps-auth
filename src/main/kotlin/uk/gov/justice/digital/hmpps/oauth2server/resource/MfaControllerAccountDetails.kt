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
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider.MfaUnavailableException
import uk.gov.justice.digital.hmpps.oauth2server.service.LoginFlowException
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaFlowException
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
  private val telemetryClient: TelemetryClient,
  private val mfaService: MfaService,
  @Value("\${application.smoketest.enabled}") private val smokeTestEnabled: Boolean,
) : AbstractMfaController(
  tokenService,
  mfaService,
  smokeTestEnabled,
  "AccountDetails",
  "/account-details",
  "/account/mfa-challenge",
) {
  @GetMapping("/account/mfa-challenge")
  fun mfaChallengeRequestAccountDetail(
    authentication: Authentication,
    @RequestParam contactType: String,
    @RequestParam token: String?,
    @RequestParam passToken: String?,
    @RequestParam mfaPreference: MfaPreferenceType?,
  ): ModelAndView {
    passTokenInvalidForEmail(contactType, passToken)?.let { return ModelAndView("redirect:/account-details", "error", it) }

    return try {
      // issue token to current mfa preference
      val mfaData = mfaService.createTokenAndSendMfaCode(authentication.name)
      val codeDestination = mfaService.getCodeDestination(mfaData.token, mfaData.mfaType)
      val modelAndView = ModelAndView("mfaChallengeAccountDetails", "token", mfaData.token)
        .addObject("mfaPreference", mfaData.mfaType)
        .addObject("codeDestination", codeDestination)
        .addAllObjects(extraModel(contactType, passToken))
      if (smokeTestEnabled) modelAndView.addObject("smokeCode", mfaData.code)
      modelAndView
    } catch (e: MfaUnavailableException) {
      ModelAndView("redirect:/account-details", "error", "mfaunavailable")
    }
  }

  private fun passTokenInvalidForEmail(contactType: String, passToken: String?): String? {
    if (contactType != "email") return null
    if (passToken.isNullOrEmpty()) return "tokeninvalid"
    val optionalErrorForToken = tokenService.checkToken(TokenType.CHANGE, passToken)
    return optionalErrorForToken.map { "token$it" }.orElse(null)
  }

  @GetMapping("/account/mfa-challenge-error")
  fun mfaChallengeRequestAccountDetailError(
    authentication: Authentication,
    @RequestParam contactType: String,
    @RequestParam error: String?,
    @RequestParam token: String?,
    @RequestParam passToken: String?,
    @RequestParam mfaPreference: MfaPreferenceType?,
  ): ModelAndView {
    passTokenInvalidForEmail(contactType, passToken)?.let { return ModelAndView("redirect:/account-details", "error", it) }

    val codeDestination = mfaService.getCodeDestination(token!!, mfaPreference!!)
    return ModelAndView("mfaChallengeAccountDetails", "token", token)
      .addObject("mfaPreference", mfaPreference)
      .addObject("codeDestination", codeDestination)
      .addAllObjects(extraModel(contactType, passToken))
      .addObject("error", error)
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
  ): ModelAndView? {
    val optionalErrorForToken = tokenService.checkToken(TokenType.MFA, token)
    if (optionalErrorForToken.isPresent) {
      return ModelAndView("redirect:/account-details", "error", "mfa${optionalErrorForToken.get()}")
    }
    // can just grab token here as validated above
    val username = tokenService.getToken(TokenType.MFA, token).map { it.user.username }.orElseThrow()

    try {
      mfaService.validateAndRemoveMfaCode(token, code)
    } catch (e: MfaFlowException) {
      return ModelAndView("redirect:/account/mfa-challenge-error")
        .addObject("token", token)
        .addAllObjects(extraModel(contactType, passToken))
        .addObject("error", e.error)
        .addObject("mfaPreference", mfaPreference)
    } catch (e: LoginFlowException) {
      return ModelAndView("redirect:/logout", "error", e.error)
    }

    // success, so forward on
    telemetryClient.trackEvent("MFAAuthenticateSuccess", mapOf("username" to username), null)

    return continueToChangeAccountDetails(username, contactType)
  }

  private fun continueToChangeAccountDetails(username: String, contactType: String): ModelAndView {
    // successfully passed 2fa, so generate change password token
    val token = tokenService.createToken(TokenType.ACCOUNT, username)

    @Suppress("SpringMVCViewInspection")
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
