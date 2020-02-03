package uk.gov.justice.digital.hmpps.oauth2server.security

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import java.util.*

@Service
@Transactional(readOnly = true)
open class UserService(private val nomisUserService: NomisUserService,
                       private val authUserService: AuthUserService,
                       private val deliusUserService: DeliusUserService,
                       private val userRepository: UserRepository) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  open fun findMasterUserPersonDetails(username: String): Optional<UserPersonDetails> =
      authUserService.getAuthUserByUsername(username).map { UserPersonDetails::class.java.cast(it) }
          .or { nomisUserService.getNomisUserByUsername(username).map { UserPersonDetails::class.java.cast(it) } }
          .or { deliusUserService.getDeliusUserByUsername(username).map { UserPersonDetails::class.java.cast(it) } }

  open fun findUser(username: String): Optional<User> = userRepository.findByUsername(StringUtils.upperCase(username))

  @Transactional(transactionManager = "authTransactionManager")
  open fun getOrCreateUser(username: String): User =
      findUser(username).orElseGet {
        val userPersonDetails = findMasterUserPersonDetails(username).orElseThrow()
        userRepository.save(userPersonDetails.toUser())
      }

  open fun hasVerifiedEmail(userDetails: UserPersonDetails): Boolean {
    val user: User = findUser(userDetails.username).orElseGet { userDetails.toUser() }
    return StringUtils.isNotEmpty(user.email) && user.isVerified
  }
}
