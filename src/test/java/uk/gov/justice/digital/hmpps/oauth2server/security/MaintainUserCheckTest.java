package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MaintainUserCheckTest {

    @Mock
    private UserEmailRepository userEmailRepository;

    private MaintainUserCheck maintainUserCheck;

    private static final Set<GrantedAuthority> SUPER_USER = Set.of(new SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"));
    private static final Set<GrantedAuthority> GROUP_MANAGER = Set.of(new SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"));


    @Before
    public void setUp() {
        maintainUserCheck = new MaintainUserCheck(userEmailRepository);
    }

    @Test
    public void superUserDoesNotThrowError()  {
        final var user = new UserEmail("user");
        assertThatCode(() -> maintainUserCheck.ensureUserLoggedInUserRelationship("SuperUser",SUPER_USER,user)).doesNotThrowAnyException();
    }

    @Test
    public void groupManagerGroupInCommonWithUserDoesNotThrowError()  {
        final var user = new UserEmail("user");
        final var group1 = new Group("group", "desc");
        user.setGroups(Set.of(group1, new Group("group2", "desc")));
        final var groupManager = new UserEmail("groupManager");
        groupManager.setGroups(Set.of(new Group("group3", "desc"), group1));
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString()))
                .thenReturn(Optional.of(groupManager));

        assertThatCode(() -> maintainUserCheck.ensureUserLoggedInUserRelationship("GroupManager",GROUP_MANAGER,user)).doesNotThrowAnyException();

        verify(userEmailRepository, times(1)).findByUsernameAndMasterIsTrue(anyString());
    }

    @Test
    public void groupManagerNoGroupInCommonWithUserThrowsError()  {
        final var user = new UserEmail("user");
        final var optionalUserEmail = createUserEmailUser();
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUserEmail);

        assertThatThrownBy(() -> maintainUserCheck.ensureUserLoggedInUserRelationship("GroupManager",GROUP_MANAGER,user)).
                isInstanceOf(MaintainUserCheck.AuthUserGroupRelationshipException.class).hasMessage("Unable to maintain user: user with reason: User not with your groups");

        verify(userEmailRepository, times(1)).findByUsernameAndMasterIsTrue(anyString());
    }

    private Optional<UserEmail> createUserEmailUser() {
        return Optional.of(new UserEmail("someUser"));
    }

}





