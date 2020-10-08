package uk.gov.justice.digital.hmpps.oauth2server.timed

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Captor
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class DisableInactiveAuthUsersServiceTest {
  private val userRepository: UserRepository = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val service: DisableInactiveAuthUsersService =
    DisableInactiveAuthUsersService(userRepository, telemetryClient, 10)

  @Captor
  private lateinit var mapCaptor: ArgumentCaptor<Map<String, String>>

  @Test
  fun findAndDisableInactiveAuthUsers_Processed() {
    val users = listOf(User.of("user"), User.of("joe"))
    whenever(userRepository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(any()))
      .thenReturn(users)
    assertThat(service.processInBatches()).isEqualTo(2)
  }

  @Test
  fun findAndDisableInactiveAuthUsers_CheckAge() {
    val users = listOf(User.of("user"), User.of("joe"))
    whenever(userRepository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(any()))
      .thenReturn(users)
    assertThat(service.processInBatches()).isEqualTo(2)
    verify(userRepository).findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(
      check {
        assertThat(it).isBetween(LocalDateTime.now().minusDays(11), LocalDateTime.now().minusDays(9))
      }
    )
  }

  @Test
  fun findAndDisableInactiveAuthUsers_Disabled() {
    val users = listOf(
      User.builder().username("user").enabled(true).build(),
      User.builder().username("joe").enabled(true).build()
    )
    whenever(userRepository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(any()))
      .thenReturn(users)
    service.processInBatches()
    assertThat(users).extracting<Boolean> { it.isEnabled }.containsExactly(false, false)
  }

  @Test
  fun findAndDisableInactiveAuthUsers_Telemetry() {
    val users = listOf(
      User.builder().username("user").enabled(true).build(),
      User.builder().username("joe").enabled(true).build()
    )
    whenever(userRepository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(any()))
      .thenReturn(users)
    service.processInBatches()
    verify(telemetryClient, times(2)).trackEvent(
      ArgumentMatchers.eq("DisableInactiveAuthUsersProcessed"),
      mapCaptor.capture(),
      isNull()
    )
    assertThat(mapCaptor.allValues.map { it["username"] }).containsExactly("user", "joe")
  }
}
