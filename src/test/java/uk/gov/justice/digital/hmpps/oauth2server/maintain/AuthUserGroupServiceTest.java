package uk.gov.justice.digital.hmpps.oauth2server.maintain;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.GroupRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService.AuthUserGroupException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthUserGroupServiceTest {
    @Mock
    private UserEmailRepository userEmailRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private TelemetryClient telemetryClient;

    private AuthUserGroupService service;

    @Before
    public void setUp() {
        service = new AuthUserGroupService(userEmailRepository, groupRepository, telemetryClient);
    }

    @Test
    public void addGroup_blank() {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(new UserEmail("user")));
        when(groupRepository.findByGroupCode(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addGroup("user", "        ", "admin")).
                isInstanceOf(AuthUserGroupException.class).hasMessage("Add group failed for field group with reason: notfound");
    }

    @Test
    public void addGroup_groupAlreadyOnUser() {
        final var user = new UserEmail("user");
        final var group = new Group("GROUP_LICENCE_VARY", "desc");
        user.setGroups(new HashSet<>(List.of(group)));

        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));
        when(groupRepository.findByGroupCode(anyString())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.addGroup("user", "LICENCE_VARY", "admin")).
                isInstanceOf(AuthUserGroupException.class).hasMessage("Add group failed for field group with reason: exists");
    }

    @Test
    public void addGroup_success() throws AuthUserGroupException {
        final var user = new UserEmail("user");
        user.setGroups(new HashSet<>(List.of(new Group("GROUP_JOE", "desc"))));

        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));

        final var group = new Group("GROUP_LICENCE_VARY", "desc");
        when(groupRepository.findByGroupCode(anyString())).thenReturn(Optional.of(group));

        service.addGroup("user", "GROUP_LICENCE_VARY", "admin");

        assertThat(user.getGroups()).extracting(Group::getGroupCode).containsOnly("GROUP_JOE", "GROUP_LICENCE_VARY");
    }

    @Test
    public void removeGroup_groupNotOnUser() {
        final var user = new UserEmail("user");
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.removeGroup("user", "BOB", "admin")).
                isInstanceOf(AuthUserGroupException.class).hasMessage("Add group failed for field group with reason: missing");
    }

    @Test
    public void removeGroup_success() throws AuthUserGroupException {
        final var user = new UserEmail("user");
        user.setGroups(new HashSet<>(List.of(new Group("JOE", "desc"), new Group("LICENCE_VARY", "desc2"))));
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));

        service.removeGroup("user", "  licence_vary   ", "admin");

        assertThat(user.getGroups()).extracting(Group::getGroupCode).containsOnly("JOE");
    }

    @Test
    public void getAuthUserGroups_notfound() {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.empty());
        final var groups = service.getAuthUserGroups(" BOB ");
        assertThat(groups).isNotPresent();
    }

    @Test
    public void getAuthUserAssignableGroups_notAdminAndNoUser() {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.empty());
        final var groups = service.getAssignableGroups(" BOB ", Set.of());
        assertThat(groups).isEmpty();
    }

    @Test
    public void getAuthUserGroups_success() {
        final var user = new UserEmail("user");
        user.setGroups(new HashSet<>(List.of(new Group("JOE", "desc"), new Group("LICENCE_VARY", "desc2"))));
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));
        final var groups = service.getAuthUserGroups(" BOB ");
        //noinspection OptionalGetWithoutIsPresent
        assertThat(groups.get()).extracting(Group::getGroupCode).containsOnly("JOE", "LICENCE_VARY");
    }

    @Test
    public void getAuthUserAssignableGroups_normalUser() {
        final var user = new UserEmail("user");
        user.setGroups(new HashSet<>(List.of(new Group("JOE", "desc"), new Group("LICENCE_VARY", "desc2"))));
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));
        final var groups = service.getAssignableGroups(" BOB ", Set.of());
        assertThat(groups).extracting(Group::getGroupCode).containsOnly("JOE", "LICENCE_VARY");
    }

    @Test
    public void getAuthUserAssignableGroups_superUser() {
        when(groupRepository.findAll()).thenReturn(List.of(new Group("JOE", "desc"), new Group("LICENCE_VARY", "desc2")));
        final var groups = service.getAssignableGroups(" BOB ", Set.of(new SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS")));
        assertThat(groups).extracting(Group::getGroupCode).containsOnly("JOE", "LICENCE_VARY");
    }
}
