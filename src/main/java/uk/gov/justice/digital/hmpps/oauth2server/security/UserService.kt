package uk.gov.justice.digital.hmpps.oauth2server.security

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.EmailType
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.azure.service.AzureUserService
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import java.util.Optional

@Service
@Transactional(readOnly = true)
class UserService(
  private val nomisUserService: NomisUserService,
  private val authUserService: AuthUserService,
  private val deliusUserService: DeliusUserService,
  private val azureUserService: AzureUserService,
  private val userRepository: UserRepository,
  private val verifyEmailService: VerifyEmailService
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    fun isHmppsEmail(email: String) = email.endsWith("justice.gov.uk")
  }

  fun findMasterUserPersonDetails(username: String): Optional<UserPersonDetails> =
    authUserService.getAuthUserByUsername(username).map { UserPersonDetails::class.java.cast(it) }
      .or { nomisUserService.getNomisUserByUsername(username).map { UserPersonDetails::class.java.cast(it) } }
      .or { azureUserService.getAzureUserByUsername(username).map { UserPersonDetails::class.java.cast(it) } }
      .or { deliusUserService.getDeliusUserByUsername(username).map { UserPersonDetails::class.java.cast(it) } }

  fun findUser(username: String): Optional<User> = userRepository.findByUsername(StringUtils.upperCase(username))

  fun getUser(username: String): User =
    findUser(username).orElseThrow { UsernameNotFoundException("User with username $username not found") }

  fun getUserWithContacts(username: String): User = findUser(username)
    .map {
      // initialise contacts by calling size
      it.contacts.size
      it
    }
    .orElseThrow { UsernameNotFoundException("User with username $username not found") }

  @Transactional(transactionManager = "authTransactionManager")
  fun getOrCreateUser(username: String): Optional<User> =
    findUser(username).or {
      findMasterUserPersonDetails(username).map {
        val user = it.toUser()
        if (AuthSource.valueOf(user.authSource) == AuthSource.nomis) {
          getEmailAddressFromNomis(username).ifPresent { email ->
            user.email = email
            user.isVerified = true
          }
        }
        userRepository.save(user)
      }
    }

  fun getEmailAddressFromNomis(username: String): Optional<String> {
    val emailAddresses = verifyEmailService.getExistingEmailAddressesForUsername(username)
    val justiceEmail = emailAddresses
      .filter(UserService::isHmppsEmail)
      .toList()
    return if (justiceEmail.size == 1) Optional.of(justiceEmail[0]) else Optional.empty()
  }

  fun hasVerifiedMfaMethod(userDetails: UserPersonDetails): Boolean {
    val user = findUser(userDetails.username).orElseGet { userDetails.toUser() }
    return user.hasVerifiedMfaMethod()
  }

  fun isSameAsCurrentVerifiedMobile(username: String, mobile: String?): Boolean {
    val user = getUser(username)
    val canonicalMobile = mobile?.replace("\\s+".toRegex(), "")
    return user.isMobileVerified && canonicalMobile == user.mobile
  }

  fun isSameAsCurrentVerifiedEmail(username: String, email: String, emailType: EmailType): Boolean {
    val user = getUser(username)
    if (emailType == EmailType.SECONDARY) {
      return user.isSecondaryEmailVerified && email == user.secondaryEmail
    }
    return user.isVerified && email == user.email
  }

  fun findPrisonUsersByFirstAndLastNames(firstName: String, lastName: String): List<User> {
    val nomisUsers: List<NomisUserPersonDetails> =
      nomisUserService.findPrisonUsersByFirstAndLastNames(firstName, lastName)
    val nomisUsernames = nomisUsers.map { it.username }

    val authUsers: List<User> = authUserService
      .findAuthUsersByUsernames(nomisUsernames)
      .filter {
        !it.email.isNullOrBlank() && it.source == AuthSource.nomis
      }

    val authUsernames = authUsers.map { it.username }

    val missingUsernames = nomisUsernames.minus(authUsernames)

    val emailsByUsername = verifyEmailService.getExistingEmailAddressesForUsernames(missingUsernames)

    val validEmailByUsername = emailsByUsername
      .mapValues { (_, emails) -> emails.filter(UserService::isHmppsEmail) }
      .filter { (_, emails) -> emails.size == 1 }
      .mapValues { (_, emails) -> emails.first() }

    val usersFromNomisUsers = nomisUsers
      .filter { user -> missingUsernames.contains(user.username) }
      .map { upd ->
        val user = upd.toUser()
        user.email = validEmailByUsername[user.username]
        user.isVerified = validEmailByUsername.containsKey(user.username)
        user
      }
    return authUsers.plus(usersFromNomisUsers)
  }
}
