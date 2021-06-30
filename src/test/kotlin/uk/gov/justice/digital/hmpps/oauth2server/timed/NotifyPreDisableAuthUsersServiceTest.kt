package uk.gov.justice.digital.hmpps.oauth2server.timed

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Captor
import org.mockito.Mockito.eq
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.service.notify.NotificationClientApi

@ExtendWith(MockitoExtension::class)
class NotifyPreDisableAuthUsersServiceTest {
  private val userRepository: UserRepository = mock()
  private val notificationClient: NotificationClientApi = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val service: NotifyPreDisableAuthUsersService =
    NotifyPreDisableAuthUsersService(
      userRepository,
      notificationClient,
      telemetryClient,
      10,
      "template",
      "signin-url",
      "support-url"
    )

  @Captor
  private lateinit var mapCaptor: ArgumentCaptor<Map<String, String>>

  @Test
  fun findAndWarnPreDisableInactiveAuthUsers_Processed() {
    val users = listOf(createSampleUser(username = "user", enabled = true), createSampleUser(username = "joe", enabled = true))
    whenever(
      userRepository.findByLastLoggedInBeforeAndEnabledIsTrueAndPreDisableWarningIsFalseAndSourceOrderByLastLoggedIn(
        any(),
        any()
      )
    )
      .thenReturn(users)
    assertThat(service.processInBatches()).isEqualTo(2)
    verifyNoInteractions(notificationClient)
  }

  @Test
  fun findAndWarnPreDisableInactiveAuthUsers_deactivateWarningSent() {
    val users = listOf(createSampleUser(username = "user", email = "bob@justice.gov.uk", verified = true, enabled = true))
    whenever(
      userRepository.findByLastLoggedInBeforeAndEnabledIsTrueAndPreDisableWarningIsFalseAndSourceOrderByLastLoggedIn(
        any(),
        any()
      )
    )
      .thenReturn(users)
    assertThat(service.processInBatches()).isEqualTo(1)
    assertThat(users).extracting<Boolean> { it.preDisableWarning }.containsExactly(true,)
    verify(notificationClient).sendEmail(
      eq("template"),
      eq("bob@justice.gov.uk"),
      check {
        assertThat(it["username"]).isEqualTo("user")
      },
      isNull()
    )
  }

  @Test
  fun findAndDisableInactiveAuthUsers_emailVerified_Telemetry() {
    val users = listOf(
      createSampleUser(username = "user", verified = true, enabled = true),
      createSampleUser(username = "joe", verified = true, enabled = true),
    )
    whenever(
      userRepository.findByLastLoggedInBeforeAndEnabledIsTrueAndPreDisableWarningIsFalseAndSourceOrderByLastLoggedIn(
        any(), any()
      )
    )
      .thenReturn(users)
    service.processInBatches()
    verify(telemetryClient, times(2)).trackEvent(
      ArgumentMatchers.eq("AuthNotifyPreDisableInactiveAuthUsersProcessed"),
      mapCaptor.capture(),
      ArgumentMatchers.isNull()
    )
    assertThat(mapCaptor.allValues.map { it["username"] }).containsExactly("user", "joe")
  }

  @Test
  fun findAndDisableInactiveAuthUsers_emailNotVerified_Telemetry() {
    val users = listOf(
      createSampleUser(username = "user", enabled = true),
      createSampleUser(username = "joe", enabled = true),
    )
    whenever(
      userRepository.findByLastLoggedInBeforeAndEnabledIsTrueAndPreDisableWarningIsFalseAndSourceOrderByLastLoggedIn(
        any(), any()
      )
    )
      .thenReturn(users)
    service.processInBatches()
    verify(telemetryClient, times(2)).trackEvent(
      ArgumentMatchers.eq("AuthNotifyPreDisableInactiveAuthUsersFailure"),
      mapCaptor.capture(),
      ArgumentMatchers.isNull()
    )
    assertThat(mapCaptor.allValues.map { it["username"] }).containsExactly("user", "joe")
    assertThat(mapCaptor.allValues.map { it["reason"] }).containsExactly("Primary email not verified", "Primary email not verified")
  }
}
