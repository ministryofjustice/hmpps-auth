@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.MfaPreferenceType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider.MfaUnavailableException
import uk.gov.justice.digital.hmpps.oauth2server.security.UserRetriesService
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import uk.gov.service.notify.NotificationClientApi

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Service
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
class MfaService(
  @Value("\${application.notify.mfa.template}") private val mfaEmailTemplateId: String,
  @Value("\${application.notify.mfa-text.template}") private val mfaTextTemplateId: String,
  private val tokenService: TokenService,
  private val userService: UserService,
  private val notificationClient: NotificationClientApi,
  private val userRetriesService: UserRetriesService,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(MfaUnavailableException::class)
  fun createTokenAndSendMfaCode(username: String): MfaData {
    log.info("Creating token and sending email for {}", username)
    val user = userService.getOrCreateUser(username).orElseThrow()

    val token = tokenService.createToken(TokenType.MFA, username)
    val code = tokenService.createToken(TokenType.MFA_CODE, username)

    val mfaType = user.calculateMfaFromPreference().map {
      @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
      when (it) {
        MfaPreferenceType.EMAIL -> emailCode(user, code, user.email!!)
        MfaPreferenceType.SECONDARY_EMAIL -> emailCode(user, code, user.secondaryEmail!!)
        MfaPreferenceType.TEXT -> textCode(user, code)
      }
      it
    }.orElseThrow { MfaUnavailableException("Unable to find a valid mfa preference type") }

    return MfaData(token, code, mfaType)
  }

  @Transactional(
    transactionManager = "authTransactionManager",
    noRollbackFor = [LoginFlowException::class, MfaFlowException::class]
  )
  fun validateAndRemoveMfaCode(token: String, code: String?) {
    if (code.isNullOrBlank()) throw MfaFlowException("missingcode")

    val userToken = tokenService.getToken(TokenType.MFA, token).orElseThrow()
    val userPersonDetails = userService.findMasterUserPersonDetails(userToken.user.username).orElseThrow()

    if (!userPersonDetails.isAccountNonLocked) throw LoginFlowException("locked")

    val errors = tokenService.checkToken(TokenType.MFA_CODE, code)
    errors.ifPresent {
      if (it == "invalid") {
        val locked = userRetriesService.incrementRetriesAndLockAccountIfNecessary(userPersonDetails)
        if (locked) throw LoginFlowException("locked")
      }
      throw MfaFlowException(it)
    }

    tokenService.removeToken(TokenType.MFA, token)
    tokenService.removeToken(TokenType.MFA_CODE, code)

    userRetriesService.resetRetries(userPersonDetails.username)
  }

  @Transactional(transactionManager = "authTransactionManager")
  fun updateUserMfaPreference(pref: MfaPreferenceType, username: String) {
    val user = userService.findUser(username).orElseThrow { UsernameNotFoundException(username) }
    user.mfaPreference = pref
  }

  fun resendMfaCode(token: String, mfaPreference: MfaPreferenceType): String? {
    val userToken = tokenService.getToken(TokenType.MFA, token).orElseThrow()

    val code = userToken.user.tokens.filter { it.tokenType == TokenType.MFA_CODE }.map { it.token }.firstOrNull()

    code?.run {
      if (mfaPreference == MfaPreferenceType.EMAIL) {
        emailCode(userToken.user, code, userToken.user.email!!)
      } else if (mfaPreference == MfaPreferenceType.SECONDARY_EMAIL) {
        emailCode(userToken.user, code, userToken.user.secondaryEmail!!)
      } else {
        textCode(userToken.user, code)
      }
    }

    return code
  }

  fun buildModelAndViewWithMfaResendOptions(
    view: String,
    token: String,
    mfaPreference: MfaPreferenceType,
    contactType: String
  ): ModelAndView {
    val user = tokenService.getUserFromToken(TokenType.MFA, token)
    val modelAndView = ModelAndView(view, "token", token)
      .addObject("mfaPreference", mfaPreference)
      .addObject("contactType", contactType)

    if (user.verified) {
      modelAndView.addObject("email", user.maskedEmail)
    }
    if (user.isMobileVerified) {
      modelAndView.addObject("mobile", user.maskedMobile)
    }
    if (user.isSecondaryEmailVerified) {
      modelAndView.addObject("secondaryemail", user.maskedSecondaryEmail)
    }
    return modelAndView
  }

  private fun emailCode(user: User, code: String, email: String) {
    val firstName = userService.findMasterUserPersonDetails(user.username).map { it.firstName }.orElseThrow()

    notificationClient.sendEmail(mfaEmailTemplateId, email, mapOf("firstName" to firstName, "code" to code), null)
  }

  private fun textCode(user: User, code: String) {
    notificationClient.sendSms(mfaTextTemplateId, user.mobile, mapOf("mfaCode" to code), null, null)
  }

  fun getCodeDestination(token: String, mfaPreference: MfaPreferenceType): String {
    val userToken = tokenService.getToken(TokenType.MFA, token).orElseThrow()
    return userToken.user.getCodeDestination(mfaPreference)
  }
}

class MfaFlowException(val error: String) : RuntimeException(error)
class LoginFlowException(val error: String) : RuntimeException(error)

data class MfaData(val token: String, val code: String, val mfaType: MfaPreferenceType)
