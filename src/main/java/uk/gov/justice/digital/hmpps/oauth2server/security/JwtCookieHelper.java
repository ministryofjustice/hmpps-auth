package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class JwtCookieHelper {
    private final String jwtCookieName;
    private final Duration expiryTime;
    private final boolean secure;

    @Autowired
    public JwtCookieHelper(final JwtCookieConfigurationProperties properties) {
        this.jwtCookieName = properties.getName();
        this.expiryTime = properties.getExpiryTime();
        this.secure = properties.isSecure();
    }

    void addCookieToResponse(final HttpServletRequest request, final HttpServletResponse response, final String jwt) {
        // Add a session cookie
        final Cookie sessionCookie = new Cookie(jwtCookieName, jwt);

        // path has to match exactly the path defined in spring's CookieClearingLogoutHandler
        sessionCookie.setPath(request.getContextPath() + "/");
        sessionCookie.setMaxAge(Math.toIntExact(expiryTime.toSeconds()));
        sessionCookie.setHttpOnly(true);

        sessionCookie.setSecure(request.isSecure() || secure);
        response.addCookie(sessionCookie);
    }

    Optional<String> readJwtFromCookie(final HttpServletRequest request) {
        return Stream.of(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(c -> jwtCookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
