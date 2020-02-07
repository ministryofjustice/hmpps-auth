package uk.gov.justice.digital.hmpps.oauth2server.config;

import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.justice.digital.hmpps.oauth2server.utils.JwtAuthHelper;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static uk.gov.justice.digital.hmpps.oauth2server.utils.JwtAuthHelper.JwtParameters;

@ExtendWith(SpringExtension.class)
@Import({JwtAuthHelper.class, ClientTrackingTelemetryModule.class})
@ContextConfiguration(initializers = {ConfigFileApplicationContextInitializer.class})
@ActiveProfiles("test")
public class ClientTrackingTelemetryModuleTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private ClientTrackingTelemetryModule clientTrackingTelemetryModule;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private JwtAuthHelper jwtAuthHelper;

    private final MockHttpServletResponse res = new MockHttpServletResponse();
    private final MockHttpServletRequest req = new MockHttpServletRequest();

    @BeforeEach
    void setup() {
        ThreadContext.setRequestTelemetryContext(new RequestTelemetryContext(1L));
    }

    @AfterEach
    void tearDown() {
        ThreadContext.remove();
    }

    @Test
    void shouldAddClientIdAndUserNameToInsightTelemetry() {

        final var token = createJwt("bob", 1L);

        req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        clientTrackingTelemetryModule.onBeginRequest(req, res);

        final var insightTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getProperties();

        assertThat(insightTelemetry).contains(entry("username", "bob"), entry("clientId", "elite2apiclient"));
    }

    @Test
    void shouldNotAddClientIdAndUserNameToInsightTelemetryAsTokenExpired() {

        final var token = createJwt("Fred", -1L);

        req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        clientTrackingTelemetryModule.onBeginRequest(req, res);

        final var insightTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getProperties();

        assertThat(insightTelemetry).doesNotContain(entry("username", "Fred"), entry("clientId", "elite2apiclient"));
    }

    private String createJwt(final String user, final Long duration) {
        return jwtAuthHelper.createJwt(JwtParameters.builder()
                .username(user)
                .roles(List.of())
                .scope(List.of("read", "write"))
                .expiryTime(Duration.ofDays(duration))
                .build());
    }

    @Test
    void shouldAddClientIpToInsightTelemetry() {
        final var SOME_IP_ADDRESS = "12.13.14.15";
        req.setRemoteAddr(SOME_IP_ADDRESS);

        clientTrackingTelemetryModule.onBeginRequest(req, res);

        final var insightTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getProperties();
        assertThat(insightTelemetry).contains(entry("clientIpAddress", SOME_IP_ADDRESS));
    }

}
