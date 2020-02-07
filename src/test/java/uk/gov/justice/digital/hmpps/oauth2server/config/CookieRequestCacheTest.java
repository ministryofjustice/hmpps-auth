package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Base64Utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class CookieRequestCacheTest {
    @Mock
    private SavedRequestCookieHelper helper;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Captor
    private ArgumentCaptor<String> captor;

    private CookieRequestCache cache;

    @BeforeEach
    void setUp() {
        cache = new CookieRequestCache(helper);
    }

    @Test
    void saveRequest_secureRequest() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.com:443/where"));
        when(request.getQueryString()).thenReturn("param=value");
        when(request.isSecure()).thenReturn(Boolean.TRUE);

        cache.saveRequest(request, response);

        verify(helper).addCookieToResponse(same(request), same(response), captor.capture());

        final var url = new String(Base64Utils.decodeFromString(captor.getValue()));
        assertThat(url).isEqualTo("https://some.com/where?param=value");
    }

    @Test
    void saveRequest_insecureRequest() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("https://some.com/where"));
        when(request.getQueryString()).thenReturn("param=value");
        when(request.isSecure()).thenReturn(Boolean.FALSE);

        cache.saveRequest(request, response);

        verify(helper).addCookieToResponse(same(request), same(response), captor.capture());

        final var url = new String(Base64Utils.decodeFromString(captor.getValue()));
        assertThat(url).isEqualTo("http://some.com/where?param=value");
    }

    @Test
    void saveRequest_differentPort() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("https://some.com:12345/where"));
        when(request.getQueryString()).thenReturn("param=value");
        when(request.isSecure()).thenReturn(Boolean.TRUE);

        cache.saveRequest(request, response);

        verify(helper).addCookieToResponse(same(request), same(response), captor.capture());

        final var url = new String(Base64Utils.decodeFromString(captor.getValue()));
        assertThat(url).isEqualTo("https://some.com:12345/where?param=value");
    }

    @Test
    void getRequest() {
        when(helper.readValueFromCookie(request)).thenReturn(
                Optional.of(Base64Utils.encodeToString("https://some.com/where?param=value".getBytes())));

        final var savedRequest = cache.getRequest(request, response);

        assertThat(savedRequest.getRedirectUrl()).isEqualTo("https://some.com/where?param=value");
    }

    @Test
    void getMatchingRequest_matches() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.com:443/where"));
        when(request.getQueryString()).thenReturn("param=value");
        when(request.isSecure()).thenReturn(Boolean.TRUE);

        when(helper.readValueFromCookie(request)).thenReturn(
                Optional.of(Base64Utils.encodeToString("https://some.com/where?param=value".getBytes())));

        final var savedRequest = cache.getMatchingRequest(request, response);

        assertThat(savedRequest).isSameAs(request);

        verify(helper).removeCookie(request, response);
    }

    @Test
    void getMatchingRequest_nomatch() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.com:443/where"));
        when(request.getQueryString()).thenReturn("param=othervalue");
        when(request.isSecure()).thenReturn(Boolean.TRUE);

        when(helper.readValueFromCookie(request)).thenReturn(
                Optional.of(Base64Utils.encodeToString("https://some.com/where?param=value".getBytes())));

        final var savedRequest = cache.getMatchingRequest(request, response);

        assertThat(savedRequest).isNull();

        verify(helper, never()).removeCookie(request, response);
    }

    @Test
    void getMatchingRequest_noSavedRequest() {
        when(helper.readValueFromCookie(request)).thenReturn(Optional.empty());

        final var savedRequest = cache.getMatchingRequest(request, response);

        assertThat(savedRequest).isNull();
    }
}
