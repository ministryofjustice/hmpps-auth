package uk.gov.justice.digital.hmpps.oauth2server.security

import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserRetries
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRetriesRepository
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.service.DelegatingUserService
import java.time.LocalDateTime

@Service
@Slf4j
@Transactional(transactionManager = "authTransactionManager")
open class UserRetriesService(private val userRetriesRepository: UserRetriesRepository,
                              private val userRepository: UserRepository,
                              private val delegatingUserService: DelegatingUserService,
                              @Value("\${application.authentication.lockout-count}") private val accountLockoutCount: Int) {

  open fun resetRetriesAndRecordLogin(userPersonDetails: UserPersonDetails) {
    val username = userPersonDetails.username
    resetRetries(username)
    // and record last logged in as now too (doing for all users to prevent confusion)
    val userOptional = userRepository.findByUsername(username)
    val user = userOptional.orElseGet { userPersonDetails.toUser() }
    user.lastLoggedIn = LocalDateTime.now()
    // copy across email address on each successful login
    if (userPersonDetails is DeliusUserPersonDetails) {
      user.email = userPersonDetails.email
      user.isVerified = true
    }
    userRepository.save(user)
  }

  open fun incrementRetriesAndLockAccountIfNecessary(userPersonDetails: UserPersonDetails): Boolean {
    val username = userPersonDetails.username
    val retriesOptional = userRetriesRepository.findById(username)
    val userRetries = retriesOptional.orElse(UserRetries(username, 0))
    val retryCount = userRetries.retryCount
    // not locked, so just increment and save
    if (retryCount < accountLockoutCount - 1) {
      userRetries.incrementRetryCount()
      userRetriesRepository.save(userRetries)
      return false
    }
    // otherwise lock
    delegatingUserService.lockAccount(userPersonDetails)
    // and reset retries otherwise if account is unlocked in c-nomis then user won't be allowed in
    resetRetries(userPersonDetails.username)
    return true
  }

  private fun resetRetries(username: String) { // reset their retry count
    userRetriesRepository.save(UserRetries(username, 0))
  }
}
