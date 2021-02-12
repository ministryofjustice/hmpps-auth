package uk.gov.justice.digital.hmpps.oauth2server.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class AuthIpSecurityTest {
  @Test
  fun testStandardV4IP() {
    val request = MockHttpServletRequest()
    request.remoteAddr = "127.0.0.1"
    val testClass = AuthIpSecurity(setOf("0.0.0.0/0"))
    val check = testClass.check(request)
    assertThat(check).isTrue()
  }

  @Test
  fun testRemoteAddressWithPort() {
    val request = MockHttpServletRequest()
    request.remoteAddr = "82.34.12.11:13321"
    val testClass = AuthIpSecurity(setOf("0.0.0.0/0"))
    val check = testClass.check(request)
    assertThat(check).isTrue()
  }

  @Test
  fun testRemoteAddressWithPortNoInAllowlist() {
    val request = MockHttpServletRequest()
    request.remoteAddr = "82.34.12.11:13321"
    val testClass = AuthIpSecurity(setOf("82.34.12.10/32", "82.34.12.12/32"))
    val check = testClass.check(request)
    assertThat(check).isFalse()
  }

  @Test
  fun testIpV6Address() {
    val request = MockHttpServletRequest()
    request.remoteAddr = "0:0:0:0:0:0:0:1"
    val testClass = AuthIpSecurity(setOf("0:0:0:0:0:0:0:1", "127.0.0.1/32"))
    val check = testClass.check(request)
    assertThat(check).isTrue()
  }
}
