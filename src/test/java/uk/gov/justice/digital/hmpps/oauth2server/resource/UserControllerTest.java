package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.CreateUserService;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.CreateUserService.CreateUserException;
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail;
import uk.gov.justice.digital.hmpps.oauth2server.model.UserDetail;
import uk.gov.justice.digital.hmpps.oauth2server.model.UserRole;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.resource.UserController.CreateUser;
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException;
import uk.gov.service.notify.NotificationClientException;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UserControllerTest {
    @Mock
    private UserService userService;
    @Mock
    private CreateUserService createUserService;
    @Mock
    private HttpServletRequest request;

    private UserController userController;

    @Before
    public void setUp() {
        userController = new UserController(userService, createUserService, true);
    }

    @Test
    public void user_userNotFound() {
        final var responseEntity = userController.user("bob");
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(404);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("Not Found", "Account for username bob not found"));
    }

    @Test
    public void user_nomisUserNoCaseload() {
        setupFindUserCallForNomis();
        final var responseEntity = userController.user("joe");
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(200);
        assertThat(responseEntity.getBody()).isEqualTo(new UserDetail("principal", false, "Joe Bloggs", AuthSource.NOMIS, 5L, null));
    }

    @Test
    public void user_nomisUser() {
        final var staffUserAccount = setupFindUserCallForNomis();
        staffUserAccount.setActiveCaseLoadId("somecase");
        final var responseEntity = userController.user("joe");
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(200);
        assertThat(responseEntity.getBody()).isEqualTo(new UserDetail("principal", false, "Joe Bloggs", AuthSource.NOMIS, 5L, "somecase"));
    }

    @Test
    public void user_authUser() {
        setupFindUserCallForAuth();
        final var responseEntity = userController.user("joe");
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(200);
        assertThat(responseEntity.getBody()).isEqualTo(new UserDetail("principal", true, "Joe Bloggs", AuthSource.AUTH, null, null));
    }

    @Test
    public void me_userNotFound() {
        final var principal = new TestingAuthenticationToken("principal", "credentials");
        assertThat(userController.me(principal)).isEqualTo(UserDetail.fromUsername("principal"));
    }

    @Test
    public void me_nomisUserNoCaseload() {
        setupFindUserCallForNomis();
        final var principal = new TestingAuthenticationToken("principal", "credentials");
        assertThat(userController.me(principal)).isEqualTo(new UserDetail("principal", false, "Joe Bloggs", AuthSource.NOMIS, 5L, null));
    }

    @Test
    public void me_nomisUser() {
        final var staffUserAccount = setupFindUserCallForNomis();
        staffUserAccount.setActiveCaseLoadId("somecase");
        final var principal = new TestingAuthenticationToken("principal", "credentials");
        assertThat(userController.me(principal)).isEqualTo(new UserDetail("principal", false, "Joe Bloggs", AuthSource.NOMIS, 5L, "somecase"));
    }

    @Test
    public void me_authUser() {
        setupFindUserCallForAuth();
        final var principal = new TestingAuthenticationToken("principal", "credentials");
        assertThat(userController.me(principal)).isEqualTo(new UserDetail("principal", true, "Joe Bloggs", AuthSource.AUTH, null, null));
    }

    @Test
    public void myRoles() {
        final var authorities = List.of(new SimpleGrantedAuthority("ROLE_BOB"), new SimpleGrantedAuthority("ROLE_JOE_FRED"));
        final var token = new UsernamePasswordAuthenticationToken("principal", "credentials", authorities);
        assertThat(userController.myRoles(token)).containsOnly(new UserRole("BOB"), new UserRole("JOE_FRED"));
    }

    @Test
    public void myRoles_noRoles() {
        final var token = new UsernamePasswordAuthenticationToken("principal", "credentials", Collections.emptyList());
        assertThat(userController.myRoles(token)).isEmpty();
    }

    @Test
    public void createUser_AlreadyExists() throws NotificationClientException {
        when(userService.findUser(anyString())).thenReturn(Optional.of(new UserDetailsImpl("name", "bob", Collections.emptySet(), null)));
        final var responseEntity = userController.createUser("user", new CreateUser("email", "first", "last"), request);

        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(409);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("username.exists", "Username user already exists"));
    }

    @Test
    public void createUser_BlankDoesntCallUserService() throws NotificationClientException, CreateUserException, VerifyEmailException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/user/newusername"));
        final var responseEntity = userController.createUser("  ", new CreateUser("email", "first", "last"), request);

        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(200);
        verify(userService, never()).findUser(anyString());
    }

    @Test
    public void createUser_Success() throws NotificationClientException, CreateUserException, VerifyEmailException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/user/newusername"));
        final var responseEntity = userController.createUser("newusername", new CreateUser("email", "first", "last"), request);

        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(200);
        assertThat(responseEntity.getBody()).isNull();
    }

    @Test
    public void createUser_CreateUserError() throws NotificationClientException, CreateUserException, VerifyEmailException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/user/newusername"));
        when(createUserService.createUser(anyString(), anyString(), anyString(), anyString(), anyString())).thenThrow(new CreateUserException("username", "errorcode"));
        final var responseEntity = userController.createUser("newusername", new CreateUser("email", "first", "last"), request);

        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(400);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("username.errorcode", "username failed validation"));
    }

    @Test
    public void createUser_VerifyUserError() throws NotificationClientException, CreateUserException, VerifyEmailException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/user/newusername"));
        when(createUserService.createUser(anyString(), anyString(), anyString(), anyString(), anyString())).thenThrow(new VerifyEmailException("reason"));
        final var responseEntity = userController.createUser("newusername", new CreateUser("email", "first", "last"), request);

        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(400);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("email.reason", "Email address failed validation"));
    }

    @Test
    public void createUser_InitialPasswordUrl() throws NotificationClientException, CreateUserException, VerifyEmailException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/auth/api/user/newusername"));

        userController.createUser("newusername", new CreateUser("email", "first", "last"), request);

        verify(createUserService).createUser("newusername", "email", "first", "last", "http://some.url/auth/initial-password-confirm?token=");
    }

    @Test
    public void createUser_() throws NotificationClientException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url/api/user/newusername"));
        final var responseEntity = userController.createUser("newusername", new CreateUser("email", "first", "last"), request);

        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(200);
        assertThat(responseEntity.getBody()).isNull();
    }

    private StaffUserAccount setupFindUserCallForNomis() {
        final var user = new StaffUserAccount();
        user.setUsername("principal");
        final var staff = new Staff();
        staff.setStaffId(5L);
        staff.setFirstName("JOE");
        staff.setLastName("bloggs");
        user.setStaff(staff);
        user.setAccountDetail(new AccountDetail());
        when(userService.findUser(anyString())).thenReturn(Optional.of(user));
        return user;
    }

    private void setupFindUserCallForAuth() {
        final var user = new UserEmail("principal", "email", true, false);
        user.setPerson(new Person());
        user.getPerson().setFirstName("Joe");
        user.getPerson().setLastName("Bloggs");
        user.setEnabled(true);
        when(userService.findUser(anyString())).thenReturn(Optional.of(user));

    }
}
