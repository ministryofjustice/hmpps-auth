package uk.gov.justice.digital.hmpps.oauth2server.timed;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class DisableInactiveAuthUsersServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private TelemetryClient telemetryClient;
    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    private DisableInactiveAuthUsersService service;

    @BeforeEach
    void setUp() {
        service = new DisableInactiveAuthUsersService(userRepository, telemetryClient, 10);
    }

    @Test
    void findAndDisableInactiveAuthUsers_Processed() {
        final var users = List.of(User.of("user"), User.of("joe"));
        when(userRepository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(any()))
                .thenReturn(users);
        assertThat(service.processInBatches()).isEqualTo(2);
    }

    @Test
    void findAndDisableInactiveAuthUsers_CheckAge() {
        final var users = List.of(User.of("user"), User.of("joe"));
        when(userRepository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(any()))
                .thenReturn(users);
        assertThat(service.processInBatches()).isEqualTo(2);
        final var captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userRepository).findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(captor.capture());
        assertThat(captor.getValue()).isBetween(LocalDateTime.now().minusDays(11), LocalDateTime.now().minusDays(9));
    }

    @Test
    void findAndDisableInactiveAuthUsers_Disabled() {
        final var users = List.of(
                User.builder().username("user").enabled(true).build(),
                User.builder().username("joe").enabled(true).build());
        when(userRepository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(any()))
                .thenReturn(users);
        service.processInBatches();
        assertThat(users).extracting(User::isEnabled).containsExactly(false, false);
    }

    @Test
    void findAndDisableInactiveAuthUsers_Telemetry() {
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
