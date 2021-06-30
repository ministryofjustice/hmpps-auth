package uk.gov.justice.digital.hmpps.oauth2server.timed

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify

class NotifyPreDisableAuthUsersTest {
  private val service: NotifyPreDisableAuthUsersService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private var notifyPreDisableAuthUsers = NotifyPreDisableAuthUsers(service, telemetryClient)

  @Test
  fun findAndDisableInactiveAuthUsers() {
    whenever(service.processInBatches()).thenReturn(0)
    notifyPreDisableAuthUsers.findAndNotifyPreDisableInactiveAuthUsers()
    verify(service).processInBatches()
  }
}
