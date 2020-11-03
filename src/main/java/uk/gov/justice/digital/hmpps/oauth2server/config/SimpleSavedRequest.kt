package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.security.web.savedrequest.SavedRequest
import java.util.Locale
import javax.servlet.http.Cookie

/**
 * Simple implementation of saved request that just uses the redirect url.
 * Taken from https://github.com/AusDTO/spring-security-stateless/blob/master/src/main/java/au/gov/dto/springframework/security/web/savedrequest/SimpleSavedRequest.java
 */
class SimpleSavedRequest internal constructor(private val redirectUrl: String) : SavedRequest {
  override fun getRedirectUrl(): String {
    return redirectUrl
  }

  override fun getCookies(): List<Cookie> {
    return emptyList()
  }

  override fun getMethod(): String {
    return "GET"
  }

  override fun getHeaderValues(name: String): List<String> {
    return emptyList()
  }

  override fun getHeaderNames(): Collection<String> {
    return emptyList()
  }

  override fun getLocales(): List<Locale> {
    return emptyList()
  }

  override fun getParameterValues(name: String): Array<String> {
    return emptyArray()
  }

  override fun getParameterMap(): Map<String, Array<String>> {
    return emptyMap()
  }
}
