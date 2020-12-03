package uk.gov.justice.digital.hmpps.oauth2server.timed

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.OauthCodeRepository
import java.time.LocalDateTime

/**
 * Delete users that haven't logged into the system for a year.  Will only affect auth users that are disabled,
 * user data for other users will be removed if they haven't logged in for the last year too.
 */
@Component
class RemoveExpiredAuthCodes(private val service: RemoveExpiredAuthCodesService) {

  @Scheduled(
    fixedDelayString = "\${application.authentication.remove-codes.frequency}",
    initialDelayString = "\${random.int[600000,\${application.authentication.remove-codes.frequency}]}"
  )
  fun removeExpiredAuthCodes() {
    try {
      service.removeExpiredAuthCodes()
    } catch (e: Exception) {
      // have to catch the exception here otherwise scheduling will stop
      log.error("Caught exception {} during expired auth code removal", e.javaClass.simpleName, e)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

@Service
class RemoveExpiredAuthCodesService(private val repository: OauthCodeRepository) {
  @Transactional(transactionManager = "authTransactionManager")
  fun removeExpiredAuthCodes() {
    val oneDayAgo = LocalDateTime.now().minusDays(1)
    repository.deleteByCreatedDateBefore(oneDayAgo)
  }
}
