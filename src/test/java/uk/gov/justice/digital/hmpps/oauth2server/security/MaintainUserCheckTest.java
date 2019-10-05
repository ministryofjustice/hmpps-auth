package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MaintainUserCheckTest {

    @Mock
    private UserRepository userRepository;

    private MaintainUserCheck maintainUserCheck;

    private static final Set<GrantedAuthority> SUPER_USER = Set.of(new SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"));
    private static final Set<GrantedAuthority> GROUP_MANAGER = Set.of(new SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"));


    @Before
    public void setUp() {
        maintainUserCheck = new MaintainUserCheck(userRepository);
    }

    @Test
    public void superUserDoesNotThrowError()  {
        final var user = User.of("user");
        assertThatCode(() -> maintainUserCheck.ensureUserLoggedInUserRelationship("SuperUser",SUPER_USER,user)).doesNotThrowAnyException();
    }

    @Test
    public void groupManagerGroupInCommonWithUserDoesNotThrowError()  {
        final var user = User.of("user");
        final var group1 = new Group("group", "desc");
        user.setGroups(Set.of(group1, new Group("group2", "desc")));
        final var groupManager = User.of("groupManager");
        groupManager.setGroups(Set.of(new Group("group3", "desc"), group1));
        when(userRepository.findByUsernameAndMasterIsTrue(anyString()))
                .thenReturn(Optional.of(groupManager));

        assertThatCode(() -> maintainUserCheck.ensureUserLoggedInUserRelationship("GroupManager",GROUP_MANAGER,user)).doesNotThrowAnyException();

        verify(userRepository, times(1)).findByUsernameAndMasterIsTrue(anyString());
    }

    @Test
    public void groupManagerNoGroupInCommonWithUserThrowsError()  {
        final var user = User.of("user");
        final var optionalUserEmail = createUser();
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUserEmail);

        assertThatThrownBy(() -> maintainUserCheck.ensureUserLoggedInUserRelationship("GroupManager",GROUP_MANAGER,user)).
                isInstanceOf(MaintainUserCheck.AuthUserGroupRelationshipException.class).hasMessage("Unable to maintain user: user with reason: User not with your groups");

        verify(userRepository, times(1)).findByUsernameAndMasterIsTrue(anyString());
    }

    private Optional<User> createUser() {
        return Optional.of(User.of("someUser"));
    }

}





