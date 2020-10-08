package uk.gov.justice.digital.hmpps.oauth2server.timed

import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.OauthCodeRepository
import java.time.LocalDateTime

class RemoveExpiredAuthCodesServiceTest {
  val repository: OauthCodeRepository = mock()
  val removeExpiredAuthCodesService = RemoveExpiredAuthCodesService(repository)

  @Test
  fun removeExpiredAuthCodes() {
    removeExpiredAuthCodesService.removeExpiredAuthCodes()
    verify(repository).deleteByCreatedDateBefore(
      check {
        val now = LocalDateTime.now()
        assertThat(it).isBetween(now.minusDays(1).minusMinutes(1), now.minusDays(1).plusMinutes(1))
      }
    )
  }
}
