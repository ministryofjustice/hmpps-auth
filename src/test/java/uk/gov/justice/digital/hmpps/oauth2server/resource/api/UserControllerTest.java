package uk.gov.justice.digital.hmpps.oauth2server.resource.api;

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
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail;
import uk.gov.justice.digital.hmpps.oauth2server.model.UserDetail;
import uk.gov.justice.digital.hmpps.oauth2server.model.UserRole;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserControllerTest {
    @Mock
    private UserService userService;

    private UserController userController;

    @Before
    public void setUp() {
        userController = new UserController(userService);
    }

    @Test
    public void user_userNotFound() {
        final var responseEntity = userController.user("bob");
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(404);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDetail("Not Found", "Account for username bob not found", "username"));
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
        final var user = UserEmail.builder().username("principal").email("email").verified(true).build();
        user.setPerson(new Person());
        user.getPerson().setFirstName("Joe");
        user.getPerson().setLastName("Bloggs");
        user.setEnabled(true);
        when(userService.findUser(anyString())).thenReturn(Optional.of(user));

    }
}
