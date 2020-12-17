package uk.gov.justice.digital.hmpps.oauth2server.maintain

import com.microsoft.applicationinsights.TelemetryClient
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
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.EmailType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserFilter
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
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
import java.util.Optional
import javax.persistence.EntityNotFoundException

@Service
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
class AuthUserService(
  private val userRepository: UserRepository,
  private val notificationClient: NotificationClientApi,
  private val telemetryClient: TelemetryClient,
  private val verifyEmailService: VerifyEmailService,
  private val authUserGroupService: AuthUserGroupService,
  private val maintainUserCheck: MaintainUserCheck,
  private val passwordEncoder: PasswordEncoder,
  private val oauthServiceRepository: OauthServiceRepository,
  @Value("\${application.notify.create-initial-password.template}") private val initialPasswordTemplateId: String,
  @Value("\${application.authentication.disable.login-days}") private val loginDaysTrigger: Int,
  @Value("\${application.authentication.password-age}") private val passwordAge: Long,
) {

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(CreateUserException::class, NotificationClientException::class, VerifyEmailException::class)
  fun createUser(
    usernameInput: String?,
    emailInput: String?,
    firstName: String?,
    lastName: String?,
    groupCodes: Set<String>?,
    url: String,
    creator: String,
    authorities: Collection<GrantedAuthority>,
  ): String {
    // ensure username always uppercase
    val username = StringUtils.upperCase(usernameInput)
    // and use email helper to format input email
    val email = EmailHelper.format(emailInput)
    // validate
    validate(username, email, firstName, lastName, EmailType.PRIMARY)
    // get the initial groups to assign to - only allowed to be empty if super user
    val groups = getInitialGroups(groupCodes, creator, authorities)
    // create the user
    val person = Person(firstName!!.trim(), lastName!!.trim())
    // obtain list of authorities that should be assigned for group
    val roles = groups.flatMap { it.assignableRoles }.filter { it.automatic }.mapNotNull { it.role }.toSet()

    val user = User(
      username = username,
      email = email,
      enabled = true,
      source = AuthSource.auth,
      person = person,
      authorities = roles,
      groups = groups,
    )
    return saveAndSendInitialEmail(url, user, creator, "AuthUserCreate", groups)
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(CreateUserException::class, NotificationClientException::class, VerifyEmailException::class)
  fun createUserByEmail(
    emailInput: String?,
    firstName: String?,
    lastName: String?,
    groupCodes: Set<String>?,
    url: String,
    creator: String,
    authorities: Collection<GrantedAuthority>,
  ): String {
    val email = EmailHelper.format(emailInput)
    validate(email, firstName, lastName, EmailType.PRIMARY)
    // get the initial groups to assign to - only allowed to be empty if super user
    val groups = getInitialGroups(groupCodes, creator, authorities)
    val person = Person(firstName!!.trim(), lastName!!.trim())
    // obtain list of authorities that should be assigned for group
    val roles = groups.flatMap { it.assignableRoles }.filter { it.automatic }.mapNotNull { it.role }.toSet()

    // username should now be set to a user's email address & ensure username always uppercase
    val username = StringUtils.upperCase(email)

    val user = User(
      username = username,
      email = email,
      enabled = true,
      source = AuthSource.auth,
      person = person,
      authorities = roles,
      groups = groups,
    )
    return saveAndSendInitialEmail(url, user, creator, "AuthUserCreate", groups)
  }

  private fun getInitialEmailSupportLink(groups: Collection<Group>): String {
    val serviceCode = groups.firstOrNull { it.groupCode.startsWith("PECS") }?.let { "BOOK_MOVE" } ?: "NOMIS"
    return oauthServiceRepository.findById(serviceCode).map { it.email!! }.orElseThrow()
  }

  fun findAuthUsers(
    name: String?,
    roleCodes: List<String>?,
    groupCodes: List<String>?,
    pageable: Pageable,
    searcher: String,
    authorities: Collection<GrantedAuthority>,
  ): Page<User> {
    val groupSearchCodes = if (authorities.any { it.authority == "ROLE_MAINTAIN_OAUTH_USERS" }) {
      groupCodes
    } else {
      val assignableGroupCodes = authUserGroupService.getAssignableGroups(searcher, authorities).map { it.groupCode }
      if (groupCodes.isNullOrEmpty()) assignableGroupCodes else groupCodes.filter { g -> assignableGroupCodes.any { it == g } }
    }
    val userFilter = UserFilter(name = name, roleCodes = roleCodes, groupCodes = groupSearchCodes)
    return userRepository.findAll(userFilter, pageable)
  }

  fun findAuthUsersByUsernames(usernames: List<String>): List<User> = userRepository.findByUsernameIn(usernames)

  @Throws(CreateUserException::class)
  private fun getInitialGroups(
    groupCodes: Set<String>?,
    creator: String,
    authorities: Collection<GrantedAuthority>
  ): Set<Group> {
    if (groupCodes.isNullOrEmpty()) {
      return if (authorities.any { it.authority == "ROLE_MAINTAIN_OAUTH_USERS" }) {
        emptySet()
      } else throw CreateUserException("groupCode", "missing")
    }
    val authUserGroups = authUserGroupService.getAssignableGroups(creator, authorities)
    val groups = authUserGroups.filter { it.groupCode in groupCodes }.toSet()

    if (groups.isEmpty()) {
      throw CreateUserException("groupCode", "notfound")
    }
    return groups
  }

  @Throws(NotificationClientException::class)
  private fun saveAndSendInitialEmail(
    url: String,
    user: User,
    creator: String,
    eventPrefix: String,
    groups: Collection<Group>,
  ): String { // then the reset token
    val userToken = user.createToken(UserToken.TokenType.RESET)
    // give users more time to do the reset
    userToken.tokenExpiry = LocalDateTime.now().plusDays(7)
    userRepository.save(user)
    // support link
    val supportLink = getInitialEmailSupportLink(groups)
    val setPasswordLink = url + userToken.token
    val username = user.username
    val email = user.email
    val parameters = mapOf(
      "firstName" to user.name,
      "fullName" to user.name,
      "resetLink" to setPasswordLink,
      "supportLink" to supportLink
    )
    // send the email
    try {
      log.info("Sending initial set password to notify for user {}", username)
      notificationClient.sendEmail(initialPasswordTemplateId, email, parameters, null)
      telemetryClient.trackEvent("${eventPrefix}Success", mapOf("username" to username, "admin" to creator), null)
    } catch (e: NotificationClientException) {
      val reason = (e.cause?.let { e.cause } ?: e).javaClass.simpleName
      log.warn("Failed to send create user notify for user {}", username, e)
      telemetryClient.trackEvent(
        "${eventPrefix}Failure",
        mapOf("username" to username, "reason" to reason, "admin" to creator),
        null
      )
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
  fun amendUserEmail(
    usernameInput: String,
    emailAddressInput: String?,
    url: String,
    admin: String,
    authorities: Collection<GrantedAuthority>,
    emailType: EmailType,
  ): String {
    val username = StringUtils.upperCase(usernameInput)
    val user = userRepository.findByUsernameAndMasterIsTrue(username)
      .orElseThrow { EntityNotFoundException("User not found with username $username") }
    maintainUserCheck.ensureUserLoggedInUserRelationship(admin, authorities, user)
    if (user.password != null) {
      user.verified = false
      userRepository.save(user)
      return verifyEmailService.changeEmailAndRequestVerification(
        username,
        emailAddressInput,
        user.firstName,
        user.name,
        url.replace("initial-password", "verify-email-confirm"),
        emailType
      ).link
    }
    val email = EmailHelper.format(emailAddressInput)
    verifyEmailService.validateEmailAddress(email, emailType)
    if (user.email == username.toLowerCase()) {
      userRepository.findByUsername(email!!.toUpperCase()).ifPresent {
        throw VerifyEmailException("duplicate")
      }
      user.username = email
    }
    user.email = email
    user.verified = false
    return saveAndSendInitialEmail(url, user, admin, "AuthUserAmend", user.groups)
  }

  fun findAuthUsersByEmail(email: String?): List<User> =
    userRepository.findByEmailAndMasterIsTrueOrderByUsername(EmailHelper.format(email))

  fun getAuthUserByUsername(username: String?): Optional<User> =
    userRepository.findByUsernameAndMasterIsTrue(StringUtils.upperCase(StringUtils.trim(username)))

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(AuthUserGroupRelationshipException::class)
  fun enableUser(usernameInDb: String, admin: String, authorities: Collection<GrantedAuthority>) =
    changeUserEnabled(usernameInDb, true, admin, authorities)

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(AuthUserGroupRelationshipException::class)
  fun disableUser(usernameInDb: String, admin: String, authorities: Collection<GrantedAuthority>) =
    changeUserEnabled(usernameInDb, false, admin, authorities)

  @Throws(AuthUserGroupRelationshipException::class)
  private fun changeUserEnabled(
    username: String,
    enabled: Boolean,
    admin: String,
    authorities: Collection<GrantedAuthority>,
  ) {
    val user = userRepository.findByUsernameAndMasterIsTrue(username)
      .orElseThrow { EntityNotFoundException("User not found with username $username") }
    maintainUserCheck.ensureUserLoggedInUserRelationship(admin, authorities, user)
    user.isEnabled = enabled
    // give user 7 days grace if last logged in more than x days ago
    if (user.lastLoggedIn.isBefore(LocalDateTime.now().minusDays(loginDaysTrigger.toLong()))) {
      user.lastLoggedIn = LocalDateTime.now().minusDays(loginDaysTrigger - 7L)
    }
    userRepository.save(user)
    telemetryClient.trackEvent(
      "AuthUserChangeEnabled",
      mapOf("username" to user.username, "enabled" to enabled.toString(), "admin" to admin),
      null
    )
  }

  // Old validate method.
  @Throws(CreateUserException::class, VerifyEmailException::class)
  private fun validate(username: String?, email: String?, firstName: String?, lastName: String?, emailType: EmailType) {
    if (username.isNullOrBlank() || username.length < MIN_LENGTH_USERNAME) throw CreateUserException(
      "username",
      "length"
    )

    if (username.length > MAX_LENGTH_USERNAME) throw CreateUserException("username", "maxlength")
    if (!username.matches("^[A-Z0-9_]*\$".toRegex())) throw CreateUserException("username", "format")

    validate(firstName, lastName)
    verifyEmailService.validateEmailAddress(email, emailType)
  }

  @Throws(CreateUserException::class, VerifyEmailException::class)
  private fun validate(email: String?, firstName: String?, lastName: String?, emailType: EmailType) {
    validate(firstName, lastName)

    if (email.isNullOrBlank() || email.length > MAX_LENGTH_EMAIL) throw CreateUserException("username", "maxlength")

    verifyEmailService.validateEmailAddress(email, emailType)
  }

  @Throws(CreateUserException::class)
  private fun validate(firstName: String?, lastName: String?) {
    if (firstName.isNullOrBlank()) throw CreateUserException("firstName", "required")
    else {
      if (firstName.contains('<') || firstName.contains('>')) throw CreateUserException("firstName", "invalid")
      if (firstName.length < MIN_LENGTH_FIRST_NAME) throw CreateUserException("firstName", "length")
      else if (firstName.length > MAX_LENGTH_FIRST_NAME) throw CreateUserException("firstName", "maxlength")
    }

    if (lastName.isNullOrBlank()) throw CreateUserException("lastName", "required")
    else {
      if (lastName.contains('<') || lastName.contains('>')) throw CreateUserException("lastName", "invalid")
      if (lastName.length < MIN_LENGTH_LAST_NAME) throw CreateUserException("lastName", "length")
      else if (lastName.length > MAX_LENGTH_LAST_NAME) throw CreateUserException("lastName", "maxlength")
    }
  }

  @Transactional(transactionManager = "authTransactionManager")
  fun lockUser(userPersonDetails: UserPersonDetails) {
    val username = userPersonDetails.username
    val userOptional = userRepository.findByUsername(username)
    val user = userOptional.orElseGet { userPersonDetails.toUser() }
    user.locked = true
    userRepository.save(user)
  }

  @Transactional(transactionManager = "authTransactionManager")
  fun unlockUser(userPersonDetails: UserPersonDetails) {
    val username = userPersonDetails.username
    val userOptional = userRepository.findByUsername(username)
    val user = userOptional.orElseGet { userPersonDetails.toUser() }
    user.locked = false
    // TODO: This isn't quite right - shouldn't always verify a user when unlocking...
    user.verified = true
    userRepository.save(user)
  }

  @Transactional(transactionManager = "authTransactionManager")
  fun changePassword(user: User, password: String) {
    // check user not setting password to existing password
    if (passwordEncoder.matches(password, user.password)) {
      throw ReusedPasswordException()
    }
    user.setPassword(passwordEncoder.encode(password))
    user.passwordExpiry = LocalDateTime.now().plusDays(passwordAge)
  }

  @Transactional(transactionManager = "authTransactionManager")
  @Throws(CreateUserException::class)
  fun amendUser(username: String, firstName: String?, lastName: String?) {
    validate(firstName, lastName)
    // will always be a user at this stage since we're retrieved it from the authentication
    val user = userRepository.findByUsernameAndSource(username, AuthSource.auth).orElseThrow()
    user.person!!.firstName = firstName!!.trim()
    user.person.lastName = lastName!!.trim()
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
    private const val MAX_LENGTH_EMAIL = 240
  }
}
