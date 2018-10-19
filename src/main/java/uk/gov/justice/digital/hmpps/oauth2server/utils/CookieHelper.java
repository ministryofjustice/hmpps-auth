package uk.gov.justice.digital.hmpps.oauth2server.utils;

import lombok.AccessLevel;
import lombok.Getter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

@Getter(AccessLevel.PROTECTED)
public class CookieHelper {
    private final String name;
    private final Duration expiryTime;
    private final boolean secure;

    public CookieHelper(final String name, final Duration expiryTime, final boolean secure) {
        this.name = name;
        this.expiryTime = expiryTime;
        this.secure = secure;
    }

    public void addCookieToResponse(final HttpServletRequest request, final HttpServletResponse response, final String value) {
        // Add a session cookie
        final Cookie sessionCookie = new Cookie(name, value);

        // path has to match exactly the path defined in spring's CookieClearingLogoutHandler
        sessionCookie.setPath(request.getContextPath() + "/");
        sessionCookie.setMaxAge(Math.toIntExact(expiryTime.toSeconds()));
        sessionCookie.setHttpOnly(true);
        sessionCookie.setSecure(request.isSecure() || secure);
        response.addCookie(sessionCookie);
    }

    public Optional<String> readValueFromCookie(final HttpServletRequest request) {
        return Stream.of(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
