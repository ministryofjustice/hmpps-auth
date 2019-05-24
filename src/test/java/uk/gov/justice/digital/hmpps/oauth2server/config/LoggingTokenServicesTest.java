package uk.gov.justice.digital.hmpps.oauth2server.config;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.common.DefaultOAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.digital.hmpps.oauth2server.security.ExternalIdAuthenticationHelper;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LoggingTokenServicesTest {
    private static final OAuth2Request OAUTH_2_REQUEST = new OAuth2Request(Collections.emptyMap(), "client", Collections.emptySet(), true, Collections.emptySet(), Collections.emptySet(), "redirect", null, null);
    private static final UserDetailsImpl USER_DETAILS = new UserDetailsImpl("authenticateduser", "name", Collections.emptySet(), null);

    @Mock
    private TelemetryClient telemetryClient;

    @Mock
    private TokenStore tokenStore;

    @Mock
    private ExternalIdAuthenticationHelper externalIdAuthenticationHelper;

    private LoggingTokenServices loggingTokenServices;

    @Before
    public void setUp() {
        loggingTokenServices = new LoggingTokenServices(telemetryClient);
        loggingTokenServices.setSupportRefreshToken(true);
        loggingTokenServices.setTokenStore(tokenStore);
        final var tokenEnhancer = new JWTTokenEnhancer();
        ReflectionTestUtils.setField(tokenEnhancer, "externalIdAuthenticationHelper", externalIdAuthenticationHelper);
        loggingTokenServices.setTokenEnhancer(tokenEnhancer);
    }

    @Test
    public void createAccessToken() {
        final var userAuthentication = new UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials");
        loggingTokenServices.createAccessToken(new OAuth2Authentication(OAUTH_2_REQUEST, userAuthentication));
        verify(telemetryClient).trackEvent("CreateAccessToken", Map.of("username", "authenticateduser", "clientId", "client"), null);
    }

    @Test
    public void createAccessToken_ClientOnly() {
        when(externalIdAuthenticationHelper.getUserDetails(anyMap())).thenReturn(USER_DETAILS);
        loggingTokenServices.createAccessToken(new OAuth2Authentication(OAUTH_2_REQUEST, null));
        verify(telemetryClient, never()).trackEvent(any(), anyMap(), isNull());
    }

    @Test
    public void refreshAccessToken() {
        when(tokenStore.readRefreshToken(anyString())).thenReturn(new DefaultOAuth2RefreshToken("newValue"));
        when(tokenStore.readAuthenticationForRefreshToken(any())).thenReturn(new OAuth2Authentication(OAUTH_2_REQUEST, new UsernamePasswordAuthenticationToken(USER_DETAILS, "credentials")));
        loggingTokenServices.refreshAccessToken("tokenValue", new TokenRequest(Collections.emptyMap(), "client", Collections.emptySet(), "refresh"));
        verify(telemetryClient).trackEvent("RefreshAccessToken", Map.of("username", "authenticateduser", "clientId", "client"), null);
    }

}
