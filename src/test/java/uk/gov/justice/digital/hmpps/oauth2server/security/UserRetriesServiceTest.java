package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserRetries;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRetriesRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UserRetriesServiceTest {
    @Mock
    private UserRetriesRepository userRetriesRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AlterUserService alterUserService;

    private UserRetriesService service;

    @Before
    public void setUp() {
        service = new UserRetriesService(userRetriesRepository, userRepository, alterUserService);
    }

    @Test
    public void resetRetries() {
        service.resetRetriesAndRecordLogin("bob");
        final var captor = ArgumentCaptor.forClass(UserRetries.class);
        verify(userRetriesRepository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new UserRetries("bob", 0));
    }

    @Test
    public void resetRetries_RecordLastLogginIn() {
        final var user = User.builder().username("joe").lastLoggedIn(LocalDateTime.now().minusDays(1)).build();
        when(userRepository.findById(anyString())).thenReturn(Optional.of(user));
        service.resetRetriesAndRecordLogin("bob");

        assertThat(user.getLastLoggedIn()).isBetween(LocalDateTime.now().plusMinutes(-1), LocalDateTime.now());
    }

    @Test
    public void resetRetries_SaveNewUser() {
        service.resetRetriesAndRecordLogin("bob");

        final var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getUsername()).isEqualTo("bob");
        assertThat(captor.getValue().getLastLoggedIn()).isBetween(LocalDateTime.now().plusMinutes(-1), LocalDateTime.now());
    }

    @Test
    public void lockAccount_retriesTo0() {
        service.lockAccount("bob");
        final var captor = ArgumentCaptor.forClass(UserRetries.class);
        verify(userRetriesRepository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new UserRetries("bob", 0));
    }

    @Test
    public void lockAccount_lockUserEmailNoRecord() {
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());
        service.lockAccount("bob");
        final var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isLocked()).isEqualTo(true);
    }

    @Test
    public void lockAccount_lockUserEmailExistingRecord() {
        final var existingUserEmail = User.of("username");
        when(userRepository.findById(anyString())).thenReturn(Optional.of(existingUserEmail));
        service.lockAccount("bob");
        final var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isLocked()).isEqualTo(true);
        assertThat(captor.getValue()).isSameAs(existingUserEmail);
    }

    @Test
    public void lockAccount_NoAlterUserForAuthOnlyAccounts() {
        final var existingUserEmail = User.of("username");
        existingUserEmail.setMaster(true);
        when(userRepository.findById(anyString())).thenReturn(Optional.of(existingUserEmail));
        service.lockAccount("bob");
        verify(alterUserService, never()).lockAccount(anyString());
    }

    @Test
    public void lockAccount_alterUser() {
        service.lockAccount("bob");
        verify(alterUserService).lockAccount("bob");
    }

    @Test
    public void incrementRetries_NoExistingRow() {
        when(userRetriesRepository.findById(anyString())).thenReturn(Optional.empty());
        assertThat(service.incrementRetries("bob")).isEqualTo(1);
        final var captor = ArgumentCaptor.forClass(UserRetries.class);
        verify(userRetriesRepository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new UserRetries("bob", 11));
    }

    @Test
    public void incrementRetries_ExistingRow() {
        when(userRetriesRepository.findById(anyString())).thenReturn(Optional.of(new UserRetries("bob", 5)));
        assertThat(service.incrementRetries("bob")).isEqualTo(6);
        final var captor = ArgumentCaptor.forClass(UserRetries.class);
        verify(userRetriesRepository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new UserRetries("bob", 6));
    }
}
