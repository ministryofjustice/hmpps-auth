package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Implementation of request cache that stores the saved request in a cookie rather than the Http session.
 * Based off https://github.com/AusDTO/spring-security-stateless/blob/master/src/main/java/au/gov/dto/springframework/security/web/savedrequest/CookieRequestCache.java
 */
@Component
@Slf4j
public class CookieRequestCache implements RequestCache {
    public static final String DEFAULT_SAVEDREQUEST_COOKIE_NAME = "savedrequest";

    private final Base64.Encoder base64Encoder = Base64.getMimeEncoder(Integer.MAX_VALUE, new byte[]{'\n'});
    private final Base64.Decoder base64Decoder = Base64.getMimeDecoder();

    private String savedRequestCookieName = DEFAULT_SAVEDREQUEST_COOKIE_NAME;
    private String savedRequestCookiePath = "/";
    private int savedRequestCookieMaxAgeSeconds = -1;  // default to session cookie (non-persistent)

    @Override
    public void saveRequest(final HttpServletRequest request, final HttpServletResponse response) {
        final String redirectUrl = buildUrlFromRequest(request);
        final String redirectUrlBase64 = base64Encoder.encodeToString(redirectUrl.getBytes(StandardCharsets.ISO_8859_1));
        final Cookie savedRequestCookie = new Cookie(savedRequestCookieName, redirectUrlBase64);
        savedRequestCookie.setPath(savedRequestCookiePath);
        savedRequestCookie.setMaxAge(savedRequestCookieMaxAgeSeconds);
        savedRequestCookie.setSecure(request.isSecure());
        savedRequestCookie.setHttpOnly(true);
        response.addCookie(savedRequestCookie);
    }

    private String buildUrlFromRequest(final HttpServletRequest request) {
        final String requestUrl = request.getRequestURL().toString();
        final URI requestUri;
        try {
            requestUri = new URI(requestUrl);
        } catch (final URISyntaxException e) {
            throw new RuntimeException("Problem creating URI from request.getRequestURL() = [" + requestUrl + "]", e);
        }
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance()
                .scheme(request.isSecure() ? "https" : "http")
                .host(requestUri.getHost())
                .path(requestUri.getPath())
                .query(request.getQueryString());
        if ((request.isSecure() && requestUri.getPort() != 443) || (!request.isSecure() && requestUri.getPort() != 80)) {
            uriComponentsBuilder.port(requestUri.getPort());
        }
        return uriComponentsBuilder.build().toUriString();
    }

    @Override
    public SavedRequest getRequest(final HttpServletRequest request, final HttpServletResponse response) {
        if (request.getCookies() == null) {
            return null;
        }
        final Optional<Cookie> maybeCookie = Stream.of(request.getCookies()).filter(cookie -> cookie != null && savedRequestCookieName.equals(cookie.getName())).findFirst();
        if (!maybeCookie.isPresent()) {
            return null;
        }
        final Cookie savedRequestCookie = maybeCookie.get();
        final String redirectUrl = new String(base64Decoder.decode(savedRequestCookie.getValue()), StandardCharsets.ISO_8859_1);
        return new SimpleSavedRequest(redirectUrl);
    }

    @Override
    public HttpServletRequest getMatchingRequest(final HttpServletRequest request, final HttpServletResponse response) {
        final SimpleSavedRequest saved = (SimpleSavedRequest) getRequest(request, response);

        if (saved == null) {
            log.debug("No saved request found for request {}", buildUrlFromRequest(request));
            return null;
        }

        final String requestUrl = buildUrlFromRequest(request);
        if (!requestUrl.equals(saved.getRedirectUrl())) {
            log.debug("saved request doesn't match: {} vs saved {}", requestUrl, saved.getRedirectUrl());
            return null;
        }
        log.debug("saved request matched {}", requestUrl);

        removeRequest(request, response);

        return request;
    }

    @Override
    public void removeRequest(final HttpServletRequest request, final HttpServletResponse response) {
        log.debug("Removing request for {}", buildUrlFromRequest(request));

        final Cookie removeSavedRequestCookie = new Cookie(savedRequestCookieName, "");
        removeSavedRequestCookie.setPath(savedRequestCookiePath);
        removeSavedRequestCookie.setMaxAge(0);
        removeSavedRequestCookie.setSecure(request.isSecure());
        removeSavedRequestCookie.setHttpOnly(true);
        response.addCookie(removeSavedRequestCookie);
    }
}
