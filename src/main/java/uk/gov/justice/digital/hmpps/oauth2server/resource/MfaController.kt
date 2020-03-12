package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.MfaPreferenceType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
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
open class MfaController(private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler,
                         private val tokenService: TokenService,
                         private val userService: UserService,
                         private val telemetryClient: TelemetryClient,
                         private val mfaService: MfaService,
                         @Value("\${application.smoketest.enabled}") private val smokeTestEnabled: Boolean) {
  @GetMapping("/mfa-challenge")
  fun mfaChallengeRequest(@RequestParam(required = false) token: String?): ModelAndView {

    if (token.isNullOrBlank()) return ModelAndView("redirect:/login?error=mfainvalid")

    val optionalError = tokenService.checkToken(TokenType.MFA, token)

    return optionalError.map { ModelAndView("redirect:/login?error=mfa${it}") }
        .orElseGet {
          val preference = tokenService.getToken(TokenType.MFA, token).map { it.user.mfaPreference }.orElseThrow()
          ModelAndView("mfaChallenge", "token", token)
              .addObject("mfaPreference", preference)
        }
  }

  @PostMapping("/mfa-challenge")
  @Throws(IOException::class, ServletException::class)
  fun mfaChallenge(@RequestParam token: String,
                   @RequestParam mfaPreference: MfaPreferenceType,
                   @RequestParam code: String,
                   request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
    val optionalErrorForToken = tokenService.checkToken(TokenType.MFA, token)
    if (optionalErrorForToken.isPresent) {
      return ModelAndView("redirect:/login?error=mfa${optionalErrorForToken.get()}")
    }
    // can just grab token here as validated above
    val username = tokenService.getToken(TokenType.MFA, token).map { it.user.username }.orElseThrow()

    // now load the user
    val userPersonDetails = userService.findMasterUserPersonDetails(username).orElseThrow()

    try {
      mfaService.validateAndRemoveMfaCode(token, code)
    } catch (e: MfaFlowException) {
      return ModelAndView("mfaChallenge", mapOf("token" to token, "error" to e.error, "mfaPreference" to mfaPreference))
    } catch (e: LoginFlowException) {
      return ModelAndView("redirect:/login?error=${e.error}")
    }

    // success, so forward on
    telemetryClient.trackEvent("MFAAuthenticateSuccess", mapOf("username" to username), null)
    val successToken = UsernamePasswordAuthenticationToken(userPersonDetails, "code", userPersonDetails.authorities)
    jwtAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, successToken)

    // return here is not required, since the success handler will have redirected
    return null
  }

  @GetMapping("/mfa-resend-email")
  fun mfaResendEmailRequest(@RequestParam token: String, @RequestParam mfaPreference: MfaPreferenceType): ModelAndView {

    val optionalError = tokenService.checkToken(TokenType.MFA, token)

    return optionalError.map { ModelAndView("redirect:/login?error=mfa${it}") }
        .orElse(ModelAndView("mfaResendEmail", "token", token)
            .addObject("mfaPreference", mfaPreference))
  }

  @PostMapping("/mfa-resend-email")
  @Throws(IOException::class, ServletException::class)
  fun mfaResendEmail(@RequestParam token: String,
                     @RequestParam mfaResendPreference: MfaPreferenceType,
                     request: HttpServletRequest,
                     response: HttpServletResponse): ModelAndView {
    val optionalErrorForToken = tokenService.checkToken(TokenType.MFA, token)
    if (optionalErrorForToken.isPresent) {
      return ModelAndView("redirect:/login?error=mfa${optionalErrorForToken.get()}")
    }

    val code = mfaService.resendMfaCode(token, mfaResendPreference)
    // shouldn't really get a code without a valid token, but cope with the scenario anyway
    if (code.isNullOrEmpty()) {
      return ModelAndView("redirect:/login?error=mfainvalid")
    }

    val modelAndView = ModelAndView("redirect:/mfa-challenge", "token", token)
        .addObject("mfaPreference", mfaResendPreference)
    if (smokeTestEnabled) modelAndView.addObject("smokeCode", code)
    return modelAndView
  }

  @GetMapping("/mfa-resend-text")
  fun mfaResendTextRequest(@RequestParam token: String, @RequestParam mfaPreference: MfaPreferenceType, authentication: Authentication): ModelAndView {

    val optionalError = tokenService.checkToken(TokenType.MFA, token)
    val user = userService.findUser(authentication.name).orElseThrow { UsernameNotFoundException(authentication.name) }
    return optionalError.map { ModelAndView("redirect:/login?error=mfa${it}") }
        .orElse(ModelAndView("mfaResendText", "token", token)
            .addObject("mfaPreference", mfaPreference))
  }

  @PostMapping("/mfa-resend-text")
  @Throws(IOException::class, ServletException::class)
  fun mfaResendText(@RequestParam token: String,
                    @RequestParam mfaResendPreference: MfaPreferenceType,
                    request: HttpServletRequest,
                    response: HttpServletResponse): ModelAndView {
    val optionalErrorForToken = tokenService.checkToken(TokenType.MFA, token)
    if (optionalErrorForToken.isPresent) {
      return ModelAndView("redirect:/login?error=mfa${optionalErrorForToken.get()}")
    }

    val code = mfaService.resendMfaCode(token, mfaResendPreference)
    // shouldn't really get a code without a valid token, but cope with the scenario anyway
    if (code.isNullOrEmpty()) {
      return ModelAndView("redirect:/login?error=mfainvalid")
    }

    val modelAndView = ModelAndView("redirect:/mfa-challenge", "token", token)
        .addObject("mfaPreference", mfaResendPreference)
    if (smokeTestEnabled) modelAndView.addObject("smokeCode", code)
    return modelAndView
  }
}
