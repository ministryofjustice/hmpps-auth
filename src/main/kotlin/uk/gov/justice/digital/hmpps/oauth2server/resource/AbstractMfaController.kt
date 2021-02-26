@file:Suppress("SpringMVCViewInspection")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.MfaPreferenceType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService

abstract class AbstractMfaController(
  private val tokenService: TokenService,
  private val viewNameSuffix: String,
  private val startPageUrl: String,
) {
  internal fun createMfaResendRequest(
    token: String,
    mfaPreference: MfaPreferenceType,
    extraModel: Map<String, Any?> = emptyMap()
  ): ModelAndView {
    val optionalError = tokenService.checkToken(UserToken.TokenType.MFA, token)

    return optionalError.map { ModelAndView("redirect:$startPageUrl", "error", "mfa$it") }
      .orElseGet {
        ModelAndView(
          "mfaResend$viewNameSuffix",
          buildModelWithMfaResendOptions(token, mfaPreference, extraModel)
        )
      }
  }

  private fun buildModelWithMfaResendOptions(
    token: String,
    mfaPreference: MfaPreferenceType,
    extraModel: Map<String, Any?>,
  ): Map<String, Any?> {
    val user = tokenService.getUserFromToken(UserToken.TokenType.MFA, token)
    val model = mutableMapOf<String, Any?>("token" to token, "mfaPreference" to mfaPreference)
    model.putAll(extraModel)

    if (user.verified) model["email"] = user.maskedEmail
    if (user.isMobileVerified) model["mobile"] = user.maskedMobile
    if (user.isSecondaryEmailVerified) model["secondaryemail"] = user.maskedSecondaryEmail
    return model
  }
}
