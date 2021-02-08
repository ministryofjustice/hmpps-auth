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
class AccountMfaController(
  private val tokenService: TokenService,
  private val telemetryClient: TelemetryClient,
  private val mfaService: MfaService,
  @Value("\${application.smoketest.enabled}") private val smokeTestEnabled: Boolean,
) {

  @GetMapping("/account/mfa-challenge")
  fun serviceMfaChallengeRequest(
    authentication: Authentication,
    @RequestParam contactType: String,
  ): ModelAndView {
    // issue token to current mfa preference
    val mfaData = mfaService.createTokenAndSendMfaCode(authentication.name)
    val codeDestination = mfaService.getCodeDestination(mfaData.token, mfaData.mfaType)
    val modelAndView = ModelAndView("mfaChallengeAD", "token", mfaData.token)
      .addObject("mfaPreference", mfaData.mfaType)
      .addObject("codeDestination", codeDestination)
      .addObject("contactType", contactType)
    if (smokeTestEnabled) modelAndView.addObject("smokeCode", mfaData.code)
    return modelAndView
  }

  @PostMapping("/account/mfa-challenge")
  @Throws(IOException::class, ServletException::class)
  fun serviceMfaChallenge(
    @RequestParam token: String,
    @RequestParam mfaPreference: MfaPreferenceType,
    @RequestParam code: String,
    @RequestParam contactType: String,
    request: HttpServletRequest,
    response: HttpServletResponse,
  ): ModelAndView? {
    val optionalErrorForToken = tokenService.checkToken(TokenType.MFA, token)
    if (optionalErrorForToken.isPresent) {
      return ModelAndView("redirect:/account-details?error=mfa${optionalErrorForToken.get()}")
    }
    // can just grab token here as validated above
    val username = tokenService.getToken(TokenType.MFA, token).map { it.user.username }.orElseThrow()

    // now load the user
    // val userPersonDetails = userService.findMasterUserPersonDetails(username).orElseThrow()

    try {
      mfaService.validateAndRemoveMfaCode(token, code)
    } catch (e: MfaFlowException) {
      return ModelAndView(
        "mfaChallengeAD",
        mapOf("token" to token, "error" to e.error, "mfaPreference" to mfaPreference, "contactType" to contactType)
      )
    } catch (e: LoginFlowException) {
      return ModelAndView("redirect:/login?error=${e.error}")
    }

    // success, so forward on
    telemetryClient.trackEvent("MFAAuthenticateSuccess", mapOf("username" to username), null)
    // val successToken = MfaPassedAuthenticationToken(userPersonDetails, "code", userPersonDetails.authorities)

    return continueToChangeAccountDetails(username, contactType)
  }

  private fun continueToChangeAccountDetails(username: String, contactType: String): ModelAndView {
    // successfully passed 2fa, so generate change password token
    val token = tokenService.createToken(TokenType.CHANGE, username)

    @Suppress("SpringMVCViewInspection")
    return ModelAndView("redirect:/new-$contactType", "token", token)
  }

  @GetMapping("/account/mfa-resend")
  fun mfaResendRequest(
    @RequestParam contactType: String,
    @RequestParam token: String,
    @RequestParam mfaPreference: MfaPreferenceType
  ): ModelAndView {

    val optionalError = tokenService.checkToken(TokenType.MFA, token)

    return optionalError.map { ModelAndView("redirect:/account-detail?error=mfa$it") }
      .orElseGet { mfaService.buildModelAndViewWithMfaResendOptions("mfaResendAD", token, mfaPreference, contactType) }
  }

  @PostMapping("/account/mfa-resend")
  @Throws(IOException::class, ServletException::class)
  fun mfaResend(
    @RequestParam contactType: String,
    @RequestParam token: String,
    @RequestParam mfaResendPreference: MfaPreferenceType,
    request: HttpServletRequest,
    response: HttpServletResponse,
  ): ModelAndView {
    val optionalErrorForToken = tokenService.checkToken(TokenType.MFA, token)
    if (optionalErrorForToken.isPresent) {
      return ModelAndView("redirect:/account-detail?error=mfa${optionalErrorForToken.get()}")
    }

    val code = mfaService.resendMfaCode(token, mfaResendPreference)
    // shouldn't really get a code without a valid token, but cope with the scenario anyway
    if (code.isNullOrEmpty()) {
      return ModelAndView("redirect:/account-detail?error=mfainvalid")
    }

    val modelAndView = ModelAndView("redirect:/account/mfa-challenge", "token", token)
      .addObject("contactType", contactType)
      .addObject("mfaPreference", mfaResendPreference)
    if (smokeTestEnabled) modelAndView.addObject("smokeCode", code)
    return modelAndView
  }
}
