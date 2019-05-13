package uk.gov.justice.digital.hmpps.oauth2server.maintain;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService.AuthUserRoleException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthUserRolesServiceTest {
    @Mock
    private UserEmailRepository userEmailRepository;
    @Mock
    private TelemetryClient telemetryClient;

    private AuthUserRoleService service;

    @Before
    public void setUp() {
        service = new AuthUserRoleService(userEmailRepository, telemetryClient);
    }

    @Test
    public void addRole_blank() {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(new UserEmail("user")));

        assertThatThrownBy(() -> service.addRole("user", "        ", "admin")).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: blank");
    }

    @Test
    public void addRole_invalidRole() {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(new UserEmail("user")));

        assertThatThrownBy(() -> service.addRole("user", "BOB", "admin")).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: invalid");
    }

    @Test
    public void addRole_roleAlreadyOnUser() {
        final var user = new UserEmail("user");
        user.setAuthorities(new HashSet<>(List.of(new Authority("ROLE_LICENCE_VARY"))));

        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.addRole("user", "LICENCE_VARY", "admin")).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: exists");
    }

    @Test
    public void addRole_success() throws AuthUserRoleException {
        final var user = new UserEmail("user");
        user.setAuthorities(new HashSet<>(List.of(new Authority("JOE"))));
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));

        service.addRole("user", "ROLE_LICENCE_VARY", "admin");

        assertThat(user.getAuthorities()).extracting(Authority::getAuthority).containsOnly("ROLE_JOE", "ROLE_LICENCE_VARY");
        verify(userEmailRepository).save(user);
    }

    @Test
    public void removeRole_roleNotOnUser() {
        final var user = new UserEmail("user");
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.removeRole("user", "BOB", "admin")).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: missing");
    }

    @Test
    public void removeRole_success() throws AuthUserRoleException {
        final var user = new UserEmail("user");
        user.setAuthorities(new HashSet<>(List.of(new Authority("JOE"), new Authority("LICENCE_VARY"))));
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));

        service.removeRole("user", "  licence_vary   ", "admin");

        assertThat(user.getAuthorities()).extracting(Authority::getAuthority).containsOnly("ROLE_JOE");
        verify(userEmailRepository).save(user);
    }
}
