package uk.gov.justice.digital.hmpps.oauth2server.security

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import java.util.*

@Service
@Transactional(readOnly = true)
class UserService(private val nomisUserService: NomisUserService,
                  private val authUserService: AuthUserService,
                  private val deliusUserService: DeliusUserService,
                  private val userRepository: UserRepository) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun findMasterUserPersonDetails(username: String): Optional<UserPersonDetails> =
      authUserService.getAuthUserByUsername(username).map { UserPersonDetails::class.java.cast(it) }
          .or { nomisUserService.getNomisUserByUsername(username).map { UserPersonDetails::class.java.cast(it) } }
          .or { deliusUserService.getDeliusUserByUsername(username).map { UserPersonDetails::class.java.cast(it) } }

  fun findUser(username: String): Optional<User> = userRepository.findByUsername(StringUtils.upperCase(username))

  fun getUser(username: String): User = findUser(username).orElseThrow { UsernameNotFoundException("User with username $username not found") }

  @Transactional(transactionManager = "authTransactionManager")
  fun getOrCreateUser(username: String): User =
      findUser(username).orElseGet {
        val userPersonDetails = findMasterUserPersonDetails(username).orElseThrow()
        userRepository.save(userPersonDetails.toUser())
      }

  fun hasVerifiedEmail(userDetails: UserPersonDetails): Boolean {
    val user: User = findUser(userDetails.username).orElseGet { userDetails.toUser() }
    return StringUtils.isNotEmpty(user.email) && user.isVerified
  }

  fun isSameAsCurrentVerifiedMobile(username: String, mobile: String): Boolean {
    val user: User = findUser(username).orElseThrow()
    val canonicalMobile = mobile.replace("\\s+".toRegex(), "")
    return user.isMobileVerified && canonicalMobile == user.mobile
  }

  fun isSameAsCurrentVerifiedEmail(username: String, email: String): Boolean {
    val user: User = findUser(username).orElseThrow()
    return user.isVerified && email == user.email
  }
}
