package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserRetries;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRetriesRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserRetriesServiceTest {
    @Mock
    private UserRetriesRepository userRetriesRepository;
    @Mock
    private UserEmailRepository userEmailRepository;
    @Mock
    private AlterUserService alterUserService;

    private UserRetriesService service;

    @Before
    public void setUp() {
        service = new UserRetriesService(userRetriesRepository, userEmailRepository, alterUserService);
    }

    @Test
    public void resetRetries() {
        service.resetRetries("bob");
        final var captor = ArgumentCaptor.forClass(UserRetries.class);
        verify(userRetriesRepository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new UserRetries("bob", 0));
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
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.empty());
        service.lockAccount("bob");
        final var captor = ArgumentCaptor.forClass(UserEmail.class);
        verify(userEmailRepository).save(captor.capture());
        assertThat(captor.getValue().isLocked()).isEqualTo(true);
    }

    @Test
    public void lockAccount_lockUserEmailExistingRecord() {
        final var existingUserEmail = new UserEmail("username");
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(existingUserEmail));
        service.lockAccount("bob");
        final var captor = ArgumentCaptor.forClass(UserEmail.class);
        verify(userEmailRepository).save(captor.capture());
        assertThat(captor.getValue().isLocked()).isEqualTo(true);
        assertThat(captor.getValue()).isSameAs(existingUserEmail);
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
