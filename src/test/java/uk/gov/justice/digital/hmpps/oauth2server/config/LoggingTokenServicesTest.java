package uk.gov.justice.digital.hmpps.oauth2server.config;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.TokenStore;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LoggingTokenServicesTest {
    private static final OAuth2Request OAUTH_2_REQUEST = new OAuth2Request(Collections.emptyMap(), "client", Collections.emptySet(), true, Collections.emptySet(), Collections.emptySet(), "redirect", null, null);

    @Mock
    private TelemetryClient telemetryClient;

    @Mock
    private TokenStore tokenStore;

    private LoggingTokenServices loggingTokenServices;

    @Before
    public void setUp() {
        loggingTokenServices = new LoggingTokenServices(telemetryClient);
        loggingTokenServices.setTokenStore(tokenStore);
    }

    @Test
    public void createAccessToken() {
        final var userAuthentication = new UsernamePasswordAuthenticationToken("principal", "credentials");
        loggingTokenServices.createAccessToken(new OAuth2Authentication(OAUTH_2_REQUEST, userAuthentication));
        verify(telemetryClient).trackEvent("CreateAccessToken", Map.of("username", "principal"), null);
    }

    @Test
    public void createAccessToken_ClientOnly() {
        loggingTokenServices.createAccessToken(new OAuth2Authentication(OAUTH_2_REQUEST, null));
        verify(telemetryClient, never()).trackEvent(any(), anyMap(), isNull());
    }

    @Test
    public void refreshAccessToken() {
    }
}
