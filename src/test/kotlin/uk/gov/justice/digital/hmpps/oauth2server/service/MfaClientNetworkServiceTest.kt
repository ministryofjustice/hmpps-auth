@file:Suppress("ClassName", "DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

internal class MfaClientNetworkServiceTest {
  private val request = MockHttpServletRequest()
  private val service = MfaClientNetworkService(setOf("12.21.23.24"), setOf("MFA"))

  @BeforeEach
  fun setUp() {
    request.remoteAddr = "0:0:0:0:0:0:0:1"
    RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request, null))
  }

  @AfterEach
  fun tearDown(): Unit = RequestContextHolder.resetRequestAttributes()

  @Nested
  inner class outsideApprovedNetwork {
    @Test
    fun `allowlisted IP`() {
      request.remoteAddr = "12.21.23.24"

      assertThat(service.outsideApprovedNetwork()).isFalse
    }

    @Test
    fun `non allowlisted IP`() {
      assertThat(service.outsideApprovedNetwork()).isTrue
    }
  }

  @Nested
  inner class needsMfa {
    @Test
    fun `allowlisted IP`() {
      request.remoteAddr = "12.21.23.24"

      assertThat(service.needsMfa(emptySet())).isFalse
    }

    @Test
    fun `non allowlisted IP`() {
      assertThat(service.needsMfa(emptySet())).isFalse
    }

    @Test
    fun `non allowlisted IP enabled role`() {
      assertThat(service.needsMfa(setOf(SimpleGrantedAuthority("MFA")))).isTrue
    }
  }
}
