package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffIdentifierRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffUserAccountRepository;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {
    @Mock
    private UserEmailRepository userEmailRepository;
    @Mock
    private StaffUserAccountRepository staffUserAccountRepository;
    @Mock
    private StaffIdentifierRepository staffIdentifierRepository;
    @Mock
    private TelemetryClient telemetryClient;

    private UserService userService;

    @Before
    public void setUp() {
        userService = new UserService(staffUserAccountRepository, staffIdentifierRepository, userEmailRepository, telemetryClient);
    }

    @Test
    public void findUser_AuthUser() {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUserEmailUser());

        final var user = userService.findUser("   bob   ");

        assertThat(user).isPresent().get().extracting(UserPersonDetails::getUsername).isEqualTo("someuser");

        verify(userEmailRepository).findByUsernameAndMasterIsTrue("BOB");
    }

    @Test
    public void findUser_NomisUser() {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.empty());
        when(staffUserAccountRepository.findById(anyString())).thenReturn(getStaffUserAccountForBob());

        final var user = userService.findUser("bob");

        assertThat(user).isPresent().get().extracting(UserPersonDetails::getUsername).isEqualTo("nomisuser");

        verify(userEmailRepository).findByUsernameAndMasterIsTrue("BOB");
        verify(staffUserAccountRepository).findById("BOB");
    }

    @Test
    public void findByEmailAndMasterIsTrue() {
        when(userEmailRepository.findByEmailAndMasterIsTrueOrderByUsername(anyString())).thenReturn(List.of(new UserEmail("someuser")));

        final var user = userService.findAuthUsersByEmail("  bob  ");

        assertThat(user).extracting(UserPersonDetails::getUsername).containsOnly("someuser");
    }

    @Test
    public void enableUser() {
        final var optionalUserEmail = createUserEmailUser();
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUserEmail);
        userService.enableUser("user", "admin");
        assertThat(optionalUserEmail).get().extracting(UserEmail::isEnabled).isEqualTo(Boolean.TRUE);
        //noinspection OptionalGetWithoutIsPresent
        verify(userEmailRepository).save(optionalUserEmail.get());
    }

    @Test
    public void enableUser_notFound() {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.enableUser("user", "admin")).isInstanceOf(EntityNotFoundException.class).hasMessageContaining("username user");
    }

    @Test
    public void enableUser_trackEvent() {
        final var optionalUserEmail = createUserEmailUser();
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUserEmail);
        userService.enableUser("someuser", "someadmin");
        verify(telemetryClient).trackEvent("AuthUserChangeStatus", Map.of("username", "someuser", "admin", "someadmin", "status", "true"), null);
    }

    @Test
    public void disableUser() {
        final var optionalUserEmail = createUserEmailUser();
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUserEmail);
        userService.disableUser("user", "admin");
        assertThat(optionalUserEmail).get().extracting(UserEmail::isEnabled).isEqualTo(Boolean.FALSE);
        //noinspection OptionalGetWithoutIsPresent
        verify(userEmailRepository).save(optionalUserEmail.get());
    }

    @Test
    public void disableUser_trackEvent() {
        final var optionalUserEmail = createUserEmailUser();
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUserEmail);
        userService.disableUser("someuser", "someadmin");
        verify(telemetryClient).trackEvent("AuthUserChangeStatus", Map.of("username", "someuser", "admin", "someadmin", "status", "false"), null);
    }

    @Test
    public void disableUser_notFound() {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.disableUser("user", "admin")).isInstanceOf(EntityNotFoundException.class).hasMessageContaining("username user");
    }

    private Optional<UserEmail> createUserEmailUser() {
        return Optional.of(new UserEmail("someuser"));
    }

    private Optional<StaffUserAccount> getStaffUserAccountForBob() {
        final var staffUserAccount = new StaffUserAccount();
        staffUserAccount.setUsername("nomisuser");
        final var staff = new Staff();
        staff.setFirstName("bOb");
        staff.setStatus("ACTIVE");
        staffUserAccount.setStaff(staff);
        final var detail = new AccountDetail("user", "OPEN", "profile", null);
        staffUserAccount.setAccountDetail(detail);
        return Optional.of(staffUserAccount);
    }
}
