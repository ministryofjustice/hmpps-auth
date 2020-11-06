package uk.gov.justice.digital.hmpps.oauth2server.config

import org.slf4j.LoggerFactory
import org.springframework.security.web.savedrequest.RequestCache
import org.springframework.security.web.savedrequest.SavedRequest
import org.springframework.stereotype.Component
import org.springframework.util.Base64Utils
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.net.URISyntaxException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Implementation of request cache that stores the saved request in a cookie rather than the Http session.
 * Based off https://github.com/AusDTO/spring-security-stateless/blob/master/src/main/java/au/gov/dto/springframework/security/web/savedrequest/CookieRequestCache.java
 */
@Component
class CookieRequestCache(private val savedRequestCookieHelper: SavedRequestCookieHelper) : RequestCache {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun saveRequest(request: HttpServletRequest, response: HttpServletResponse) {
    val redirectUrl = buildUrlFromRequest(request)
    val redirectUrlBase64 = Base64Utils.encodeToString(redirectUrl.toByteArray())
    savedRequestCookieHelper.addCookieToResponse(request, response, redirectUrlBase64)
  }

  private fun buildUrlFromRequest(request: HttpServletRequest): String {
    val requestUrl = request.requestURL.toString()
    val requestUri: URI
    requestUri = try {
      URI(requestUrl)
    } catch (e: URISyntaxException) {
      log.error("Problem creating URI from request.getRequestURL() = [{}]", requestUrl, e)
      throw RuntimeException("Problem creating URI from request.getRequestURL() = [$requestUrl]", e)
    }
    val uriComponentsBuilder = UriComponentsBuilder.newInstance()
      .scheme(if (request.isSecure) "https" else "http")
      .host(requestUri.host)
      .path(requestUri.path)
      .query(request.queryString)
    if (request.isSecure && requestUri.port != 443 || !request.isSecure && requestUri.port != 80) {
      uriComponentsBuilder.port(requestUri.port)
    }
    return uriComponentsBuilder.build().toUriString()
  }

  override fun getRequest(request: HttpServletRequest, response: HttpServletResponse): SavedRequest? {
    val value = savedRequestCookieHelper.readValueFromCookie(request)
    return value.map { SimpleSavedRequest(String(Base64Utils.decodeFromString(it))) }
      .orElse(null)
  }

  override fun getMatchingRequest(request: HttpServletRequest, response: HttpServletResponse): HttpServletRequest? {
    val savedRequest = getRequest(request, response) ?: return null

    val requestUrl = buildUrlFromRequest(request)
    if (requestUrl != savedRequest.redirectUrl) {
      return null
    }
    removeRequest(request, response)
    return request
  }

  override fun removeRequest(request: HttpServletRequest, response: HttpServletResponse) {
    savedRequestCookieHelper.removeCookie(request, response)
  }
}
