package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.springframework.security.web.savedrequest.SavedRequest;

import javax.servlet.http.Cookie;
import java.util.*;

/**
 * Simple implementation of saved request that just uses the redirect url.
 * Taken from https://github.com/AusDTO/spring-security-stateless/blob/master/src/main/java/au/gov/dto/springframework/security/web/savedrequest/SimpleSavedRequest.java
 */
public class SimpleSavedRequest implements SavedRequest {
    private final String redirectUrl;

    SimpleSavedRequest(final String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    @Override
    public String getRedirectUrl() {
        return redirectUrl;
    }

    @Override
    public List<Cookie> getCookies() {
        return Collections.emptyList();
    }

    @Override
    public String getMethod() {
        return "GET";
    }

    @Override
    public List<String> getHeaderValues(final String name) {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return Collections.emptyList();
    }

    @Override
    public List<Locale> getLocales() {
        return Collections.emptyList();
    }

    @Override
    public String[] getParameterValues(final String name) {
        return new String[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return Collections.emptyMap();
    }
}
