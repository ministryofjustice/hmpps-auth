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
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.RoleRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService.AuthUserRoleException;
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthUserRolesServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private TelemetryClient telemetryClient;
    @Mock
    private MaintainUserCheck maintainUserCheck;

    private AuthUserRoleService service;

    private static final Set<GrantedAuthority> SUPER_USER = Set.of(new SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"));
    private static final Set<GrantedAuthority> GROUP_MANAGER = Set.of(new SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"));

    @Before
    public void setUp() {
        service = new AuthUserRoleService(userRepository, roleRepository, telemetryClient, maintainUserCheck);
    }

    @Test
    public void addRole_notfound() {
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(User.of("user")));

        assertThatThrownBy(() -> service.addRole("user", "        ", "admin", SUPER_USER)).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: notfound");
    }

    @Test
    public void addRole_invalidRole() {
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(User.of("user")));
        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        when(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role));
        when(roleRepository.findAllByOrderByRoleName()).thenReturn(List.of(new Authority("FRED", "Role Fred")));

        assertThatThrownBy(() -> service.addRole("user", "BOB", "admin", SUPER_USER)).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: invalid");
    }

    @Test
    public void addRole_noaccess() throws MaintainUserCheck.AuthUserGroupRelationshipException {
        final var user = User.of("user");
        user.setGroups(Set.of(new Group("group", "desc")));
        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        final var role2 = new Authority("BOB", "Bloggs");
        user.setAuthorities(new HashSet<>(List.of(role, role2)));
        final var groupManager = User.of("groupManager");
        groupManager.setGroups(Set.of(new Group("group2", "desc")));
        when(userRepository.findByUsernameAndMasterIsTrue(anyString()))
                .thenReturn(Optional.of(user))
                .thenReturn(Optional.of(groupManager));
        doThrow(new MaintainUserCheck.AuthUserGroupRelationshipException("user", "User not with your groups")).when(maintainUserCheck).ensureUserLoggedInUserRelationship(anyString(), anyCollection(), any(User.class));

        assertThatThrownBy(() -> service.addRole("user", "BOB", "admin", GROUP_MANAGER)).
                isInstanceOf(MaintainUserCheck.AuthUserGroupRelationshipException.class).hasMessage("Unable to maintain user: user with reason: User not with your groups");
    }

    @Test
    public void addRole_invalidRoleGroupManager() {
        final var user = User.of("user");
        final var group1 = new Group("group", "desc");
        user.setGroups(Set.of(group1));
        final var groupManager = User.of("groupManager");
        groupManager.setGroups(Set.of(group1));
        when(userRepository.findByUsernameAndMasterIsTrue(anyString()))
                .thenReturn(Optional.of(user))
                .thenReturn(Optional.of(groupManager));
        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        when(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role));
        when(roleRepository.findByGroupAssignableRolesForUsername(anyString())).thenReturn(List.of(new Authority("FRED", "Role Fred")));

        assertThatThrownBy(() -> service.addRole("user", "BOB", "admin", GROUP_MANAGER)).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: invalid");
    }

    @Test
    public void addRole_oauthAdminRestricted() {
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(User.of("user")));
        final var role = new Authority("ROLE_OAUTH_ADMIN", "Role Licence Vary");
        when(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role));
        when(roleRepository.findAllByOrderByRoleName()).thenReturn(List.of(role));

        assertThatThrownBy(() -> service.addRole("user", "BOB", "admin", SUPER_USER)).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: invalid");
    }

    @Test
    public void addRole_oauthAdminRestricted_success() throws AuthUserRoleException, MaintainUserCheck.AuthUserGroupRelationshipException {
        final var user = User.of("user");
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));
        final var role = new Authority("ROLE_OAUTH_ADMIN", "Role Auth Admin");
        when(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role));
        when(roleRepository.findAllByOrderByRoleName()).thenReturn(List.of(role));

        service.addRole("user", "BOB", "admin",
                Set.of(new SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"), new SimpleGrantedAuthority("ROLE_OAUTH_ADMIN")));

        assertThat(user.getAuthorities()).extracting(Authority::getAuthority).containsOnly("ROLE_OAUTH_ADMIN");

    }

    @Test
    public void addRole_roleAlreadyOnUser() {
        final var user = User.of("user");
        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        user.setAuthorities(new HashSet<>(List.of(role)));

        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));
        when(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> service.addRole("user", "LICENCE_VARY", "admin", SUPER_USER)).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: exists");
    }

    @Test
    public void addRole_success() throws AuthUserRoleException, MaintainUserCheck.AuthUserGroupRelationshipException {
        final var user = User.of("user");
        user.setAuthorities(new HashSet<>(List.of(new Authority("JOE", "bloggs"))));
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));
        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        when(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role));
        when(roleRepository.findAllByOrderByRoleName()).thenReturn(List.of(role));

        service.addRole("user", "ROLE_LICENCE_VARY", "admin", SUPER_USER);

        assertThat(user.getAuthorities()).extracting(Authority::getAuthority).containsOnly("ROLE_JOE", "ROLE_LICENCE_VARY");
    }

    @Test
    public void addRole_successGroupManager() throws AuthUserRoleException, MaintainUserCheck.AuthUserGroupRelationshipException {
        final var user = User.of("user");
        final var group1 = new Group("group", "desc");
        user.setGroups(Set.of(group1, new Group("group2", "desc")));

        user.setAuthorities(new HashSet<>(List.of(new Authority("JOE", "bloggs"))));
        final var groupManager = User.of("groupManager");
        groupManager.setGroups(Set.of(new Group("group3", "desc"), group1));
        when(userRepository.findByUsernameAndMasterIsTrue(anyString()))
                .thenReturn(Optional.of(user))
                .thenReturn(Optional.of(groupManager));
        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        when(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role));
        when(roleRepository.findByGroupAssignableRolesForUsername(anyString())).thenReturn(List.of(role));

        service.addRole("user", "ROLE_LICENCE_VARY", "admin", GROUP_MANAGER);

        assertThat(user.getAuthorities()).extracting(Authority::getAuthority).containsOnly("ROLE_JOE", "ROLE_LICENCE_VARY");
    }

    @Test
    public void removeRole_roleNotOnUser() {
        final var user = User.of("user");
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));
        final var role2 = new Authority("BOB", "Bloggs");
        when(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role2));

        assertThatThrownBy(() -> service.removeRole("user", "BOB", "admin", SUPER_USER)).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: missing");
    }

    @Test
    public void removeRole_invalid() {
        final var user = User.of("user");
        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        final var role2 = new Authority("BOB", "Bloggs");
        user.setAuthorities(new HashSet<>(List.of(role, role2)));
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));
        when(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role2));
        when(roleRepository.findAllByOrderByRoleName()).thenReturn(List.of(role));

        assertThatThrownBy(() -> service.removeRole("user", "BOB", "admin", SUPER_USER)).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: invalid");
    }

    @Test
    public void removeRole_noaccess() throws MaintainUserCheck.AuthUserGroupRelationshipException {
        final var user = User.of("user");
        user.setGroups(Set.of(new Group("group", "desc")));
        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        final var role2 = new Authority("BOB", "Bloggs");
        final var groupManager = User.of("groupManager");
        groupManager.setGroups(Set.of(new Group("group2", "desc")));
        user.setAuthorities(new HashSet<>(List.of(role, role2)));
        doThrow(new MaintainUserCheck.AuthUserGroupRelationshipException("user", "User not with your groups")).when(maintainUserCheck).ensureUserLoggedInUserRelationship(anyString(), anyCollection(), any(User.class));

        when(userRepository.findByUsernameAndMasterIsTrue(anyString()))
                .thenReturn(Optional.of(user))
                .thenReturn(Optional.of(groupManager));

        assertThatThrownBy(() -> service.removeRole("user", "BOB", "admin", GROUP_MANAGER)).
                isInstanceOf(MaintainUserCheck.AuthUserGroupRelationshipException.class).hasMessage("Unable to maintain user: user with reason: User not with your groups");
    }

    @Test
    public void removeRole_invalidGroupManager() {
        final var user = User.of("user");
        final var group1 = new Group("group", "desc");
        user.setGroups(Set.of(group1));
        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        final var role2 = new Authority("BOB", "Bloggs");
        final var groupManager = User.of("groupManager");
        groupManager.setGroups(Set.of(group1));
        user.setAuthorities(new HashSet<>(List.of(role, role2)));
        when(userRepository.findByUsernameAndMasterIsTrue(anyString()))
                .thenReturn(Optional.of(user))
                .thenReturn(Optional.of(groupManager));

        when(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role2));
        when(roleRepository.findByGroupAssignableRolesForUsername(anyString())).thenReturn(List.of(role));

        assertThatThrownBy(() -> service.removeRole("user", "BOB", "admin", GROUP_MANAGER)).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: invalid");
    }

    @Test
    public void removeRole_notfound() {
        final var user = User.of("user");
        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        final var role2 = new Authority("BOB", "Bloggs");
        user.setAuthorities(new HashSet<>(List.of(role, role2)));
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.removeRole("user", "BOB", "admin", SUPER_USER)).
                isInstanceOf(AuthUserRoleException.class).hasMessage("Add role failed for field role with reason: notfound");
    }

    @Test
    public void removeRole_success() throws AuthUserRoleException, MaintainUserCheck.AuthUserGroupRelationshipException {
        final var user = User.of("user");
        final var group1 = new Group("group", "desc");
        user.setGroups(Set.of(group1));

        user.setAuthorities(new HashSet<>(List.of(new Authority("JOE", "bloggs"))));
        final var groupManager = User.of("groupManager");
        groupManager.setGroups(Set.of(group1));
        when(userRepository.findByUsernameAndMasterIsTrue(anyString()))
                .thenReturn(Optional.of(user))
                .thenReturn(Optional.of(groupManager));

        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        final var role2 = new Authority("JOE", "Bloggs");
        user.setAuthorities(new HashSet<>(List.of(role, role2)));
        when(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role));
        when(roleRepository.findAllByOrderByRoleName()).thenReturn(List.of(role, role2));

        service.removeRole("user", "  licence_vary   ", "admin", SUPER_USER);

        assertThat(user.getAuthorities()).extracting(Authority::getAuthority).containsOnly("ROLE_JOE");
    }

    @Test
    public void removeRole_successGroupManager() throws AuthUserRoleException, MaintainUserCheck.AuthUserGroupRelationshipException {
        final var user = User.of("user");
        final var group1 = new Group("group", "desc");
        user.setGroups(Set.of(group1));
        final var role = new Authority("ROLE_LICENCE_VARY", "Role Licence Vary");
        final var role2 = new Authority("JOE", "Bloggs");
        user.setAuthorities(new HashSet<>(List.of(role, role2)));
        final var groupManager = User.of("groupManager");
        groupManager.setGroups(Set.of(group1));
        when(userRepository.findByUsernameAndMasterIsTrue(anyString()))
                .thenReturn(Optional.of(user))
                .thenReturn(Optional.of(groupManager));

        when(roleRepository.findByRoleCode(anyString())).thenReturn(Optional.of(role));
        when(roleRepository.findByGroupAssignableRolesForUsername(anyString())).thenReturn(List.of(role, role2));

        service.removeRole("user", "  licence_vary   ", "admin", GROUP_MANAGER);

        assertThat(user.getAuthorities()).extracting(Authority::getAuthority).containsOnly("ROLE_JOE");
    }
}
