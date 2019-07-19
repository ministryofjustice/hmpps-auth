package uk.gov.justice.digital.hmpps.oauth2server.resource.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService.AuthUserRoleException;
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserRole;
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail;
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthUserRolesControllerTest {
    private final Authentication principal = new UsernamePasswordAuthenticationToken("bob", "pass");

    @Mock
    private UserService userService;
    @Mock
    private AuthUserRoleService authUserRoleService;

    private AuthUserRolesController authUserRolesController;

    @Before
    public void setUp() {
        authUserRolesController = new AuthUserRolesController(userService, authUserRoleService);
    }

    @Test
    public void roles_userNotFound() {
        final var responseEntity = authUserRolesController.roles("bob");
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(404);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("Not Found", "Account for username bob not found", "username"));
    }

    @Test
    public void roles_success() {
        when(userService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(getAuthUser()));
        final var responseEntity = authUserRolesController.roles("joe");
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(200);
        //noinspection unchecked
        assertThat(((Set) responseEntity.getBody())).containsOnly(new AuthUserRole(new Authority("FRED", "FRED")), new AuthUserRole(new Authority("GLOBAL_SEARCH", "Global Search")));
    }

    @Test
    public void addRole_userNotFound() {
        final var responseEntity = authUserRolesController.addRole("bob", "role", principal);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(404);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("Not Found", "Account for username bob not found", "username"));
    }

    @Test
    public void addRole_success() throws AuthUserRoleException, MaintainUserCheck.AuthUserGroupRelationshipException {
        when(userService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(getAuthUser()));
        final var responseEntity = authUserRolesController.addRole("someuser", "role", principal);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(204);
        verify(authUserRoleService).addRole("USER", "role", "bob", principal.getAuthorities());
    }

    @Test
    public void addRole_conflict() throws AuthUserRoleException, MaintainUserCheck.AuthUserGroupRelationshipException {
        when(userService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(getAuthUser()));
        doThrow(new MaintainUserCheck.AuthUserGroupRelationshipException("someuser","User not with your groups")).when(authUserRoleService).addRole(anyString(), anyString(), anyString(), any());

        final var responseEntity = authUserRolesController.addRole("someuser", "joe", principal);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(409);
    }

    @Test
    public void addRole_validation() throws AuthUserRoleException, MaintainUserCheck.AuthUserGroupRelationshipException {
        when(userService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(getAuthUser()));

        doThrow(new AuthUserRoleException("role", "error")).when(authUserRoleService).addRole(anyString(), anyString(), anyString(), any());
        final var responseEntity = authUserRolesController.addRole("someuser", "harry", principal);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(400);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("role.error", "role failed validation", "role"));
    }

    @Test
    public void removeRole_userNotFound() {
        final var responseEntity = authUserRolesController.removeRole("bob", "role", principal);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(404);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("Not Found", "Account for username bob not found", "username"));
    }

    @Test
    public void removeRole_success() throws AuthUserRoleException, MaintainUserCheck.AuthUserGroupRelationshipException {
        when(userService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(getAuthUser()));
        final var responseEntity = authUserRolesController.removeRole("someuser", "joe", principal);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(204);
        verify(authUserRoleService).removeRole("USER", "joe", "bob", principal.getAuthorities());
    }

    @Test
    public void removeRole_roleMissing() throws AuthUserRoleException, MaintainUserCheck.AuthUserGroupRelationshipException {
        when(userService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(getAuthUser()));
        doThrow(new AuthUserRoleException("role", "error")).when(authUserRoleService).removeRole(anyString(), anyString(), anyString(), any());

        final var responseEntity = authUserRolesController.removeRole("someuser", "harry", principal);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(400);
    }

    @Test
    public void getAssignableRoles() {
        when(authUserRoleService.getAssignableRoles(anyString(), any())).thenReturn(List.of(new Authority("FRED", "FRED"), new Authority("GLOBAL_SEARCH", "Global Search")));

        final var response = authUserRolesController.assignableRoles("someuser", principal);
        assertThat(response).containsOnly(
                new AuthUserRole(new Authority("FRED", "FRED")),
                new AuthUserRole(new Authority("GLOBAL_SEARCH", "Global Search")));
    }

    private UserEmail getAuthUser() {
        final var user = new UserEmail("USER", "email", true, false);

        user.setAuthorities(Set.of(new Authority("FRED", "FRED"), new Authority("GLOBAL_SEARCH", "Global Search")));
        return user;
    }
}
