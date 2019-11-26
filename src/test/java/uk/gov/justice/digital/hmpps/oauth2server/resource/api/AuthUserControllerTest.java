package uk.gov.justice.digital.hmpps.oauth2server.resource.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService.AmendUserException;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService.CreateUserException;
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserGroup;
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail;
import uk.gov.justice.digital.hmpps.oauth2server.resource.api.AuthUserController.AmendUser;
import uk.gov.justice.digital.hmpps.oauth2server.resource.api.AuthUserController.AuthUser;
import uk.gov.justice.digital.hmpps.oauth2server.resource.api.AuthUserController.CreateUser;
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck;
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException;
import uk.gov.service.notify.NotificationClientException;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthUserControllerTest {
    private static final String USER_ID = "07395ef9-53ec-4d6c-8bb1-0dc96cd4bd2f";

    @Mock
    private UserService userService;
    @Mock
    private AuthUserService authUserService;
    @Mock
    private AuthUserGroupService authUserGroupService;
    @Mock
    private HttpServletRequest request;

    private AuthUserController authUserController;

    private final Authentication authentication = new UsernamePasswordAuthenticationToken("bob", "pass");

    @Before
    public void setUp() {
        authUserController = new AuthUserController(userService, authUserService, authUserGroupService, false);
    }

    @Test
    public void user_userNotFound() {
        final var responseEntity = authUserController.user("bob");
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(404);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("Not Found", "Account for username bob not found", "username"));
    }

    @Test
    public void user_success() {
        when(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(getAuthUser()));
        final var responseEntity = authUserController.user("joe");
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(200);
        assertThat(responseEntity.getBody()).isEqualTo(new AuthUser(USER_ID, "authentication", "email", "Joe", "Bloggs", false, true, true, LocalDateTime.of(2019,1,1,12,0)));
    }

    @Test
    public void search() {
        when(authUserService.findAuthUsersByEmail(anyString())).thenReturn(List.of(getAuthUser()));
        final var responseEntity = authUserController.searchForUser("joe");
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(200);
        assertThat(responseEntity.getBody()).isEqualTo(List.of(new AuthUser(USER_ID, "authentication", "email", "Joe", "Bloggs", false, true, true, LocalDateTime.of(2019,1,1,12,0))));
    }

    @Test
    public void search_noResults() {
        when(authUserService.findAuthUsersByEmail(anyString())).thenReturn(List.of());
        final var responseEntity = authUserController.searchForUser("joe");
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(204);
        assertThat(responseEntity.getBody()).isNull();
    }

    @Test
    public void createUser_AlreadyExists() throws NotificationClientException {
        when(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(new UserDetailsImpl("name", "bob", Set.of(), null, null)));
        final var responseEntity = authUserController.createUser("user", new CreateUser("email", "first", "last", null), request, authentication);

        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(409);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("username.exists", "Username user already exists", "username"));
    }

    @Test
    public void createUser_BlankDoesntCallUserService() throws NotificationClientException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/authuser/newusername"));
        final var responseEntity = authUserController.createUser("  ", new CreateUser("email", "first", "last", null), request, authentication);

        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(204);
        verify(userService, never()).findMasterUserPersonDetails(anyString());
    }

    @Test
    public void createUser_Success() throws NotificationClientException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/authuser/newusername"));
        final var responseEntity = authUserController.createUser("newusername", new CreateUser("email", "first", "last", null), request, authentication);

        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(204);
        assertThat(responseEntity.getBody()).isNull();
    }

    @Test
    public void createUser_trim() throws NotificationClientException, CreateUserException, VerifyEmailException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/authuser/newusername"));
        authUserController.createUser("   newusername   ", new CreateUser("email", "first", "last", null), request, authentication);

        verify(userService).findMasterUserPersonDetails("newusername");
        verify(authUserService).createUser("newusername", "email", "first", "last", null, "http://some.url/auth/initial-password?token=", "bob", authentication.getAuthorities());
    }

    @Test
    public void createUser_CreateUserError() throws NotificationClientException, CreateUserException, VerifyEmailException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/authuser/newusername"));
        when(authUserService.createUser(anyString(), anyString(), anyString(), anyString(), isNull(), anyString(), anyString(), any())).thenThrow(new CreateUserException("username", "errorcode"));
        final var responseEntity = authUserController.createUser("newusername", new CreateUser("email", "first", "last", null), request, authentication);

        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(400);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("username.errorcode", "username failed validation", "username"));
    }

    @Test
    public void createUser_VerifyUserError() throws NotificationClientException, CreateUserException, VerifyEmailException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/authuser/newusername"));
        when(authUserService.createUser(anyString(), anyString(), anyString(), anyString(), isNull(), anyString(), anyString(), any())).thenThrow(new VerifyEmailException("reason"));
        final var responseEntity = authUserController.createUser("newusername", new CreateUser("email", "first", "last", null), request, authentication);

        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(400);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("email.reason", "Email address failed validation", "email"));
    }

    @Test
    public void createUser_InitialPasswordUrl() throws NotificationClientException, CreateUserException, VerifyEmailException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/authuser/newusername"));

        authUserController.createUser("newusername", new CreateUser("email", "first", "last", null), request, authentication);

        verify(authUserService).createUser("newusername", "email", "first", "last", null, "http://some.url/auth/initial-password?token=", "bob", authentication.getAuthorities());
    }

    @Test
    public void createUser_NoAdditionalRoles() throws NotificationClientException, CreateUserException, VerifyEmailException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/authuser/newusername"));

        authUserController.createUser("newusername", new CreateUser("email", "first", "last", null), request, authentication);

        verify(authUserService).createUser("newusername", "email", "first", "last", null, "http://some.url/auth/initial-password?token=", "bob", authentication.getAuthorities());
    }

    @Test
    public void createUser() throws NotificationClientException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/api/authuser/newusername"));
        final var responseEntity = authUserController.createUser("newusername", new CreateUser("email", "first", "last", null), request, authentication);

        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(204);
        assertThat(responseEntity.getBody()).isNull();
    }

    @Test
    public void enableUser() throws MaintainUserCheck.AuthUserGroupRelationshipException {
        final var user = User.builder().username("USER").email("email").verified(true).build();
        when(authUserService.getAuthUserByUsername("user")).thenReturn(Optional.of(user));
        final var responseEntity = authUserController.enableUser("user", authentication);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(204);
        verify(authUserService).enableUser("USER", "bob", authentication.getAuthorities());
    }

    @Test
    public void enableUser_notFound() throws MaintainUserCheck.AuthUserGroupRelationshipException {
        final var user = User.builder().username("USER").email("email").verified(true).build();
        when(authUserService.getAuthUserByUsername("user")).thenReturn(Optional.of(user));
        doThrow(new EntityNotFoundException("message")).when(authUserService).enableUser(anyString(), anyString(), any());
        final var responseEntity = authUserController.enableUser("user", authentication);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(404);
    }

    @Test
    public void disableUser() throws MaintainUserCheck.AuthUserGroupRelationshipException {
        final var user = User.builder().username("USER").email("email").verified(true).build();
        when(authUserService.getAuthUserByUsername("user")).thenReturn(Optional.of(user));
        final var responseEntity = authUserController.disableUser("user", authentication);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(204);
        verify(authUserService).disableUser("USER", "bob", authentication.getAuthorities());
    }

    @Test
    public void disableUser_notFound() throws MaintainUserCheck.AuthUserGroupRelationshipException {
        final var user = User.builder().username("USER").email("email").verified(true).build();
        when(authUserService.getAuthUserByUsername("user")).thenReturn(Optional.of(user));
        doThrow(new EntityNotFoundException("message")).when(authUserService).disableUser(anyString(), anyString(), any());
        final var responseEntity = authUserController.disableUser("user", authentication);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(404);
    }

    @Test
    public void amendUser_checkService() throws NotificationClientException, VerifyEmailException, AmendUserException, AuthUserGroupRelationshipException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/authuser/newusername"));

        authUserController.amendUser("user", new AmendUser("a@b.com"), request, authentication);

        verify(authUserService).amendUser("user", "a@b.com", "http://some.url/auth/initial-password?token=", "bob", authentication.getAuthorities());
    }

    @Test
    public void amendUser_statusCode() throws NotificationClientException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/authuser/newusername"));

        final var responseEntity = authUserController.amendUser("user", new AmendUser("a@b.com"), request, authentication);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(204);
        assertThat(responseEntity.getBody()).isNull();
    }

    @Test
    public void amendUser_notFound() throws NotificationClientException, VerifyEmailException, AmendUserException, AuthUserGroupRelationshipException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/authuser/newusername"));
        when(authUserService.amendUser(anyString(), anyString(), anyString(), anyString(), any())).thenThrow(new EntityNotFoundException("not found"));

        final var responseEntity = authUserController.amendUser("user", new AmendUser("a@b.com"), request, authentication);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(404);
    }

    @Test
    public void amendUser_amendException() throws NotificationClientException, VerifyEmailException, AmendUserException, AuthUserGroupRelationshipException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/authuser/newusername"));
        when(authUserService.amendUser(anyString(), anyString(), anyString(), anyString(), any())).thenThrow(new AmendUserException("fiel", "cod"));

        final var responseEntity = authUserController.amendUser("user", new AmendUser("a@b.com"), request, authentication);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(400);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("fiel.cod", "fiel failed validation", "fiel"));
    }

    @Test
    public void amendUser_verifyException() throws NotificationClientException, VerifyEmailException, AmendUserException, AuthUserGroupRelationshipException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/authuser/newusername"));
        when(authUserService.amendUser(anyString(), anyString(), anyString(), anyString(), any())).thenThrow(new VerifyEmailException("reason"));

        final var responseEntity = authUserController.amendUser("user", new AmendUser("a@b.com"), request, authentication);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(400);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("email.reason", "Email address failed validation", "email"));
    }

    @Test
    public void amendUser_groupException() throws NotificationClientException, VerifyEmailException, AmendUserException, AuthUserGroupRelationshipException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/authuser/newusername"));
        when(authUserService.amendUser(anyString(), anyString(), anyString(), anyString(), any())).thenThrow(new AuthUserGroupRelationshipException("user", "reason"));

        final var responseEntity = authUserController.amendUser("user", new AmendUser("a@b.com"), request, authentication);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(403);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("unable to maintain user", "Unable to amend user, the user is not within one of your groups", "groups"));
    }

    @Test
    public void assignableGroups_success() {
        final var group1 = new Group("FRED", "desc");
        final var group2 = new Group("GLOBAL_SEARCH", "desc2");
        when(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(List.of(group1, group2));
        final var responseEntity = authUserController.assignableGroups(authentication);
        assertThat(responseEntity).containsOnly(new AuthUserGroup(group1), new AuthUserGroup(group2));
    }

    private User getAuthUser() {
        final var user = User.builder().id(UUID.fromString(USER_ID)).username("authentication").email("email").verified(true).enabled(true).lastLoggedIn(LocalDateTime.of(2019,1,1,12,0)).build();
        user.setPerson(new Person());
        user.getPerson().setFirstName("Joe");
        user.getPerson().setLastName("Bloggs");
        return user;
    }

    @Test
    public void searchForUser() {
        final var unpaged = Pageable.unpaged();
        when(authUserService.findAuthUsers(anyString(), anyString(), anyString(), any())).thenReturn(new PageImpl<>(List.of(getAuthUser())));
        authUserController.searchForUser("somename", "somerole", "somegroup", unpaged);
        verify(authUserService).findAuthUsers("somename", "somerole", "somegroup", unpaged);
    }
}
