package uk.gov.justice.digital.hmpps.oauth2server.timed

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserRetries
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRetriesRepository
import java.time.LocalDateTime
import java.util.function.Consumer

@Service
class DeleteDisabledUsersService(
  private val repository: UserRepository,
  private val userRetriesRepository: UserRetriesRepository,
  private val telemetryClient: TelemetryClient,
) : BatchUserService {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(transactionManager = "authTransactionManager")
  override fun processInBatches(): Int {
    val usersToDelete = repository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(
      LocalDateTime.now().minusYears(1)
    )
    usersToDelete.forEach(
      Consumer { user: User ->
        val username = user.username
        userRetriesRepository.findById(username)
          .ifPresent { entity: UserRetries -> userRetriesRepository.delete(entity) }
        repository.delete(user)
        log.debug("Deleting auth user {} due to inactivity", username)
        telemetryClient.trackEvent("DeleteDisabledUsersProcessed", mapOf("username" to username), null)
      }
    )
    return usersToDelete.size
  }
}
