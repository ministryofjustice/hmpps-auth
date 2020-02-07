package uk.gov.justice.digital.hmpps.oauth2server.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IpAddressHelperTest {

    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void retrieveIpFromRemoteAddr_testStandardV4IP() {
        request.setRemoteAddr("127.0.0.1");

        final var addr = IpAddressHelper.retrieveIpFromRemoteAddr(request);
        assertThat(addr).isEqualTo("127.0.0.1");
    }

    @Test
    void retrieveIpFromRemoteAddr_testRemoteAddressWithPort() {
        request.setRemoteAddr("82.34.12.11:13321");

        final var addr = IpAddressHelper.retrieveIpFromRemoteAddr(request);
        assertThat(addr).isEqualTo("82.34.12.11");
    }

    @Test
    void retrieveIpFromRemoteAddr_testIpV6Address() {
        request.setRemoteAddr("0:0:0:0:0:0:0:1");

        final var addr = IpAddressHelper.retrieveIpFromRemoteAddr(request);
        assertThat(addr).isEqualTo("0:0:0:0:0:0:0:1");
    }

    @Test
    void retrieveIpFromRequest() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, null));
        request.setRemoteAddr("0:0:0:0:0:0:0:1");

        final var addr = IpAddressHelper.retrieveIpFromRequest();
        assertThat(addr).isEqualTo("0:0:0:0:0:0:0:1");
    }

    @Test
    void retrieveIpFromRequest_NotSet() {
        assertThatThrownBy(IpAddressHelper::retrieveIpFromRequest).isInstanceOf(IllegalStateException.class);
    }
}
