package uk.gov.justice.digital.hmpps.oauth2server.timed;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DisableInactiveAuthUsersServiceTest {
    @Mock
    private UserEmailRepository userEmailRepository;
    @Mock
    private TelemetryClient telemetryClient;
    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    private DisableInactiveAuthUsersService service;

    @Before
    public void setUp() {
        service = new DisableInactiveAuthUsersService(userEmailRepository, telemetryClient);
    }

    @Test
    public void findAndDisableInactiveAuthUsers_Processed() {
        final var users = List.of(UserEmail.of("user"), UserEmail.of("joe"));
        when(userEmailRepository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(any()))
                .thenReturn(users);
        assertThat(service.processInBatches()).isEqualTo(2);
    }

    @Test
    public void findAndDisableInactiveAuthUsers_Disabled() {
        final var users = List.of(
                UserEmail.builder().username("user").enabled(true).build(),
                UserEmail.builder().username("joe").enabled(true).build());
        when(userEmailRepository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(any()))
                .thenReturn(users);
        service.processInBatches();
        assertThat(users).extracting(UserEmail::isEnabled).containsExactly(false, false);
    }

    @Test
    public void findAndDisableInactiveAuthUsers_Telemetry() {
        final var users = List.of(
                UserEmail.builder().username("user").enabled(true).build(),
                UserEmail.builder().username("joe").enabled(true).build());
        when(userEmailRepository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(any()))
                .thenReturn(users);
        service.processInBatches();

        verify(telemetryClient, times(2)).trackEvent(eq("DisableInactiveAuthUsersProcessed"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getAllValues().stream().map(m -> m.get("username")).collect(Collectors.toList())).containsExactly("user", "joe");
    }
}
