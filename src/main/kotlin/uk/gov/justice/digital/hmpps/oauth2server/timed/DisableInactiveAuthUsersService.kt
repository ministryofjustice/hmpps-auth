package uk.gov.justice.digital.hmpps.oauth2server.timed

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import java.time.LocalDateTime
import java.util.function.Consumer

@Service
class DisableInactiveAuthUsersService(
  private val repository: UserRepository,
  private val telemetryClient: TelemetryClient,
  @Value("\${application.authentication.disable.login-days}") private val loginDays: Int,
) : BatchUserService {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(transactionManager = "authTransactionManager")
  override fun processInBatches(): Int {
    val usersToDisable = repository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(
      LocalDateTime.now().minusDays(loginDays.toLong())
    )
    usersToDisable.forEach(
      Consumer { user: User ->
        user.isEnabled = false
        user.inactiveReason = "INACTIVE FOR 90 DAYS"
        log.debug("Disabling auth user {} due to inactivity", user.username)
        telemetryClient.trackEvent("DisableInactiveAuthUsersProcessed", mapOf("username" to user.username), null)
      }
    )
    return usersToDisable.size
  }
}
