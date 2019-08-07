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
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserRetries;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRetriesRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeleteDisabledUsersServiceTest {
    @Mock
    private UserEmailRepository userEmailRepository;
    @Mock
    private UserTokenRepository userTokenRepository;
    @Mock
    private UserRetriesRepository userRetriesRepository;
    @Mock
    private TelemetryClient telemetryClient;
    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    private DeleteDisabledUsersService service;

    @Before
    public void setUp() {
        service = new DeleteDisabledUsersService(userEmailRepository, userRetriesRepository, userTokenRepository, telemetryClient);
    }

    @Test
    public void findAndDeleteDisabledUsers_Processed() {
        final var users = List.of(UserEmail.of("user"), UserEmail.of("joe"));
        when(userEmailRepository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(any()))
                .thenReturn(users);
        assertThat(service.processInBatches()).isEqualTo(2);
    }

    @Test
    public void findAndDeleteDisabledUsers_Deleted() {
        final var user = UserEmail.of("user");
        final var joe = UserEmail.of("joe");
        final var users = List.of(user, joe);

        when(userEmailRepository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(any()))
                .thenReturn(users);
        service.processInBatches();
        verify(userEmailRepository).delete(user);
        verify(userEmailRepository).delete(joe);
    }

    @Test
    public void findAndDeleteDisabledUsers_DeleteAll() {
        final var user = UserEmail.of("user");
        final var token = new UserToken(TokenType.RESET, user);
        final var retry = new UserRetries("user", 3);
        when(userRetriesRepository.findById(anyString())).thenReturn(Optional.of(retry));
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(token));

        when(userEmailRepository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(any()))
                .thenReturn(List.of(user));
        service.processInBatches();
        verify(userRetriesRepository).delete(retry);
        verify(userTokenRepository).delete(token);
    }

    @Test
    public void findAndDeleteDisabledUsers_Telemetry() {
        final var users = List.of(UserEmail.of("user"), UserEmail.of("joe"));
        when(userEmailRepository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(any()))
                .thenReturn(users);
        service.processInBatches();

        verify(telemetryClient, times(2)).trackEvent(eq("DeleteDisabledUsersProcessed"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getAllValues().stream().map(m -> m.get("username")).collect(Collectors.toList())).containsExactly("user", "joe");
    }
}
