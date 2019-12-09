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
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.service.DelegatingUserService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserRetriesServiceTest {
    @Mock
    private UserRetriesRepository userRetriesRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DelegatingUserService delegatingUserService;

    private UserRetriesService service;

    @Before
    public void setUp() {
        service = new UserRetriesService(userRetriesRepository, userRepository, delegatingUserService);
    }

    @Test
    public void resetRetries() {
        service.resetRetriesAndRecordLogin(getUserPersonDetailsForBob());
        final var captor = ArgumentCaptor.forClass(UserRetries.class);
        verify(userRetriesRepository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new UserRetries("bob", 0));
    }

    @Test
    public void resetRetries_RecordLastLogginIn() {
        final var user = User.builder().username("joe").lastLoggedIn(LocalDateTime.now().minusDays(1)).build();
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        service.resetRetriesAndRecordLogin(getUserPersonDetailsForBob());

        assertThat(user.getLastLoggedIn()).isBetween(LocalDateTime.now().plusMinutes(-1), LocalDateTime.now());
    }

    @Test
    public void resetRetries_SaveDeliusEmailAddress() {
        final var user = User.builder().username("joe").lastLoggedIn(LocalDateTime.now().minusDays(1)).build();
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        service.resetRetriesAndRecordLogin(new DeliusUserPersonDetails("deliusUser", "12345", "Delius", "Smith", "newemail@bob.com", true, false, Set.of()));

        assertThat(user.getEmail()).isEqualTo("newemail@bob.com");
        assertThat(user.isVerified()).isTrue();
    }

    @Test
    public void resetRetries_SaveNewUser() {
        service.resetRetriesAndRecordLogin(getUserPersonDetailsForBob());

        final var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getUsername()).isEqualTo("bob");
        assertThat(captor.getValue().getLastLoggedIn()).isBetween(LocalDateTime.now().plusMinutes(-1), LocalDateTime.now());
    }

    @Test
    public void lockAccount_retriesTo0() {
        service.lockAccount(getUserPersonDetailsForBob());
        final var captor = ArgumentCaptor.forClass(UserRetries.class);
        verify(userRetriesRepository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new UserRetries("bob", 0));
    }

    @Test
    public void lockAccount_alterUser() {
        final var userPersonDetailsForBob = getUserPersonDetailsForBob();
        service.lockAccount(userPersonDetailsForBob);
        verify(delegatingUserService).lockAccount(userPersonDetailsForBob);
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

    private UserPersonDetails getUserPersonDetailsForBob() {
        final var staffUserAccount = new NomisUserPersonDetails();
        final var staff = new Staff();
        staff.setFirstName("bOb");
        staff.setStatus("ACTIVE");
        staffUserAccount.setStaff(staff);
        final var detail = new AccountDetail("user", "OPEN", "profile", null);
        staffUserAccount.setAccountDetail(detail);
        staffUserAccount.setUsername("bob");
        return staffUserAccount;
    }
}
