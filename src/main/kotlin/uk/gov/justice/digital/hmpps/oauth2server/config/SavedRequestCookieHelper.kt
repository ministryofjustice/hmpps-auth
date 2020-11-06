package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.oauth2server.utils.CookieHelper
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class SavedRequestCookieHelper(properties: SavedRequestCookieConfigurationProperties) :
  CookieHelper(properties.name, properties.expiryTime) {

  fun removeCookie(request: HttpServletRequest, response: HttpServletResponse) {
    val removeSavedRequestCookie = Cookie(name, "")
    removeSavedRequestCookie.path = request.contextPath + "/"
    removeSavedRequestCookie.maxAge = 0
    removeSavedRequestCookie.secure = request.isSecure
    removeSavedRequestCookie.isHttpOnly = true
    response.addCookie(removeSavedRequestCookie)
  }
}
