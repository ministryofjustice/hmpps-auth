package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.util.UriComponentsBuilder
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
  open fun mfaChallengeRequest(@RequestParam token: String?): ModelAndView {

    if (token.isNullOrBlank()) return ModelAndView("redirect:/login?error=mfainvalid")

    val optionalError = tokenService.checkToken(TokenType.MFA, token)

    return optionalError.map { ModelAndView("redirect:/login?error=mfa${it}") }
        .orElse(ModelAndView("mfaChallenge", "token", token))
  }

  @PostMapping("/mfa-challenge")
  @Throws(IOException::class, ServletException::class)
  open fun mfaChallenge(@RequestParam token: String,
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
      return ModelAndView("mfaChallenge", mapOf("token" to token, "error" to e.error))
    } catch (e: LoginFlowException) {
      return ModelAndView("redirect:/login?error=${e.error}")
    }

    // success, so forward on
    telemetryClient.trackEvent("MfaAuthenticateSuccess", mapOf("username" to username), null)
    val successToken = UsernamePasswordAuthenticationToken(userPersonDetails, "code")
    jwtAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, successToken)

    // return here is not required, since the success handler will have redirected
    return null
  }

  @GetMapping("/mfa-resend")
  open fun mfaResendRequest(@RequestParam token: String): ModelAndView {

    val optionalError = tokenService.checkToken(TokenType.MFA, token)

    return optionalError.map { ModelAndView("redirect:/login?error=mfa${it}") }
        .orElse(ModelAndView("mfaResend", "token", token))
  }

  @PostMapping("/mfa-resend")
  @Throws(IOException::class, ServletException::class)
  open fun mfaResend(@RequestParam token: String, request: HttpServletRequest, response: HttpServletResponse): ModelAndView {
    val optionalErrorForToken = tokenService.checkToken(TokenType.MFA, token)
    if (optionalErrorForToken.isPresent) {
      return ModelAndView("redirect:/login?error=mfa${optionalErrorForToken.get()}")
    }

    val code = mfaService.resendMfaCode(token)
    // shouldn't really get a code without a valid token, but cope with the scenario anyway
    if (code.isNullOrEmpty()) {
      return ModelAndView("redirect:/login?error=mfainvalid")
    }

    val urlBuilder = UriComponentsBuilder.fromPath("/mfa-challenge").queryParam("token", token)
    if (smokeTestEnabled) urlBuilder.queryParam("smokeCode", code)
    val url = urlBuilder.build().toString()

    return ModelAndView("redirect:${url}")
  }
}
