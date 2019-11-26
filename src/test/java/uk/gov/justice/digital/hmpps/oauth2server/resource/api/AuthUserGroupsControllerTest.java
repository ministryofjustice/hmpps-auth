package uk.gov.justice.digital.hmpps.oauth2server.resource.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService.AuthUserGroupException;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService.AuthUserGroupExistsException;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService;
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserGroup;
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthUserGroupsControllerTest {
    private final Principal principal = new UsernamePasswordAuthenticationToken("bob", "pass");

    @Mock
    private AuthUserService authUserService;
    @Mock
    private AuthUserGroupService authUserGroupService;

    private AuthUserGroupsController authUserGroupsController;

    @Before
    public void setUp() {
        authUserGroupsController = new AuthUserGroupsController(authUserService, authUserGroupService);
    }

    @Test
    public void groups_userNotFound() {
        final var responseEntity = authUserGroupsController.groups("bob");
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(404);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("Not Found", "Account for username bob not found", "username"));
    }

    @Test
    public void groups_success() {
        final var group1 = new Group("FRED", "desc");
        final var group2 = new Group("GLOBAL_SEARCH", "desc2");
        when(authUserGroupService.getAuthUserGroups(anyString())).thenReturn(Optional.of(Set.of(group1, group2)));
        final var responseEntity = authUserGroupsController.groups("joe");
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(200);
        //noinspection unchecked
        assertThat(((List) responseEntity.getBody())).containsOnly(new AuthUserGroup(group1), new AuthUserGroup(group2));
    }

    @Test
    public void addGroup_userNotFound() {
        final var responseEntity = authUserGroupsController.addGroup("bob", "group", principal);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(404);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("Not Found", "Account for username bob not found", "username"));
    }

    @Test
    public void addGroup_success() throws AuthUserGroupException {
        when(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(getAuthUser()));
        final var responseEntity = authUserGroupsController.addGroup("someuser", "group", principal);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(204);
        verify(authUserGroupService).addGroup("USER", "group", "bob");
    }

    @Test
    public void addGroup_conflict() throws AuthUserGroupException {
        when(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(getAuthUser()));
        doThrow(new AuthUserGroupExistsException()).when(authUserGroupService).addGroup(anyString(), anyString(), anyString());

        final var responseEntity = authUserGroupsController.addGroup("someuser", "joe", principal);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(409);
    }

    @Test
    public void addGroup_validation() throws AuthUserGroupException {
        when(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(getAuthUser()));

        doThrow(new AuthUserGroupException("group", "error")).when(authUserGroupService).addGroup(anyString(), anyString(), anyString());
        final var responseEntity = authUserGroupsController.addGroup("someuser", "harry", principal);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(400);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("group.error", "group failed validation", "group"));
    }

    @Test
    public void removeGroup_userNotFound() {
        final var responseEntity = authUserGroupsController.removeGroup("bob", "group", principal);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(404);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("Not Found", "Account for username bob not found", "username"));
    }

    @Test
    public void removeGroup_success() throws AuthUserGroupException {
        when(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(getAuthUser()));
        final var responseEntity = authUserGroupsController.removeGroup("someuser", "joe", principal);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(204);
        verify(authUserGroupService).removeGroup("USER", "joe", "bob");
    }

    @Test
    public void removeGroup_groupMissing() throws AuthUserGroupException {
        when(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(getAuthUser()));
        doThrow(new AuthUserGroupException("group", "error")).when(authUserGroupService).removeGroup(anyString(), anyString(), anyString());

        final var responseEntity = authUserGroupsController.removeGroup("someuser", "harry", principal);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(400);
    }

    private User getAuthUser() {
        final var user = User.builder().username("USER").email("email").verified(true).build();
        user.setGroups(Set.of(new Group("GLOBAL_SEARCH", "desc2"), new Group("FRED", "desc")));
        return user;
    }
}
