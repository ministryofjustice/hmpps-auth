package uk.gov.justice.digital.hmpps.oauth2server.maintain

import com.microsoft.applicationinsights.TelemetryClient
import lombok.extern.slf4j.Slf4j
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.*
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.OauthServiceRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException
import uk.gov.justice.digital.hmpps.oauth2server.security.ReusedPasswordException
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.utils.EmailHelper
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException
import java.time.LocalDateTime
import java.util.*
import javax.persistence.EntityNotFoundException

@Service
@Slf4j
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
open class AuthUserService(private val userRepository: UserRepository,
                           private val notificationClient: NotificationClientApi,
                           private val telemetryClient: TelemetryClient,
                           private val verifyEmailService: VerifyEmailService,
                           private val authUserGroupService: AuthUserGroupService,
                           private val maintainUserCheck: MaintainUserCheck,
                           private val passwordEncoder: PasswordEncoder,
                           private val oauthServiceRepository: OauthServiceRepository,
                           @Value("\${application.notify.create-initial-password.template}") private val initialPasswordTemplateId: String,
                           @Value("\${application.authentication.disable.login-days}") private val loginDaysTrigger: Int,
                           @Value("\${application.authentication.password-age}") private val passwordAge: Long) {

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(CreateUserException::class, NotificationClientException::class, VerifyEmailException::class)
  open fun createUser(usernameInput: String?, emailInput: String?, firstName: String, lastName: String,
                      groupCode: String?, url: String, creator: String, authorities: Collection<GrantedAuthority>): String {
    // ensure username always uppercase
    val username = StringUtils.upperCase(usernameInput)
    // and use email helper to format input email
    val email = EmailHelper.format(emailInput)
    // validate
    validate(username, email, firstName, lastName)
    // get the initial group to assign to - only allowed to be empty if super user
    val group = getInitialGroup(groupCode, creator, authorities)
    // create the user
    val person = Person(firstName, lastName)
    // obtain list of authorities that should be assigned for group
    val roles = group?.assignableRoles?.filter { it.isAutomatic }?.map { it.role }?.toSet() ?: emptySet()
    val groups: Set<Group> = group?.let { setOf(it) } ?: emptySet()
    val user = User.builder()
        .username(username)
        .email(email)
        .enabled(true)
        .source(AuthSource.auth)
        .person(person)
        .authorities(roles)
        .groups(groups).build()
    return saveAndSendInitialEmail(url, user, creator, "AuthUserCreate", groups)
  }

  private fun getInitialEmailSupportLink(groups: Collection<Group>): String {
    val serviceCode = groups.firstOrNull { it.groupCode.startsWith("PECS") }?.let { "BOOK_MOVE" } ?: "NOMIS"
    return oauthServiceRepository.findById(serviceCode).map { it.email }.orElseThrow()
  }

  open fun findAuthUsers(name: String?, roleCode: String?, groupCode: String?, pageable: Pageable): Page<User> {
    val userFilter = UserFilter.builder().name(name).roleCode(roleCode).groupCode(groupCode).build()
    return userRepository.findAll(userFilter, pageable)
  }

  @Throws(CreateUserException::class)
  private fun getInitialGroup(groupCode: String?, creator: String, authorities: Collection<GrantedAuthority>): Group? {
    if (groupCode.isNullOrEmpty()) {
      return if (authorities.any { it.authority == "ROLE_MAINTAIN_OAUTH_USERS" }) {
        null
      } else throw CreateUserException("groupCode", "missing")
    }
    val authUserGroups = authUserGroupService.getAssignableGroups(creator, authorities)
    return authUserGroups.firstOrNull { it.groupCode == groupCode }
        ?: throw CreateUserException("groupCode", "notfound")
  }

  @Throws(NotificationClientException::class)
  private fun saveAndSendInitialEmail(url: String, user: User, creator: String, eventPrefix: String, groups: Collection<Group>): String { // then the reset token
    val userToken = user.createToken(UserToken.TokenType.RESET)
    // give users more time to do the reset
    userToken.tokenExpiry = LocalDateTime.now().plusDays(7)
    userRepository.save(user)
    // support link
    val supportLink = getInitialEmailSupportLink(groups)
    val setPasswordLink = url + userToken.token
    val username = user.username
    val email = user.email
    val parameters = mapOf("firstName" to user.firstName, "resetLink" to setPasswordLink, "supportLink" to supportLink)
    // send the email
    try {
      log.info("Sending initial set password to notify for user {}", username)
      notificationClient.sendEmail(initialPasswordTemplateId, email, parameters, null)
      telemetryClient.trackEvent("${eventPrefix}Success", mapOf("username" to username, "admin" to creator), null)
    } catch (e: NotificationClientException) {
      val reason = (e.cause?.let { e.cause } ?: e).javaClass.simpleName
      log.warn("Failed to send create user notify for user {}", username, e)
      telemetryClient.trackEvent("${eventPrefix}Failure", mapOf("username" to username, "reason" to reason, "admin" to creator), null)
      if (e.httpResult >= 500) { // second time lucky
        notificationClient.sendEmail(initialPasswordTemplateId, email, parameters, null, null)
        telemetryClient.trackEvent("${eventPrefix}Success", mapOf("username" to username, "admin" to creator), null)
      }
      throw e
    }
    // return the reset link to the controller
    return setPasswordLink
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(VerifyEmailException::class, NotificationClientException::class, AuthUserGroupRelationshipException::class)
  open fun amendUserEmail(usernameInput: String?, emailAddressInput: String?, url: String, admin: String, authorities: Collection<GrantedAuthority?>?): String {
    val username = StringUtils.upperCase(usernameInput)
    val user = userRepository.findByUsernameAndMasterIsTrue(username)
        .orElseThrow { EntityNotFoundException("User not found with username $username") }
    maintainUserCheck.ensureUserLoggedInUserRelationship(admin, authorities, user)
    if (user.isVerified) {
      user.isVerified = false
      userRepository.save(user)
      return verifyEmailService.requestVerification(
          usernameInput,
          emailAddressInput,
          user.firstName,
          url.replace("initial-password", "verify-email-confirm"))
    }
    val email = EmailHelper.format(emailAddressInput)
    verifyEmailService.validateEmailAddress(email)
    user.email = email
    return saveAndSendInitialEmail(url, user, admin, "AuthUserAmend", user.groups)
  }

  open fun findAuthUsersByEmail(email: String?): List<User> =
      userRepository.findByEmailAndMasterIsTrueOrderByUsername(EmailHelper.format(email))

  open fun getAuthUserByUsername(username: String?): Optional<User> =
      userRepository.findByUsernameAndMasterIsTrue(StringUtils.upperCase(StringUtils.trim(username)))

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(AuthUserGroupRelationshipException::class)
  open fun enableUser(usernameInDb: String, admin: String, authorities: Collection<GrantedAuthority>) =
      changeUserEnabled(usernameInDb, true, admin, authorities)

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(AuthUserGroupRelationshipException::class)
  open fun disableUser(usernameInDb: String, admin: String, authorities: Collection<GrantedAuthority>) =
      changeUserEnabled(usernameInDb, false, admin, authorities)

  @Throws(AuthUserGroupRelationshipException::class)
  private fun changeUserEnabled(username: String, enabled: Boolean, admin: String, authorities: Collection<GrantedAuthority>) {
    val user = userRepository.findByUsernameAndMasterIsTrue(username)
        .orElseThrow { EntityNotFoundException("User not found with username $username") }
    maintainUserCheck.ensureUserLoggedInUserRelationship(admin, authorities, user)
    user.isEnabled = enabled
    // give user 7 days grace if last logged in more than x days ago
    if (user.lastLoggedIn.isBefore(LocalDateTime.now().minusDays(loginDaysTrigger.toLong()))) {
      user.lastLoggedIn = LocalDateTime.now().minusDays(loginDaysTrigger - 7.toLong())
    }
    userRepository.save(user)
    telemetryClient.trackEvent("AuthUserChangeEnabled",
        mapOf("username" to user.username, "enabled" to enabled.toString(), "admin" to admin), null)
  }

  @Throws(CreateUserException::class, VerifyEmailException::class)
  private fun validate(username: String?, email: String?, firstName: String?, lastName: String?) {
    if (username.isNullOrBlank() || StringUtils.length(username) < MIN_LENGTH_USERNAME) {
      throw CreateUserException("username", "length")
    }
    if (StringUtils.length(username) > MAX_LENGTH_USERNAME) {
      throw CreateUserException("username", "maxlength")
    }
    if (!username.matches("^[A-Z0-9_]*\$".toRegex())) {
      throw CreateUserException("username", "format")
    }
    validate(firstName, lastName)
    verifyEmailService.validateEmailAddress(email)
  }

  @Throws(CreateUserException::class)
  private fun validate(firstName: String?, lastName: String?) {
    if (StringUtils.length(firstName) < MIN_LENGTH_FIRST_NAME) {
      throw CreateUserException("firstName", "length")
    }
    if (StringUtils.length(firstName) > MAX_LENGTH_FIRST_NAME) {
      throw CreateUserException("firstName", "maxlength")
    }
    if (StringUtils.length(lastName) < MIN_LENGTH_LAST_NAME) {
      throw CreateUserException("lastName", "length")
    }
    if (StringUtils.length(lastName) > MAX_LENGTH_LAST_NAME) {
      throw CreateUserException("lastName", "maxlength")
    }
  }

  @Transactional(transactionManager = "authTransactionManager")
  open fun lockUser(userPersonDetails: UserPersonDetails) {
    val username = userPersonDetails.username
    val userOptional = userRepository.findByUsername(username)
    val user = userOptional.orElseGet { userPersonDetails.toUser() }
    user.isLocked = true
    userRepository.save(user)
  }

  @Transactional(transactionManager = "authTransactionManager")
  open fun unlockUser(userPersonDetails: UserPersonDetails) {
    val username = userPersonDetails.username
    val userOptional = userRepository.findByUsername(username)
    val user = userOptional.orElseGet { userPersonDetails.toUser() }
    user.isLocked = false
    // TODO: This isn't quite right - shouldn't always verify a user when unlocking...
    user.isVerified = true
    userRepository.save(user)
  }

  @Transactional(transactionManager = "authTransactionManager")
  open fun changePassword(user: User, password: String) {
    // check user not setting password to existing password
    if (passwordEncoder.matches(password, user.password)) {
      throw ReusedPasswordException()
    }
    user.password = passwordEncoder.encode(password)
    user.passwordExpiry = LocalDateTime.now().plusDays(passwordAge)
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(CreateUserException::class)
  open fun amendUser(username: String, firstName: String, lastName: String) {
    validate(firstName, lastName)
    // will always be a user at this stage since we're retrieved it from the authentication
    val user = userRepository.findByUsernameAndSource(username, AuthSource.auth).orElseThrow()
    user.person.firstName = firstName
    user.person.lastName = lastName
    userRepository.save(user)
  }

  class CreateUserException(val field: String, val errorCode: String) :
      Exception("Create user failed for field $field with reason: $errorCode")

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    // Data item field size validation checks
    private const val MAX_LENGTH_USERNAME = 30
    private const val MAX_LENGTH_FIRST_NAME = 50
    private const val MAX_LENGTH_LAST_NAME = 50
    private const val MIN_LENGTH_USERNAME = 6
    private const val MIN_LENGTH_FIRST_NAME = 2
    private const val MIN_LENGTH_LAST_NAME = 2
  }

}
