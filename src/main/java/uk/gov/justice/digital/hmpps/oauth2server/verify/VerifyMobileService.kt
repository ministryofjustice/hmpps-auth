package uk.gov.justice.digital.hmpps.oauth2server.verify

import com.microsoft.applicationinsights.TelemetryClient
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ContactType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.MOBILE
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException
import java.util.Optional

@Service
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
class VerifyMobileService(
  private val userRepository: UserRepository,
  private val userTokenRepository: UserTokenRepository,
  private val telemetryClient: TelemetryClient,
  private val notificationClient: NotificationClientApi,
  @Value("\${application.notify.verify-mobile.template}") private val notifyTemplateId: String,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getMobile(username: String?): Optional<User> =
    userRepository.findByUsername(username).filter { StringUtils.isNotBlank(it.mobile) }

  fun isNotVerified(name: String?): Boolean = !getMobile(name).map { it.isVerified }.orElse(false)

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(VerifyMobileException::class, NotificationClientException::class)
  fun changeMobileAndRequestVerification(username: String, mobile: String?): String {
    val user = userRepository.findByUsername(username).orElseThrow()
    val canonicalMobile = mobile?.replace("\\s+".toRegex(), "")
    validateMobileNumber(canonicalMobile)
    user.addContact(ContactType.MOBILE_PHONE, canonicalMobile)
    val verifyCode = user.createToken(MOBILE).token
    val parameters = mapOf("verifyCode" to verifyCode)
    sendNotification(username, canonicalMobile!!, parameters)
    userRepository.save(user)
    return verifyCode
  }

  @Throws(NotificationClientException::class)
  private fun sendNotification(username: String, mobile: String, parameters: Map<String, String?>) {
    try {
      log.info("Sending sms verification to notify for user {}", username)
      notificationClient.sendSms(notifyTemplateId, mobile, parameters, null)
      telemetryClient.trackEvent("VerifyMobileRequestSuccess", mapOf("username" to username), null)
    } catch (e: NotificationClientException) {
      val reason = (if (e.cause != null) e.cause else e)!!.javaClass.simpleName
      log.warn("Failed to send sms verification to notify for user {}", username, e)
      telemetryClient.trackEvent(
        "VerifyMobileRequestFailure",
        mapOf("username" to username, "reason" to reason),
        null
      )
      if (e.httpResult >= 500) {
        // second time lucky
        notificationClient.sendSms(notifyTemplateId, mobile, parameters, null, null)
      }
      throw e
    }
  }

  @Throws(VerifyMobileException::class)
  fun validateMobileNumber(mobile: String?) {
    if (mobile.isNullOrBlank()) {
      throw VerifyMobileException("blank")
    }
    if (!mobile.matches("((\\+44(\\s\\s|\\s0\\s|\\s)?)|0)7\\d{3}(\\s)?\\d{6}".toRegex())) {
      throw VerifyMobileException("format")
    }
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(NotificationClientException::class)
  fun confirmMobile(token: String): Optional<Map<String, String>> {
    val userTokenOptional = userTokenRepository.findById(token)
    if (userTokenOptional.isEmpty) {
      return trackAndReturnFailureForInvalidToken()
    }
    val userToken = userTokenOptional.get()
    val user = userToken.user
    val username = user.username
    if (user.isMobileVerified) {
      log.info("Verify mobile succeeded due to already verified")
      telemetryClient.trackEvent(
        "VerifyMobileConfirmFailure",
        mapOf("reason" to "alreadyverified", "username" to username),
        null
      )
      return Optional.empty()
    }
    if (userToken.hasTokenExpired()) {
      return issueNewTokenToReplaceExpiredToken(username)
    }
    markMobileAsVerified(user)
    return Optional.empty()
  }

  private fun markMobileAsVerified(user: User) {
    // verification token match
    user.findContact(ContactType.MOBILE_PHONE).ifPresent { it.verified = true }
    userRepository.save(user)
    log.info("Verify mobile succeeded for {}", user.username)
    telemetryClient.trackEvent("VerifyMobileConfirmSuccess", mapOf("username" to user.username), null)
  }

  private fun trackAndReturnFailureForInvalidToken(): Optional<Map<String, String>> {
    log.info("Verify mobile failed due to invalid token")
    telemetryClient.trackEvent("VerifyMobileConfirmFailure", mapOf("error" to "invalid"), null)
    return Optional.of(mapOf("error" to "invalid"))
  }

  @Throws(NotificationClientException::class)
  private fun issueNewTokenToReplaceExpiredToken(username: String): Optional<Map<String, String>> {
    log.info("Verify mobile failed due to expired token")
    telemetryClient.trackEvent(
      "VerifyMobileConfirmFailure",
      mapOf("reason" to "expired", "username" to username),
      null
    )
    val user = userRepository.findByUsername(username).orElseThrow()
    val verifyCode = user.createToken(MOBILE).token
    val parameters = mapOf("verifyCode" to verifyCode)
    sendNotification(username, user.mobile, parameters)
    return Optional.of(mapOf("error" to "expired", "verifyCode" to verifyCode))
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(NotificationClientException::class, VerifyMobileException::class)
  fun resendVerificationCode(username: String): Optional<String> {
    val user = userRepository.findByUsername(username).orElseThrow()
    if (user.mobile == null) {
      throw VerifyMobileException("nomobile")
    }
    if (user.isMobileVerified) {
      log.info("Verify mobile succeeded due to already verified")
      telemetryClient.trackEvent(
        "VerifyMobileConfirmFailure",
        mapOf("reason" to "alreadyverified", "username" to username),
        null
      )
      return Optional.empty()
    }
    val verifyCode = user.createToken(MOBILE).token
    val parameters = mapOf("verifyCode" to verifyCode)
    sendNotification(username, user.mobile, parameters)
    return Optional.of(verifyCode)
  }

  fun mobileVerified(username: String?): Boolean =
    userRepository.findByUsername(username).orElseThrow().isMobileVerified

  class VerifyMobileException(val reason: String) : Exception("Verify mobile failed with reason: $reason")
}
