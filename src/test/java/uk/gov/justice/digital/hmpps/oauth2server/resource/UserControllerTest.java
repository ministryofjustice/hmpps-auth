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
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;
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
        assertThat(responseEntity.getBody()).containsOnly(entry("status", 404), entry("userMessage", "Account for username bob not found"));
    }

    @Test
    public void user_nomisUserNoCaseload() {
        setupFindUserCallForNomis();
        final var responseEntity = userController.user("joe");
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(200);
        assertThat(responseEntity.getBody()).containsOnly(entry("username", "principal"),
                entry("active", Boolean.FALSE), entry("staffId", 5L), entry("name", "Joe Bloggs"),
                entry("authSource", "nomis"));
    }

    @Test
    public void user_nomisUser() {
        final var staffUserAccount = setupFindUserCallForNomis();
        staffUserAccount.setActiveCaseLoadId("somecase");
        final var responseEntity = userController.user("joe");
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(200);
        assertThat(responseEntity.getBody()).containsOnly(entry("username", "principal"),
                entry("active", Boolean.FALSE), entry("staffId", 5L), entry("name", "Joe Bloggs"),
                entry("authSource", "nomis"), entry("activeCaseLoadId", "somecase"));
    }

    @Test
    public void user_authUser() {
        setupFindUserCallForAuth();
        final var responseEntity = userController.user("joe");
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(200);
        assertThat(responseEntity.getBody()).containsOnly(entry("username", "principal"),
                entry("active", Boolean.TRUE), entry("name", "Joe Bloggs"),
                entry("authSource", "auth"));
    }

    @Test
    public void me_userNotFound() {
        final var principal = new TestingAuthenticationToken("principal", "credentials");
        assertThat(userController.me(principal)).containsOnly(entry("username", "principal"));
    }

    @Test
    public void me_nomisUserNoCaseload() {
        setupFindUserCallForNomis();
        final var principal = new TestingAuthenticationToken("principal", "credentials");
        assertThat(userController.me(principal)).containsOnly(entry("username", "principal"),
                entry("active", Boolean.FALSE), entry("staffId", 5L), entry("name", "Joe Bloggs"),
                entry("authSource", "nomis"));
    }

    @Test
    public void me_nomisUser() {
        final var staffUserAccount = setupFindUserCallForNomis();
        staffUserAccount.setActiveCaseLoadId("somecase");
        final var principal = new TestingAuthenticationToken("principal", "credentials");
        assertThat(userController.me(principal)).containsOnly(entry("username", "principal"),
                entry("active", Boolean.FALSE), entry("staffId", 5L), entry("name", "Joe Bloggs"),
                entry("authSource", "nomis"), entry("activeCaseLoadId", "somecase"));
    }

    @Test
    public void me_authUser() {
        setupFindUserCallForAuth();
        final var principal = new TestingAuthenticationToken("principal", "credentials");
        assertThat(userController.me(principal)).containsOnly(entry("username", "principal"),
                entry("active", Boolean.TRUE), entry("name", "Joe Bloggs"),
                entry("authSource", "auth"));
    }

    @Test
    public void myRoles() {
        final var authorities = List.of(new SimpleGrantedAuthority("ROLE_BOB"), new SimpleGrantedAuthority("ROLE_JOE_FRED"));
        final var token = new UsernamePasswordAuthenticationToken("principal", "credentials", authorities);
        assertThat(userController.myRoles(token)).containsOnly(Map.of("roleCode", "BOB"), Map.of("roleCode", "JOE_FRED"));
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
        final var user = new UserEmail("principal", "email", true, false);
        user.setPerson(new Person());
        user.getPerson().setFirstName("Joe");
        user.getPerson().setLastName("Bloggs");
        user.setEnabled(true);
        when(userService.findUser(anyString())).thenReturn(Optional.of(user));

    }
}
