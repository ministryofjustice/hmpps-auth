package uk.gov.justice.digital.hmpps.oauth2server.maintain;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.RoleRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService.AuthUserRoleException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthUserRolesServiceTest {
    @Mock
    private UserEmailRepository userEmailRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private TelemetryClient telemetryClient;

    private AuthUserRoleService service;

    private static final Set<GrantedAuthority> GRANTED_AUTHORITY_SUPER_USER = Set.of(new SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"));

    @Before
    public void setUp() {
        service = new AuthUserRoleService(userEmailRepository, roleRepository, telemetryClient);
    }

    @Test
    public void addRole_notfound() {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(new UserEmail("user")));

        assertThatThrownBy(() -> service.addRole("user", "        ", "admin", GRANTED_AUTHORITY_SUPER_USER)).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: notfound");
    }

    @Test
    public void addRole_invalidRole() {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(new UserEmail("user")));
        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        when(roleRepository.findByAuthority(anyString())).thenReturn(Optional.of(role));
        when(roleRepository.findAllByOrderByRoleName()).thenReturn(List.of(new Authority("FRED", "Role Fred")));

        assertThatThrownBy(() -> service.addRole("user", "BOB", "admin", GRANTED_AUTHORITY_SUPER_USER)).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: invalid");
    }

    @Test
    public void addRole_oauthAdminRestricted() {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(new UserEmail("user")));
        final var role = new Authority("ROLE_OAUTH_ADMIN", "Role Licence Vary");
        when(roleRepository.findByAuthority(anyString())).thenReturn(Optional.of(role));
        when(roleRepository.findAllByOrderByRoleName()).thenReturn(List.of(role));

        assertThatThrownBy(() -> service.addRole("user", "BOB", "admin", GRANTED_AUTHORITY_SUPER_USER)).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: invalid");
    }

    @Test
    public void addRole_oauthAdminRestricted_success() throws AuthUserRoleException {
        final UserEmail user = new UserEmail("user");
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));
        final var role = new Authority("ROLE_OAUTH_ADMIN", "Role Auth Admin");
        when(roleRepository.findByAuthority(anyString())).thenReturn(Optional.of(role));
        when(roleRepository.findAllByOrderByRoleName()).thenReturn(List.of(role));

        service.addRole("user", "BOB", "admin",
                Set.of(new SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"), new SimpleGrantedAuthority("ROLE_OAUTH_ADMIN")));

        assertThat(user.getAuthorities()).extracting(Authority::getAuthority).containsOnly("ROLE_OAUTH_ADMIN");

    }

    @Test
    public void addRole_roleAlreadyOnUser() {
        final var user = new UserEmail("user");
        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        user.setAuthorities(new HashSet<>(List.of(role)));

        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));
        when(roleRepository.findByAuthority(anyString())).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> service.addRole("user", "LICENCE_VARY", "admin", GRANTED_AUTHORITY_SUPER_USER)).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: exists");
    }

    @Test
    public void addRole_success() throws AuthUserRoleException {
        final var user = new UserEmail("user");
        user.setAuthorities(new HashSet<>(List.of(new Authority("JOE", "bloggs"))));
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));
        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        when(roleRepository.findByAuthority(anyString())).thenReturn(Optional.of(role));
        when(roleRepository.findAllByOrderByRoleName()).thenReturn(List.of(role));

        service.addRole("user", "ROLE_LICENCE_VARY", "admin", GRANTED_AUTHORITY_SUPER_USER);

        assertThat(user.getAuthorities()).extracting(Authority::getAuthority).containsOnly("ROLE_JOE", "ROLE_LICENCE_VARY");
    }

    @Test
    public void removeRole_roleNotOnUser() {
        final var user = new UserEmail("user");
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.removeRole("user", "BOB", "admin", GRANTED_AUTHORITY_SUPER_USER)).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: missing");
    }

    @Test
    public void removeRole_invalid() {
        final var user = new UserEmail("user");
        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        final var role2 = new Authority("BOB", "Bloggs");
        user.setAuthorities(new HashSet<>(List.of(role, role2)));
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));
        when(roleRepository.findByAuthority(anyString())).thenReturn(Optional.of(role2));
        when(roleRepository.findAllByOrderByRoleName()).thenReturn(List.of(role));

        assertThatThrownBy(() -> service.removeRole("user", "BOB", "admin", GRANTED_AUTHORITY_SUPER_USER)).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: invalid");
    }

    @Test
    public void removeRole_notfound() {
        final var user = new UserEmail("user");
        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        final var role2 = new Authority("BOB", "Bloggs");
        user.setAuthorities(new HashSet<>(List.of(role, role2)));
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.removeRole("user", "BOB", "admin", GRANTED_AUTHORITY_SUPER_USER)).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: notfound");
    }

    @Test
    public void removeRole_success() throws AuthUserRoleException {
        final var user = new UserEmail("user");
        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        final var role2 = new Authority("JOE", "Bloggs");
        user.setAuthorities(new HashSet<>(List.of(role, role2)));
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));
        when(roleRepository.findByAuthority(anyString())).thenReturn(Optional.of(role));
        when(roleRepository.findAllByOrderByRoleName()).thenReturn(List.of(role, role2));

        service.removeRole("user", "  licence_vary   ", "admin", GRANTED_AUTHORITY_SUPER_USER);

        assertThat(user.getAuthorities()).extracting(Authority::getAuthority).containsOnly("ROLE_JOE");
    }
}
