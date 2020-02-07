package uk.gov.justice.digital.hmpps.oauth2server.utils

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

internal class IpAddressHelperTest {
  private val request = MockHttpServletRequest()
  @AfterEach
  fun tearDown() {
    RequestContextHolder.resetRequestAttributes()
  }

  @Test
  fun retrieveIpFromRemoteAddr_testStandardV4IP() {
    request.remoteAddr = "127.0.0.1"
    val addr = IpAddressHelper.retrieveIpFromRemoteAddr(request)
    assertThat(addr).isEqualTo("127.0.0.1")
  }

  @Test
  fun retrieveIpFromRemoteAddr_testRemoteAddressWithPort() {
    request.remoteAddr = "82.34.12.11:13321"
    val addr = IpAddressHelper.retrieveIpFromRemoteAddr(request)
    assertThat(addr).isEqualTo("82.34.12.11")
  }

  @Test
  fun retrieveIpFromRemoteAddr_testIpV6Address() {
    request.remoteAddr = "0:0:0:0:0:0:0:1"
    val addr = IpAddressHelper.retrieveIpFromRemoteAddr(request)
    assertThat(addr).isEqualTo("0:0:0:0:0:0:0:1")
  }

  @Test
  fun retrieveIpFromRequest() {
    RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request, null))
    request.remoteAddr = "0:0:0:0:0:0:0:1"
    val addr = IpAddressHelper.retrieveIpFromRequest()
    assertThat(addr).isEqualTo("0:0:0:0:0:0:0:1")
  }

  @Test
  fun retrieveIpFromRequest_NotSet() {
    assertThatThrownBy { IpAddressHelper.retrieveIpFromRequest() }.isInstanceOf(IllegalStateException::class.java)
  }
}
