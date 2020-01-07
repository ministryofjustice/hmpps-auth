package uk.gov.justice.digital.hmpps.oauth2server.service

import com.nhaarman.mockito_kotlin.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService

class MfaServiceTest {
  private val tokenService: TokenService = mock()
  private val request = MockHttpServletRequest()
  private lateinit var service: MfaService

  @Before
  fun setUp() {
    service = MfaService(setOf("12.21.23.24"), setOf("MFA"), tokenService)
    request.remoteAddr = "0:0:0:0:0:0:0:1"
    RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request, null))
  }

  @After
  fun tearDown() {
    RequestContextHolder.resetRequestAttributes()
  }

  @Test
  fun `needsMfa whitelisted IP`() {
    request.remoteAddr = "12.21.23.24"

    assertThat(service.needsMfa(emptySet())).isFalse()
  }

  @Test
  fun `needsMfa non whitelisted IP`() {
    assertThat(service.needsMfa(emptySet())).isFalse()
  }

  @Test
  fun `needsMfa non whitelisted IP enabled role`() {
    assertThat(service.needsMfa(setOf(SimpleGrantedAuthority("MFA")))).isTrue()
  }
}
