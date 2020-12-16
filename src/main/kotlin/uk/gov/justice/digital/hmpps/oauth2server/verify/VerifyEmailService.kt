package uk.gov.justice.digital.hmpps.oauth2server.verify

import com.microsoft.applicationinsights.TelemetryClient
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Contact
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ContactType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.EmailType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository
import uk.gov.justice.digital.hmpps.oauth2server.utils.EmailHelper
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException
import java.sql.ResultSet
import java.util.HashMap
import java.util.HashSet
import java.util.Optional
import javax.persistence.EntityNotFoundException

@Service
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
class VerifyEmailService(
  private val userRepository: UserRepository,
  private val userTokenRepository: UserTokenRepository,
  private val jdbcTemplate: NamedParameterJdbcTemplate,
  private val telemetryClient: TelemetryClient,
  private val notificationClient: NotificationClientApi,
  private val referenceCodesService: ReferenceCodesService,
  @Value("\${application.notify.verify.template}") private val notifyTemplateId: String,
) {

  fun getEmail(username: String): Optional<User> =
    userRepository.findByUsername(username).filter { ue: User -> StringUtils.isNotBlank(ue.email) }

  fun isNotVerified(name: String): Boolean =
    !getEmail(name).map { obj: User -> obj.verified }.orElse(false)

  fun getExistingEmailAddressesForUsername(username: String): List<String> =
    jdbcTemplate.queryForList(EXISTING_EMAIL_SQL, mapOf("username" to username), String::class.java)

  fun getExistingEmailAddressesForUsernames(usernames: List<String>): Map<String, MutableSet<String>> {
    val emailsByUsername: MutableMap<String, MutableSet<String>> = HashMap()
    if (usernames.isEmpty()) {
      return emailsByUsername
    }
    jdbcTemplate.query(
      EXISTING_EMAIL_FOR_USERNAMES_SQL,
      mapOf("usernames" to usernames)
    ) { rs: ResultSet ->
      val username = rs.getString("USERNAME")
      val email = rs.getString("EMAIL")
      emailsByUsername
        .computeIfAbsent(username) { HashSet() }
        .add(email)
    }
    return emailsByUsername
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(NotificationClientException::class, VerifyEmailException::class)
  fun changeEmailAndRequestVerification(
    username: String,
    emailInput: String?,
    firstName: String?,
    fullname: String?,
    url: String,
    emailType: EmailType,
  ): String {
    val user = userRepository.findByUsername(username).orElseThrow()
    val verifyLink =
      url + user.createToken(if (emailType == EmailType.PRIMARY) UserToken.TokenType.VERIFIED else UserToken.TokenType.SECONDARY).token
    val parameters = mapOf("firstName" to firstName, "fullName" to fullname, "verifyLink" to verifyLink)
    val email = EmailHelper.format(emailInput)
    validateEmailAddress(email, emailType)
    when (emailType) {
      EmailType.PRIMARY -> {
        // if the user is configured so that the email address is their username, need to check it is unique
        if (user.email == username.toLowerCase()) {
          userRepository.findByUsername(email!!.toUpperCase()).ifPresent {
            throw VerifyEmailException("duplicate")
          }
          user.username = email
        }
        user.email = email
        user.verified = false
      }
      EmailType.SECONDARY -> user.addContact(ContactType.SECONDARY_EMAIL, email)
    }
    try {
      log.info("Sending email verification to notify for user {}", username)
      notificationClient.sendEmail(notifyTemplateId, email, parameters, null)
      telemetryClient.trackEvent("VerifyEmailRequestSuccess", mapOf("username" to username), null)
    } catch (e: NotificationClientException) {
      val reason = (if (e.cause != null) e.cause else e)?.javaClass?.simpleName
      log.warn("Failed to send email verification to notify for user {}", username, e)
      telemetryClient.trackEvent(
        "VerifyEmailRequestFailure",
        mapOf("username" to username, "reason" to reason),
        null
      )
      if (e.httpResult >= 500) {
        // second time lucky
        notificationClient.sendEmail(notifyTemplateId, email, parameters, null, null)
      }
      throw e
    }
    userRepository.save(user)
    return verifyLink
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(NotificationClientException::class, VerifyEmailException::class)
  fun resendVerificationCodeEmail(username: String, url: String): Optional<String> {
    val user = userRepository.findByUsername(username).orElseThrow()
    if (user.email == null) {
      throw VerifyEmailException("noemail")
    }
    if (user.verified) {
      log.info("Verify email succeeded due to already verified")
      telemetryClient.trackEvent(
        "VerifyEmailConfirmFailure",
        mapOf("reason" to "alreadyverified", "username" to username),
        null
      )
      return Optional.empty()
    }
    val verifyLink = url + user.createToken(UserToken.TokenType.VERIFIED).token
    val parameters = mapOf("firstName" to user.firstName, "fullName" to user.name, "verifyLink" to verifyLink)
    notificationClient.sendEmail(notifyTemplateId, user.email, parameters, null)
    return Optional.of(verifyLink)
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(NotificationClientException::class, VerifyEmailException::class)
  fun resendVerificationCodeSecondaryEmail(username: String, url: String): Optional<String> {
    val user = userRepository.findByUsername(username).orElseThrow()
    if (user.secondaryEmail == null) {
      throw VerifyEmailException("nosecondaryemail")
    }
    if (user.isSecondaryEmailVerified) {
      log.info("Verify secondary email succeeded due to already verified")
      telemetryClient.trackEvent(
        "VerifySecondaryEmailConfirmFailure",
        mapOf("reason" to "alreadyverified", "username" to username),
        null
      )
      return Optional.empty()
    }
    val verifyLink = url + user.createToken(UserToken.TokenType.SECONDARY).token
    val parameters = mapOf("firstName" to user.firstName, "fullName" to user.name, "verifyLink" to verifyLink)
    notificationClient.sendEmail(notifyTemplateId, user.secondaryEmail, parameters, null)
    return Optional.of(verifyLink)
  }

  fun secondaryEmailVerified(username: String): Boolean = userRepository.findByUsername(username)
    .orElseThrow { EntityNotFoundException(String.format("User not found with username %s", username)) }
    .isSecondaryEmailVerified

  @Throws(VerifyEmailException::class)
  fun validateEmailAddress(email: String?, emailType: EmailType) {
    if (email == null) {
      throw VerifyEmailException("blank")
    }
    validateEmailAddressExcludingGsi(email, emailType)
    if (email.matches(Regex(".*@.*\\.gsi\\.gov\\.uk"))) throw VerifyEmailException("gsi")
  }

  fun maskedSecondaryEmailFromUsername(username: String): String {
    val user = userRepository.findByUsername(username)
    return user.orElseThrow().maskedSecondaryEmail
  }

  @Throws(VerifyEmailException::class)
  fun validateEmailAddressExcludingGsi(email: String, emailType: EmailType) {
    val atIndex = StringUtils.indexOf(email, '@'.toInt())
    if (atIndex == -1 || !email.matches(Regex(".*@.*\\..*"))) {
      throw VerifyEmailException("format")
    }
    val firstCharacter = email[0]
    val lastCharacter = email[email.length - 1]
    if (firstCharacter == '.' || firstCharacter == '@' || lastCharacter == '.' || lastCharacter == '@') {
      throw VerifyEmailException("firstlast")
    }
    if (email.matches(Regex(".*\\.@.*")) || email.matches(Regex(".*@\\..*"))) {
      throw VerifyEmailException("together")
    }
    if (StringUtils.countMatches(email, '@') > 1) {
      throw VerifyEmailException("at")
    }
    if (StringUtils.containsWhitespace(email)) {
      throw VerifyEmailException("white")
    }
    if (!email.matches(Regex("[0-9A-Za-z@.'_\\-+]*"))) {
      throw VerifyEmailException("characters")
    }
    if (emailType == EmailType.PRIMARY && !referenceCodesService.isValidEmailDomain(email.substring(atIndex + 1))) {
      throw VerifyEmailException("domain")
    }
  }

  @Transactional(transactionManager = "authTransactionManager")
  fun confirmEmail(token: String): Optional<String> {
    val userTokenOptional = userTokenRepository.findById(token)
    if (userTokenOptional.isEmpty) {
      return trackAndReturnFailureForInvalidToken()
    }
    val userToken = userTokenOptional.get()
    val user = userToken.user
    val username = user.username
    if (user.verified) {
      log.info("Verify email succeeded due to already verified")
      telemetryClient.trackEvent(
        "VerifyEmailConfirmFailure",
        mapOf("reason" to "alreadyverified", "username" to username),
        null
      )
      return Optional.empty()
    }
    if (userToken.hasTokenExpired()) {
      return trackAndReturnFailureForExpiredToken(username)
    }
    markEmailAsVerified(user)
    return Optional.empty()
  }

  @Transactional(transactionManager = "authTransactionManager")
  fun confirmSecondaryEmail(token: String): Optional<String> {
    val userTokenOptional = userTokenRepository.findById(token)
    if (userTokenOptional.isEmpty) {
      return trackAndReturnFailureForInvalidToken()
    }
    val userToken = userTokenOptional.get()
    val user = userToken.user
    val username = user.username
    if (user.isSecondaryEmailVerified) {
      log.info("Verify secondary email succeeded due to already verified")
      telemetryClient.trackEvent(
        "VerifySecondaryEmailConfirmFailure",
        mapOf("reason" to "alreadyverified", "username" to username),
        null
      )
      return Optional.empty()
    }
    if (userToken.hasTokenExpired()) {
      return trackAndReturnFailureForExpiredToken(username)
    }
    markSecondaryEmailAsVerified(user)
    return Optional.empty()
  }

  private fun markEmailAsVerified(user: User) {
    // verification token match
    user.verified = true
    userRepository.save(user)
    log.info("Verify email succeeded for {}", user.username)
    telemetryClient.trackEvent("VerifyEmailConfirmSuccess", mapOf("username" to user.username), null)
  }

  private fun markSecondaryEmailAsVerified(user: User) {
    // verification token match
    user.findContact(ContactType.SECONDARY_EMAIL).ifPresent { c: Contact -> c.verified = true }
    userRepository.save(user)
    log.info("Verify secondary email succeeded for {}", user.username)
    telemetryClient.trackEvent("VerifySecondaryEmailConfirmSuccess", mapOf("username" to user.username), null)
  }

  private fun trackAndReturnFailureForInvalidToken(): Optional<String> {
    log.info("Verify email failed due to invalid token")
    telemetryClient.trackEvent("VerifyEmailConfirmFailure", mapOf("reason" to "invalid"), null)
    return Optional.of("invalid")
  }

  private fun trackAndReturnFailureForExpiredToken(username: String): Optional<String> {
    log.info("Verify email failed due to expired token")
    telemetryClient.trackEvent(
      "VerifyEmailConfirmFailure",
      mapOf("reason" to "expired", "username" to username),
      null
    )
    return Optional.of("expired")
  }

  class VerifyEmailException(val reason: String?) :
    Exception(String.format("Verify email failed with reason: %s", reason))

  @Suppress("SqlResolve")
  companion object {
    private const val EXISTING_EMAIL_SQL =
      """
      select distinct internet_address  
        from internet_addresses i       
             inner join STAFF_USER_ACCOUNTS s on i.owner_id = s.staff_id and owner_class = 'STF' 
       where internet_address_class = 'EMAIL' 
             and s.username = :username
      """

    private const val EXISTING_EMAIL_FOR_USERNAMES_SQL =
      """
        select s.username username,
               internet_address email     
          from internet_addresses i
               inner join STAFF_USER_ACCOUNTS s on i.owner_id = s.staff_id and owner_class = 'STF'
         where internet_address_class = 'EMAIL' and 
               s.username in (:usernames) 
      group by s.username, internet_address
      """

    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
