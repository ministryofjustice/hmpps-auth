@file:Suppress("SpringMVCViewInspection")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.security.core.Authentication
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.MfaPreferenceType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.MFA
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider.MfaUnavailableException
import uk.gov.justice.digital.hmpps.oauth2server.service.LoginFlowException
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaFlowException
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService

abstract class AbstractMfaController(
  private val tokenService: TokenService,
  private val telemetryClient: TelemetryClient,
  private val mfaService: MfaService,
  private val smokeTestEnabled: Boolean,
  private val viewNameSuffix: String,
  private val startPageUrl: String,
  private val pageUrl: String,
  private val errorView: String,
) {
  protected fun mfaChallengeRequest(
    authentication: Authentication,
    extraModel: Map<String, Any?> = emptyMap(),
  ): ModelAndView = try {
    // issue token to current mfa preference
    val mfaData = mfaService.createTokenAndSendMfaCode(authentication.name)
    val codeDestination = mfaService.getCodeDestination(mfaData.token, mfaData.mfaType)
    val modelAndView = ModelAndView("mfaChallenge$viewNameSuffix", "token", mfaData.token)
      .addObject("mfaPreference", mfaData.mfaType)
      .addObject("codeDestination", codeDestination)
      .addAllObjects(extraModel)
    if (smokeTestEnabled) modelAndView.addObject("smokeCode", mfaData.code)
    modelAndView
  } catch (e: MfaUnavailableException) {
    ModelAndView("redirect:$startPageUrl", "error", "mfaunavailable")
  }

  protected fun mfaChallengeRequestError(
    error: String?,
    token: String?,
    mfaPreference: MfaPreferenceType?,
    extraModel: Map<String, Any?> = emptyMap(),
  ): ModelAndView {
    val codeDestination = mfaService.getCodeDestination(token!!, mfaPreference!!)
    return ModelAndView("mfaChallenge$viewNameSuffix", "token", token)
      .addObject("mfaPreference", mfaPreference)
      .addObject("codeDestination", codeDestination)
      .addAllObjects(extraModel)
      .addObject("error", error)
  }

  protected fun mfaChallenge(
    token: String,
    mfaPreference: MfaPreferenceType,
    code: String,
    extraModel: Map<String, Any?> = emptyMap(),
    successFunction: (username: String) -> ModelAndView?,
  ): ModelAndView? {
    val optionalErrorForToken = tokenService.checkToken(MFA, token)
    if (optionalErrorForToken.isPresent) {
      return ModelAndView("redirect:$startPageUrl", "error", "mfa${optionalErrorForToken.get()}")
    }
    // can just grab token here as validated above
    val username = tokenService.getToken(MFA, token).map { it.user.username }.orElseThrow()

    try {
      mfaService.validateAndRemoveMfaCode(token, code)
    } catch (e: MfaFlowException) {
      return ModelAndView(errorView)
        .addObject("token", token)
        .addAllObjects(extraModel)
        .addObject("error", e.error)
        .addObject("mfaPreference", mfaPreference)
    } catch (e: LoginFlowException) {
      return ModelAndView("redirect:/logout", "error", e.error)
    }

    // success, so forward on
    telemetryClient.trackEvent("MFAAuthenticateSuccess", mapOf("username" to username), null)

    return successFunction(username)
  }

  protected fun createMfaResendRequest(
    token: String,
    mfaPreference: MfaPreferenceType,
    extraModel: Map<String, Any?> = emptyMap(),
  ): ModelAndView {
    val optionalError = tokenService.checkToken(MFA, token)

    return optionalError.map { ModelAndView("redirect:$startPageUrl", "error", "mfa$it") }
      .orElseGet {
        ModelAndView(
          "mfaResend$viewNameSuffix",
          buildModelWithMfaResendOptions(token, mfaPreference, extraModel)
        )
      }
  }

  protected fun createMfaResend(
    token: String,
    mfaResendPreference: MfaPreferenceType,
    extraModel: Map<String, Any?> = emptyMap(),
  ): ModelAndView {
    val optionalErrorForToken = tokenService.checkToken(MFA, token)
    if (optionalErrorForToken.isPresent) {
      return ModelAndView("redirect:$startPageUrl", "error", "mfa${optionalErrorForToken.get()}")
    }

    val code = mfaService.resendMfaCode(token, mfaResendPreference)
    // shouldn't really get a code without a valid token, but cope with the scenario anyway
    if (code.isNullOrEmpty()) {
      return ModelAndView("redirect:$startPageUrl", "error", "mfainvalid")
    }

    val modelAndView = ModelAndView("redirect:$pageUrl", "token", token)
      .addObject("mfaPreference", mfaResendPreference)
      .addAllObjects(extraModel)
    if (smokeTestEnabled) modelAndView.addObject("smokeCode", code)
    return modelAndView
  }

  private fun buildModelWithMfaResendOptions(
    token: String,
    mfaPreference: MfaPreferenceType,
    extraModel: Map<String, Any?>,
  ): Map<String, Any?> {
    val user = tokenService.getUserFromToken(MFA, token)
    val model = mutableMapOf<String, Any?>("token" to token, "mfaPreference" to mfaPreference)
    model.putAll(extraModel)

    if (user.verified) model["email"] = user.maskedEmail
    if (user.isMobileVerified) model["mobile"] = user.maskedMobile
    if (user.isSecondaryEmailVerified) model["secondaryemail"] = user.maskedSecondaryEmail
    return model
  }
}
