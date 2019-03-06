package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.justice.digital.hmpps.oauth2server.utils.CookieHelper;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class SavedRequestCookieHelper extends CookieHelper {

    @Autowired
    public SavedRequestCookieHelper(final SavedRequestCookieConfigurationProperties properties) {
        super(properties.getName(), properties.getExpiryTime());
    }

    void removeCookie(final HttpServletRequest request, final HttpServletResponse response) {
        final var removeSavedRequestCookie = new Cookie(getName(), "");
        removeSavedRequestCookie.setPath(request.getContextPath() + "/");
        removeSavedRequestCookie.setMaxAge(0);
        removeSavedRequestCookie.setSecure(request.isSecure());
        removeSavedRequestCookie.setHttpOnly(true);
        response.addCookie(removeSavedRequestCookie);
    }
}
