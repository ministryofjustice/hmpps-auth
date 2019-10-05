package uk.gov.justice.digital.hmpps.oauth2server.timed;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DisableInactiveAuthUsersServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private TelemetryClient telemetryClient;
    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    private DisableInactiveAuthUsersService service;

    @Before
    public void setUp() {
        service = new DisableInactiveAuthUsersService(userRepository, telemetryClient);
    }

    @Test
    public void findAndDisableInactiveAuthUsers_Processed() {
        final var users = List.of(User.of("user"), User.of("joe"));
        when(userRepository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(any()))
                .thenReturn(users);
        assertThat(service.processInBatches()).isEqualTo(2);
    }

    @Test
    public void findAndDisableInactiveAuthUsers_Disabled() {
        final var users = List.of(
                User.builder().username("user").enabled(true).build(),
                User.builder().username("joe").enabled(true).build());
        when(userRepository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(any()))
                .thenReturn(users);
        service.processInBatches();
        assertThat(users).extracting(User::isEnabled).containsExactly(false, false);
    }

    @Test
    public void findAndDisableInactiveAuthUsers_Telemetry() {
        final var users = List.of(
                User.builder().username("user").enabled(true).build(),
                User.builder().username("joe").enabled(true).build());
        when(userRepository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(any()))
                .thenReturn(users);
        service.processInBatches();

        verify(telemetryClient, times(2)).trackEvent(eq("DisableInactiveAuthUsersProcessed"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getAllValues().stream().map(m -> m.get("username")).collect(Collectors.toList())).containsExactly("user", "joe");
    }
}
