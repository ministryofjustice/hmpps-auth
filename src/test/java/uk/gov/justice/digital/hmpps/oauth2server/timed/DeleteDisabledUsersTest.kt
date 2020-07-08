package uk.gov.justice.digital.hmpps.oauth2server.timed

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify

class DeleteDisabledUsersTest {
  private val service: DeleteDisabledUsersService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val deleteDisabledUsers: DeleteDisabledUsers = DeleteDisabledUsers(service, telemetryClient)

  @Test
  fun findAndDeleteDisabledUsers() {
    whenever(service.processInBatches()).thenReturn(0)
    deleteDisabledUsers.findAndDeleteDisabledUsers()
    verify(service).processInBatches()
  }
}
