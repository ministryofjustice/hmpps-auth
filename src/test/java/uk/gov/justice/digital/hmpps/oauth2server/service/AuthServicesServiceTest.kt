package uk.gov.justice.digital.hmpps.oauth2server.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.OauthServiceRepository

class AuthServicesServiceTest {
  private val oauthServiceRepository: OauthServiceRepository = mock()
  private val authServicesService = AuthServicesService(oauthServiceRepository)

  @Test
  fun `calls repository find all`() {
    val services = mutableListOf(Service())
    whenever(oauthServiceRepository.findAllByOrderByName()).thenReturn(services)
    assertThat(authServicesService.list()).isSameAs(services)
    verify(oauthServiceRepository).findAllByOrderByName()
  }
}
