package uk.gov.justice.digital.hmpps.oauth2server.config;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.web.extensibility.modules.WebTelemetryModule;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import uk.gov.justice.digital.hmpps.oauth2server.utils.IpAddressHelper;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.Map;
import java.util.Optional;

@Configuration
public class ClientTrackingTelemetryModule implements WebTelemetryModule, TelemetryModule {
    @Override
    public void onBeginRequest(final ServletRequest req, final ServletResponse res) {

        final var telemetryProperties = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getProperties();
        final var httpServletRequest = (HttpServletRequest) req;

        addClientIdAndUser(telemetryProperties, httpServletRequest);
        addClientIpAddress(telemetryProperties, httpServletRequest);
    }

    private void addClientIpAddress(final Map<String, String> properties, final HttpServletRequest req) {
        properties.put("clientIpAddress", IpAddressHelper.retrieveIpFromRemoteAddr(req));
    }

    private void addClientIdAndUser(final Map<String, String> properties, final HttpServletRequest httpServletRequest) {
        final var token = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        final var bearer = "Bearer ";
        if (StringUtils.startsWithIgnoreCase(token, bearer)) {

            try {
                final var jwtBody = getClaimsFromJWT(StringUtils.substringAfter(token, bearer));

                final var user = Optional.ofNullable(jwtBody.getClaim("user_name"));
                user.map(String::valueOf).ifPresent(u -> properties.put("username", u));

                properties.put("clientId", String.valueOf(jwtBody.getClaim("client_id")));

            } catch (final ParseException e) {
                // Expired token which spring security will handle
            }
        }
    }

    private JWTClaimsSet getClaimsFromJWT(final String token) throws ParseException {
        return SignedJWT.parse(token).getJWTClaimsSet();
    }

    @Override
    public void onEndRequest(final ServletRequest req, final ServletResponse res) {
    }

    @Override
    public void initialize(final TelemetryConfiguration configuration) {

    }
}
