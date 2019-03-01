package uk.gov.justice.digital.hmpps.oauth2server.config;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class LoggingTokenServices extends DefaultTokenServices {
    private final TelemetryClient telemetryClient;

    LoggingTokenServices(final TelemetryClient telemetryClient) {
        this.telemetryClient = telemetryClient;
    }

    @Override
    public OAuth2AccessToken createAccessToken(final OAuth2Authentication authentication) throws AuthenticationException {
        final var token = super.createAccessToken(authentication);

        final var username = authentication.getPrincipal().toString();
        log.info("Created access token for {}", username);

        // not interested in tracking events for client credentials tokens, only proper user access tokens
        if (!authentication.isClientOnly()) {
            telemetryClient.trackEvent("CreateAccessToken", Map.of("username", username), null);
        }
        return token;
    }

    @Override
    public OAuth2AccessToken refreshAccessToken(final String refreshTokenValue, final TokenRequest tokenRequest) throws AuthenticationException {
        final var token = super.refreshAccessToken(refreshTokenValue, tokenRequest);
        final var username = retrieveUsernameFromToken(token);
        log.info("Created refresh token for {}", username);
        telemetryClient.trackEvent("RefreshAccessToken", Map.of("username", username), null);
        return token;
    }

    private String retrieveUsernameFromToken(final OAuth2AccessToken token) {
        final var username = token.getAdditionalInformation().get(JWTTokenEnhancer.ADD_INFO_USER_NAME);
        return Optional.ofNullable(username).map(Object::toString).orElse("none");
    }
}
