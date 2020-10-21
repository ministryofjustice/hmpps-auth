package uk.gov.justice.digital.hmpps.oauth2server.config

import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.same
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.util.Base64Utils
import java.util.Optional
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class CookieRequestCacheTest {
  private val helper: SavedRequestCookieHelper = mock()
  private val request: HttpServletRequest = mock()
  private val response: HttpServletResponse = mock()
  private val cache = CookieRequestCache(helper)

  @Test
  fun saveRequest_secureRequest() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.com:443/where"))
    whenever(request.queryString).thenReturn("param=value")
    whenever(request.isSecure).thenReturn(true)
    cache.saveRequest(request, response)
    verify(helper).addCookieToResponse(
      same(request),
      same(response),
      check {
        val url = String(Base64Utils.decodeFromString(it))
        assertThat(url).isEqualTo("https://some.com/where?param=value")
      }
    )
  }

  @Test
  fun saveRequest_insecureRequest() {
    whenever(request.requestURL).thenReturn(StringBuffer("https://some.com/where"))
    whenever(request.queryString).thenReturn("param=value")
    whenever(request.isSecure).thenReturn(false)
    cache.saveRequest(request, response)
    verify(helper).addCookieToResponse(
      same(request),
      same(response),
      check {
        val url = String(Base64Utils.decodeFromString(it))
        assertThat(url).isEqualTo("http://some.com/where?param=value")
      }
    )
  }

  @Test
  fun saveRequest_differentPort() {
    whenever(request.requestURL).thenReturn(StringBuffer("https://some.com:12345/where"))
    whenever(request.queryString).thenReturn("param=value")
    whenever(request.isSecure).thenReturn(true)
    cache.saveRequest(request, response)
    verify(helper).addCookieToResponse(
      same(request),
      same(response),
      check {
        val url = String(Base64Utils.decodeFromString(it))
        assertThat(url).isEqualTo("https://some.com:12345/where?param=value")
      }
    )
  }

  @Test
  fun getRequest() {
    whenever(helper.readValueFromCookie(request)).thenReturn(
      Optional.of(Base64Utils.encodeToString("https://some.com/where?param=value".toByteArray()))
    )
    val savedRequest = cache.getRequest(request, response)
    assertThat(savedRequest?.redirectUrl).isEqualTo("https://some.com/where?param=value")
  }

  @Test
  fun matchingRequest_matches() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.com:443/where"))
    whenever(request.queryString).thenReturn("param=value")
    whenever(request.isSecure).thenReturn(true)
    whenever(helper.readValueFromCookie(request)).thenReturn(
      Optional.of(Base64Utils.encodeToString("https://some.com/where?param=value".toByteArray()))
    )
    val savedRequest = cache.getMatchingRequest(request, response)
    assertThat(savedRequest).isSameAs(request)
    verify(helper).removeCookie(request, response)
  }

  @Test
  fun matchingRequest_nomatch() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.com:443/where"))
    whenever(request.queryString).thenReturn("param=othervalue")
    whenever(request.isSecure).thenReturn(true)
    whenever(helper.readValueFromCookie(request)).thenReturn(
      Optional.of(Base64Utils.encodeToString("https://some.com/where?param=value".toByteArray()))
    )
    val savedRequest = cache.getMatchingRequest(request, response)
    assertThat(savedRequest).isNull()
    verify(helper, never()).removeCookie(request, response)
  }

  @Test
  fun matchingRequest_noSavedRequest() {
    whenever(helper.readValueFromCookie(request)).thenReturn(Optional.empty())
    val savedRequest = cache.getMatchingRequest(request, response)
    assertThat(savedRequest).isNull()
  }
}
