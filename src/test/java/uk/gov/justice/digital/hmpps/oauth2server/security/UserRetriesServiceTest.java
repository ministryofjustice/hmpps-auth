package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserRetries;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRetriesRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserRetriesServiceTest {
    @Mock
    private UserRetriesRepository repository;

    private UserRetriesService service;

    @Before
    public void setUp() {
        service = new UserRetriesService(repository);
    }

    @Test
    public void resetRetries() {
        service.resetRetries("bob");
        final var captor = ArgumentCaptor.forClass(UserRetries.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new UserRetries("bob", 0));
    }

    @Test
    public void incrementRetries_NoExistingRow() {
        when(repository.findById(anyString())).thenReturn(Optional.empty());
        assertThat(service.incrementRetries("bob", 10)).isEqualTo(11);
        final var captor = ArgumentCaptor.forClass(UserRetries.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new UserRetries("bob", 11));
    }

    @Test
    public void incrementRetries_ExistingRow() {
        when(repository.findById(anyString())).thenReturn(Optional.of(new UserRetries("bob", 5)));
        assertThat(service.incrementRetries("bob", 10)).isEqualTo(6);
        final var captor = ArgumentCaptor.forClass(UserRetries.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new UserRetries("bob", 6));
    }
}
