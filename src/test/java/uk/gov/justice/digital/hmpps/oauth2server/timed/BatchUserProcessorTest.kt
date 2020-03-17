package uk.gov.justice.digital.hmpps.oauth2server.timed

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.check
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class BatchUserProcessorTest {
  private val service: DisableInactiveAuthUsersService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private var disableInactiveAuthUsers: BatchUserProcessor = DisableInactiveAuthUsers(service, telemetryClient)

  @Test
  fun findAndProcessInBatches_noData() {
    whenever(service.processInBatches()).thenReturn(0)
    disableInactiveAuthUsers.findAndProcessInBatches()
    verify(service).processInBatches()
  }

  @Test
  fun findAndProcessInBatches_processed() {
    whenever(service.processInBatches()).thenReturn(10).thenReturn(3)
    disableInactiveAuthUsers.findAndProcessInBatches()
    verify(service, times(2)).processInBatches()
    verify(telemetryClient).trackEvent(eq("DisableInactiveAuthUsersFinished"), check {
      assertThat(it).contains(entry("errors", "0"), entry("total", "13"))
    }, isNull())
  }

  @Test
  fun findAndProcessInBatches_manyProcessed() {
    whenever(service.processInBatches())
        .thenReturn(10)
        .thenReturn(10)
        .thenReturn(10)
        .thenReturn(10)
        .thenReturn(3)
    disableInactiveAuthUsers.findAndProcessInBatches()
    verify(service, times(5)).processInBatches()
    verify(telemetryClient).trackEvent(eq("DisableInactiveAuthUsersFinished"), check {
      assertThat(it).contains(entry("errors", "0"), entry("total", "43"))
    }, isNull())
  }

  @Test
  fun findAndProcessInBatches_oneFailure() {
    whenever(service.processInBatches()).thenThrow(RuntimeException("bob")).thenReturn(5)
    disableInactiveAuthUsers.findAndProcessInBatches()
    verify(service, times(2)).processInBatches()
    verify(telemetryClient).trackEvent(eq("DisableInactiveAuthUsersFinished"), check {
      assertThat(it).contains(entry("errors", "1"), entry("total", "5"))
    }, isNull())
  }

  @Test
  fun findAndProcessInBatches_oneFailureTelemetry() {
    whenever(service.processInBatches()).thenThrow(RuntimeException("bob")).thenReturn(0)
    disableInactiveAuthUsers.findAndProcessInBatches()
    verify(telemetryClient).trackEvent(eq("DisableInactiveAuthUsersError"), isNull(), isNull())
  }

  @Test
  fun findAndProcessInBatches_manyFailures() {
    whenever(service.processInBatches())
        .thenThrow(RuntimeException("bob"))
        .thenThrow(RuntimeException("bob"))
        .thenThrow(RuntimeException("bob"))
    disableInactiveAuthUsers.findAndProcessInBatches()
    verify(service, times(3)).processInBatches()
    verify(telemetryClient).trackEvent(eq("DisableInactiveAuthUsersFinished"), check {
      assertThat(it).contains(entry("errors", "3"), entry("total", "0"))
    }, isNull())
  }
}
