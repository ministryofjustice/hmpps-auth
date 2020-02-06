package uk.gov.justice.digital.hmpps.oauth2server.config;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.DefaultOAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.digital.hmpps.oauth2server.security.ExternalIdAuthenticationHelper;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class LoggingTokenServicesTest {
    private static final OAuth2Request OAUTH_2_REQUEST = new OAuth2Request(Collections.emptyMap(), "client", Collections.emptySet(), true, Collections.emptySet(), Collections.emptySet(), "redirect", null, null);
    private static final UserDetailsImpl USER_DETAILS = new UserDetailsImpl("authenticateduser", "name", Collections.emptySet(), null, null);
    private static final OAuth2Request OAUTH_2_SCOPE_REQUEST = new OAuth2Request(Collections.emptyMap(), "community-api-client", List.of((GrantedAuthority) () -> "ROLE_COMMUNITY"), true, Set.of("proxy-user"), Collections.emptySet(), "redirect", null, null);
    private static final UserDetailsImpl UNCHECKED_USER_DETAILS = new UserDetailsImpl("notcheckeduser", null, Collections.emptySet(), "none", null);

    @Mock
    private TelemetryClient telemetryClient;

    @Mock
    private TokenStore tokenStore;

    @Mock
    private ExternalIdAuthenticationHelper externalIdAuthenticationHelper;

    private LoggingTokenServices loggingTokenServices;

    @BeforeEach
    void setUp() {
        loggingTokenServices = new LoggingTokenServices(telemetryClient);
        loggingTokenServices.setSupportRefreshToken(true);
        loggingTokenServices.setTokenStore(tokenStore);
        final var tokenEnhancer = new JWTTokenEnhancer();
        ReflectionTestUtils.setField(tokenEnhancer, "externalIdAuthenticationHelper", externalIdAuthenticationHelper);
        loggingTokenServices.setTokenEnhancer(tokenEnhancer);
    }

    @Test
    void createAccessToken() {
        final var userAuthentication = new UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials");
        loggingTokenServices.createAccessToken(new OAuth2Authentication(OAUTH_2_REQUEST, userAuthentication));
        verify(telemetryClient).trackEvent("CreateAccessToken", Map.of("username", "authenticateduser", "clientId", "client"), null);
    }

    @Test
    void createAccessToken_ClientOnly() {
        when(externalIdAuthenticationHelper.getUserDetails(anyMap(), eq(false))).thenReturn(USER_DETAILS);
        loggingTokenServices.createAccessToken(new OAuth2Authentication(OAUTH_2_REQUEST, null));
        verify(telemetryClient, never()).trackEvent(any(), anyMap(), isNull());
    }

    @Test
    void createAccessToken_ClientOnlyProxyUser() {
        when(externalIdAuthenticationHelper.getUserDetails(anyMap(), eq(true))).thenReturn(UNCHECKED_USER_DETAILS);
        loggingTokenServices.createAccessToken(new OAuth2Authentication(OAUTH_2_SCOPE_REQUEST, null));
        verify(telemetryClient, never()).trackEvent(any(), anyMap(), isNull());
    }


    @Test
    void refreshAccessToken() {
        when(tokenStore.readRefreshToken(anyString())).thenReturn(new DefaultOAuth2RefreshToken("newValue"));
        when(tokenStore.readAuthenticationForRefreshToken(any())).thenReturn(new OAuth2Authentication(OAUTH_2_REQUEST, new UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials")));
        loggingTokenServices.refreshAccessToken("tokenValue", new TokenRequest(Collections.emptyMap(), "client", Collections.emptySet(), "refresh"));
        verify(telemetryClient).trackEvent("RefreshAccessToken", Map.of("username", "authenticateduser", "clientId", "client"), null);
    }

}
