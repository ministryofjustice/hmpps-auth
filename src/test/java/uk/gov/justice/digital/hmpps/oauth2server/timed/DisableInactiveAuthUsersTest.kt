package uk.gov.justice.digital.hmpps.oauth2server.timed

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify

class DisableInactiveAuthUsersTest {
  private val service: DisableInactiveAuthUsersService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private var disableInactiveAuthUsers = DisableInactiveAuthUsers(service, telemetryClient)

  @Test
  fun findAndDisableInactiveAuthUsers() {
    whenever(service.processInBatches()).thenReturn(0)
    disableInactiveAuthUsers.findAndDisableInactiveAuthUsers()
    verify(service).processInBatches()
  }
}
