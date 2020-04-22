package uk.gov.justice.digital.hmpps.oauth2server.config;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl;

import java.util.Map;
import java.util.Optional;

@SuppressWarnings("deprecation")
@Slf4j
@AllArgsConstructor
public class LoggingTokenServices extends DefaultTokenServices {
    private final TelemetryClient telemetryClient;
    private final RestTemplate restTemplate;
    private final boolean tokenVerificationEnabled;

    @Override
    public OAuth2AccessToken createAccessToken(final OAuth2Authentication authentication) throws AuthenticationException {
        final var token = super.createAccessToken(authentication);

        final var username = retrieveUsernameFromToken(token);
        final var clientId = authentication.getOAuth2Request().getClientId();

        // not interested in tracking events for client credentials tokens, only proper user access tokens
        if (!authentication.isClientOnly()) {
            telemetryClient.trackEvent("CreateAccessToken", Map.of("username", username, "clientId", clientId), null);
        }
        if (!"token-verification-auth-api-client".equals(clientId)) {
            final var jwtId = sendAuthJwtIdToTokenVerification(authentication, token);
            log.info("Created access token for {} and client {} with jwt id of {}", username, clientId, jwtId);
        }

        return token;
    }

    @Override
    public OAuth2AccessToken refreshAccessToken(final String refreshTokenValue, final TokenRequest tokenRequest) throws AuthenticationException {
        final var token = super.refreshAccessToken(refreshTokenValue, tokenRequest);
        final var username = retrieveUsernameFromToken(token);
        final var clientId = tokenRequest.getClientId();

        if (!"token-verification-auth-api-client".equals(clientId)) {
            final var jwtId = sendRefreshToTokenVerification(token);
            log.info("Created refresh token for {} and client {} with jwt id of {}", username, clientId, jwtId);
        }

        telemetryClient.trackEvent("RefreshAccessToken", Map.of("username", username, "clientId", clientId), null);
        return token;
    }

    private String sendRefreshToTokenVerification(final OAuth2AccessToken token) {
        final var jwtId = (String) token.getAdditionalInformation().get("jti");
        if (tokenVerificationEnabled) {
            // now send token to token verification service so can validate them
            restTemplate.postForLocation("/token/refresh/{authJwtId}", token.getValue(), jwtId);
        }
        return jwtId;
    }

    @Nullable
    private String sendAuthJwtIdToTokenVerification(final OAuth2Authentication authentication, final OAuth2AccessToken token) {
        final String jwtId;
        if (authentication.getPrincipal() instanceof UserDetailsImpl) {
            final var udi = (UserDetailsImpl) authentication.getPrincipal();
            jwtId = udi.getJwtId();
        } else {
            // if we're using a password grant then there won't be any authentication, so just use the jti
            jwtId = (String) token.getAdditionalInformation().get("jti");
        }
        if (jwtId != null && tokenVerificationEnabled) {
            // now send token to token verification service so can validate them
            restTemplate.postForLocation("/token/{authJwtId}", token.getValue(), jwtId);
        }
        return jwtId;
    }

    private String retrieveUsernameFromToken(final OAuth2AccessToken token) {
        final var username = token.getAdditionalInformation().get(JWTTokenEnhancer.ADD_INFO_USER_NAME);
        return Optional.ofNullable(username).map(Object::toString).orElse("none");
    }
}
