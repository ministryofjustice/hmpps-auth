package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.*;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus.*;


@RunWith(MockitoJUnitRunner.class)
public class UserDetailsServiceImplTest {

    private UserDetailsServiceImpl service;

    @Mock
    private UserService userService;
    @Mock
    private EntityManager nomisEntityManager;
    @Mock
    private EntityManager authEntityManager;

    private static final Caseload NWEB_CASELOAD = Caseload.builder().id("NWEB").type("APP").build();
    private static final Caseload MDI_CASELOAD = Caseload.builder().id("MDI").type("INST").build();
    private static final long ROLE_ID = 1L;

    @Before
    public void setup() {
        service = new UserDetailsServiceImpl(userService);
        ReflectionTestUtils.setField(service, "nomisEntityManager", nomisEntityManager);
        ReflectionTestUtils.setField(service, "authEntityManager", authEntityManager);
    }

    @Test
    public void testHappyUserPath() {

        final var user = buildStandardUser("ITAG_USER");
        when(userService.findUser(user.getUsername())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonExpired()).isTrue();
        assertThat(itagUser.isAccountNonLocked()).isTrue();
        assertThat(itagUser.isCredentialsNonExpired()).isTrue();
        assertThat(itagUser.isEnabled()).isTrue();

        assertThat(((UserPersonDetails) itagUser).getName()).isEqualTo("Itag User");
    }

    @Test
    public void testEntityDetached() {

        final var user = buildStandardUser("ITAG_USER");
        when(userService.findUser(user.getUsername())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        verify(nomisEntityManager).detach(user);

        assertThat(((UserPersonDetails) itagUser).getName()).isEqualTo("Itag User");
    }

    @Test
    public void testAuthEntityDetached() {

        final var user = buildAuthUser();
        when(userService.findUser(user.getUsername())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        verify(authEntityManager).detach(user);

        assertThat(((UserPersonDetails) itagUser).getName()).isEqualTo("first last");
    }

    @Test
    public void testLockedUser() {

        final var user = buildLockedUser();
        when(userService.findUser(user.getUsername())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonExpired()).isTrue();
        assertThat(itagUser.isAccountNonLocked()).isFalse();
        assertThat(itagUser.isCredentialsNonExpired()).isTrue();
        assertThat(itagUser.isEnabled()).isFalse();
    }

    @Test
    public void testExpiredUser() {

        final var user = buildExpiredUser();
        when(userService.findUser(user.getUsername())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonExpired()).isTrue();
        assertThat(itagUser.isAccountNonLocked()).isTrue();
        assertThat(itagUser.isCredentialsNonExpired()).isFalse();
        assertThat(itagUser.isEnabled()).isTrue();
    }

    @Test
    public void testAuthOnlyUser() {

        final var user = buildAuthUser();
        when(userService.findUser(anyString())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonExpired()).isTrue();
        assertThat(itagUser.isAccountNonLocked()).isTrue();
        assertThat(itagUser.isCredentialsNonExpired()).isTrue();
        assertThat(itagUser.isEnabled()).isTrue();
    }

    @Test
    public void testUserNotFound() {

        when(userService.findUser(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("user")).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    public void testExpiredGraceUser() {

        final var user = buildExpiredGraceUser();
        when(userService.findUser(user.getUsername())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonExpired()).isTrue();
        assertThat(itagUser.isAccountNonLocked()).isTrue();
        assertThat(itagUser.isCredentialsNonExpired()).isTrue();
        assertThat(itagUser.isEnabled()).isTrue();
    }

    @Test
    public void testExpiredLockedUser() {

        final var user = buildExpiredLockedUser();
        when(userService.findUser(user.getUsername())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonLocked()).isFalse();
        assertThat(itagUser.isCredentialsNonExpired()).isFalse();
        assertThat(itagUser.isEnabled()).isFalse();
    }

    @Test
    public void testLockedTimedUser() {

        final var user = buildLockedTimedUser();
        when(userService.findUser(user.getUsername())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isEnabled()).isFalse();
        assertThat(itagUser.isAccountNonExpired()).isTrue();
        assertThat(itagUser.isAccountNonLocked()).isFalse();
        assertThat(itagUser.isCredentialsNonExpired()).isTrue();
    }

    private StaffUserAccount buildStandardUser(final String username) {
        final var staff = buildStaff();

        final var userAccount = StaffUserAccount.builder()
                .username(username)
                .password("pass")
                .type("GENERAL")
                .caseloads(List.of(
                        buildUserAccessibleCaseload("NWEB", NWEB_CASELOAD, username),
                        buildUserAccessibleCaseload("MDI", MDI_CASELOAD, username)))
                .staff(staff)
                .roles(List.of(UserCaseloadRole.builder()
                        .id(UserCaseloadRoleIdentity.builder().caseload("NWEB").roleId(ROLE_ID).username(username).build())
                        .role(Role.builder().code("ROLE1").id(ROLE_ID).function("General").name("A Role").sequence(1).build())
                        .caseload(NWEB_CASELOAD)
                        .build()))
                .accountDetail(buildAccountDetail(username, OPEN))
                .build();

        userAccount.getRoles().forEach(r -> r.setUser(userAccount));
        userAccount.getCaseloads().forEach(c -> c.setUser(userAccount));
        return userAccount;
    }

    private StaffUserAccount buildExpiredUser() {
        final var userAccount = buildStandardUser("EXPIRED_USER");
        userAccount.setAccountDetail(buildAccountDetail("EXPIRED_USER", EXPIRED));
        return userAccount;
    }

    private StaffUserAccount buildLockedUser() {
        final var userAccount = buildStandardUser("LOCKED_USER");
        userAccount.setAccountDetail(buildAccountDetail("LOCKED_USER", LOCKED));
        return userAccount;
    }

    private StaffUserAccount buildExpiredLockedUser() {
        final var userAccount = buildStandardUser("EXPIRED_USER");
        userAccount.setAccountDetail(buildAccountDetail("EXPIRED_USER", EXPIRED_LOCKED));
        return userAccount;
    }

    private StaffUserAccount buildLockedTimedUser() {
        final var userAccount = buildStandardUser("LOCKED_USER");
        userAccount.setAccountDetail(buildAccountDetail("LOCKED_USER", LOCKED_TIMED));
        return userAccount;
    }

    private StaffUserAccount buildExpiredGraceUser() {
        final var userAccount = buildStandardUser("EXPIRED_USER");
        userAccount.setAccountDetail(buildAccountDetail("EXPIRED_USER", EXPIRED_GRACE));
        return userAccount;
    }

    private AccountDetail buildAccountDetail(final String username, final AccountStatus status) {
        return AccountDetail.builder()
                .username(username)
                .accountStatus(status.getDesc())
                .profile("TAG_GENERAL")
                .build();
    }

    private UserAccessibleCaseload buildUserAccessibleCaseload(final String caseloadId, final Caseload caseload, final String username) {
        return UserAccessibleCaseload.builder()
                .id(UserCaseloadIdentity.builder()
                        .username(username)
                        .caseload(caseloadId)
                        .build())
                .caseload(caseload)
                .startDate(LocalDate.now()).build();
    }

    private Staff buildStaff() {
        return Staff.builder()
                .staffId(1L)
                .firstName("ITAG")
                .lastName("USER")
                .status("ACTIVE")
                .build();
    }

    private UserEmail buildAuthUser() {
        final var userEmail = new UserEmail("user", "email", true, false);
        userEmail.setPerson(new Person("user", "first", "last"));
        userEmail.setEnabled(true);
        userEmail.setPasswordExpiry(LocalDateTime.now().plusDays(1));
        return userEmail;
    }
}
