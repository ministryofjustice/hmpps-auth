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
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import java.util.*
import java.util.stream.Collectors

@Service
@Transactional(readOnly = true)
class UserService(private val nomisUserService: NomisUserService,
                  private val authUserService: AuthUserService,
                  private val deliusUserService: DeliusUserService,
                  private val userRepository: UserRepository,
                  private val verifyEmailService: VerifyEmailService) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    fun findMasterUserPersonDetails(username: String): Optional<UserPersonDetails> =
            authUserService.getAuthUserByUsername(username).map { UserPersonDetails::class.java.cast(it) }
                    .or { nomisUserService.getNomisUserByUsername(username).map { UserPersonDetails::class.java.cast(it) } }
                    .or { deliusUserService.getDeliusUserByUsername(username).map { UserPersonDetails::class.java.cast(it) } }

    fun findUser(username: String): Optional<User> = userRepository.findByUsername(StringUtils.upperCase(username))

    fun getUser(username: String): User = findUser(username).orElseThrow { UsernameNotFoundException("User with username $username not found") }

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
                        getEmailAddressFromNomis(username).ifPresent {
                            email -> user.email = email
                            user.isVerified = true
                        }
                    }
                    userRepository.save(user)
                }
            }

    fun getEmailAddressFromNomis(username: String): Optional<String> {
        val emailAddresses = verifyEmailService.getExistingEmailAddresses(username)
        val justiceEmail = emailAddresses
                .stream()
                .filter { email -> email.endsWith("justice.gov.uk") }
                .collect(Collectors.toList())
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
}
