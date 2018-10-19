package uk.gov.justice.digital.hmpps.oauth2server.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Implementation of request cache that stores the saved request in a cookie rather than the Http session.
 * Based off https://github.com/AusDTO/spring-security-stateless/blob/master/src/main/java/au/gov/dto/springframework/security/web/savedrequest/CookieRequestCache.java
 */
@Component
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class CookieRequestCache implements RequestCache {
    private final SavedRequestCookieHelper savedRequestCookieHelper;

    @Override
    public void saveRequest(final HttpServletRequest request, final HttpServletResponse response) {
        final String redirectUrl = buildUrlFromRequest(request);
        final String redirectUrlBase64 = Base64Utils.encodeToString(redirectUrl.getBytes());
        savedRequestCookieHelper.addCookieToResponse(request, response, redirectUrlBase64);
    }

    private String buildUrlFromRequest(final HttpServletRequest request) {
        final String requestUrl = request.getRequestURL().toString();
        final URI requestUri;
        try {
            requestUri = new URI(requestUrl);
        } catch (final URISyntaxException e) {
            log.error("Problem creating URI from request.getRequestURL() = [{}]", requestUrl, e);
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
        final Optional<String> value = savedRequestCookieHelper.readValueFromCookie(request);
        return value.map(v -> new SimpleSavedRequest(new String(Base64Utils.decodeFromString(v)))).orElse(null);
    }

    @Override
    public HttpServletRequest getMatchingRequest(final HttpServletRequest request, final HttpServletResponse response) {
        final SimpleSavedRequest saved = (SimpleSavedRequest) getRequest(request, response);

        if (saved == null) {
            return null;
        }

        final String requestUrl = buildUrlFromRequest(request);
        if (!requestUrl.equals(saved.getRedirectUrl())) {
            return null;
        }

        removeRequest(request, response);

        return request;
    }

    @Override
    public void removeRequest(final HttpServletRequest request, final HttpServletResponse response) {
        savedRequestCookieHelper.removeCookie(request, response);
    }
}
