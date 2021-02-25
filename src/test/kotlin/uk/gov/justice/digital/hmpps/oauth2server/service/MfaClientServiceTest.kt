@file:Suppress("ClassName", "DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.ClientDetailsService
import uk.gov.justice.digital.hmpps.oauth2server.resource.MfaAccess

internal class MfaClientServiceTest {
  private val clientDetailsService: ClientDetailsService = mock()
  private val clientDetails: ClientDetails = mock()
  private val mfaClientNetworkService: MfaClientNetworkService = mock()
  private val service = MfaClientService(clientDetailsService, mfaClientNetworkService)

  @Nested
  inner class clientNeedsMfa {
    @Test
    fun `client doesn't needs mfa on trusted network`() {
      whenever(mfaClientNetworkService.outsideApprovedNetwork()).thenReturn(false)
      whenever(clientDetails.additionalInformation).thenReturn(mapOf("mfa" to MfaAccess.untrusted.name))
      whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(clientDetails)

      assertThat(service.clientNeedsMfa("bob")).isFalse
    }

    @Test
    fun `client needs mfa on untrusted network`() {
      whenever(mfaClientNetworkService.outsideApprovedNetwork()).thenReturn(true)
      whenever(clientDetails.additionalInformation).thenReturn(mapOf("mfa" to MfaAccess.untrusted.name))
      whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(clientDetails)

      assertThat(service.clientNeedsMfa("bob")).isTrue
    }

    @Test
    fun `client needs mfa everywhere`() {
      whenever(clientDetails.additionalInformation).thenReturn(mapOf("mfa" to MfaAccess.all.name))
      whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(clientDetails)

      assertThat(service.clientNeedsMfa("bob")).isTrue
    }

    @Test
    fun `client doesn't need mfa`() {
      whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(clientDetails)

      assertThat(service.clientNeedsMfa("bob")).isFalse
    }
  }
}
