package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.userdetails.UserDetails;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus.*;


@RunWith(MockitoJUnitRunner.class)
public class UserDetailsServiceImplTest {

    private UserDetailsServiceImpl service;

    @Mock
    private UserService userService;

    private static final Caseload NWEB_CASELOAD = Caseload.builder().id("NWEB").type("APP").build();
    private static final Caseload MDI_CASELOAD = Caseload.builder().id("MDI").type("INST").build();
    private static final long ROLE_ID = 1L;

    @Before
    public void setup() {
        service = new UserDetailsServiceImpl(userService, "NWEB");
    }

    @Test
    public void testHappyUserPath() {

        StaffUserAccount user = buildStandardUser("ITAG_USER");
        when(userService.getUserByUsername(user.getUsername())).thenReturn(Optional.of(user));

        UserDetails itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonExpired()).isTrue();
        assertThat(itagUser.isAccountNonLocked()).isTrue();
        assertThat(itagUser.isCredentialsNonExpired()).isTrue();
        assertThat(itagUser.isEnabled()).isTrue();
    }

    @Test
    public void testLockedUser() {

        StaffUserAccount user = buildLockedUser("LOCKED_USER");
        when(userService.getUserByUsername(user.getUsername())).thenReturn(Optional.of(user));

        UserDetails itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonExpired()).isTrue();
        assertThat(itagUser.isAccountNonLocked()).isFalse();
        assertThat(itagUser.isCredentialsNonExpired()).isTrue();
        assertThat(itagUser.isEnabled()).isFalse();
    }

    @Test
    public void testExpiredUser() {

        StaffUserAccount user = buildExpiredUser("EXPIRED_USER");
        when(userService.getUserByUsername(user.getUsername())).thenReturn(Optional.of(user));

        UserDetails itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonExpired()).isFalse();
        assertThat(itagUser.isAccountNonLocked()).isTrue();
        assertThat(itagUser.isCredentialsNonExpired()).isTrue();
        assertThat(itagUser.isEnabled()).isFalse();
    }

    @Test
    public void testExpiredGraceUser() {

        StaffUserAccount user = buildExpiredGraceUser("EXPIRED_USER");
        when(userService.getUserByUsername(user.getUsername())).thenReturn(Optional.of(user));

        UserDetails itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonExpired()).isTrue();
        assertThat(itagUser.isAccountNonLocked()).isFalse();
        assertThat(itagUser.isCredentialsNonExpired()).isTrue();
        assertThat(itagUser.isEnabled()).isTrue();
    }

    @Test
    public void testExpiredLockedUser() {

        StaffUserAccount user = buildExpiredLockedUser("EXPIRED_USER");
        when(userService.getUserByUsername(user.getUsername())).thenReturn(Optional.of(user));

        UserDetails itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonExpired()).isFalse();
        assertThat(itagUser.isAccountNonLocked()).isFalse();
        assertThat(itagUser.isCredentialsNonExpired()).isTrue();
        assertThat(itagUser.isEnabled()).isFalse();
    }

    @Test
    public void testLockedTimedUser() {

        StaffUserAccount user = buildLockedTimedUser("LOCKED_USER");
        when(userService.getUserByUsername(user.getUsername())).thenReturn(Optional.of(user));

        UserDetails itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isEnabled()).isFalse();
        assertThat(itagUser.isAccountNonExpired()).isTrue();
        assertThat(itagUser.isAccountNonLocked()).isFalse();
        assertThat(itagUser.isCredentialsNonExpired()).isTrue();
    }

    private StaffUserAccount buildStandardUser(String username) {
        Staff staff = buildStaff();

        StaffUserAccount userAccount = StaffUserAccount.builder()
                .username(username)
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
                .accountDetail(buildAccountDetail(username, OPEN, false, false))
                .build();

        userAccount.getRoles().forEach(r -> r.setUser(userAccount));
        userAccount.getCaseloads().forEach(c -> c.setUser(userAccount));
        return userAccount;
    }

    private StaffUserAccount buildExpiredUser(String username) {
        StaffUserAccount userAccount = buildStandardUser(username);
        userAccount.setAccountDetail(buildAccountDetail(username, EXPIRED, true, false));
        return userAccount;
    }

    private StaffUserAccount buildLockedUser(String username) {
        StaffUserAccount userAccount = buildStandardUser(username);
        userAccount.setAccountDetail(buildAccountDetail(username, LOCKED, false, true));
        return userAccount;
    }

    private StaffUserAccount buildExpiredLockedUser(String username) {
        StaffUserAccount userAccount = buildStandardUser(username);
        userAccount.setAccountDetail(buildAccountDetail(username, EXPIRED_LOCKED, true, true));
        return userAccount;
    }

    private StaffUserAccount buildLockedTimedUser(String username) {
        StaffUserAccount userAccount = buildStandardUser(username);
        userAccount.setAccountDetail(buildAccountDetail(username, LOCKED_TIMED, false, true));
        return userAccount;
    }

    private StaffUserAccount buildExpiredGraceUser(String username) {
        StaffUserAccount userAccount = buildStandardUser(username);
        userAccount.setAccountDetail(buildAccountDetail(username, EXPIRED_GRACE, true, true));
        return userAccount;
    }

    private AccountDetail buildAccountDetail(String username, AccountStatus status, boolean expired, boolean locked) {
        return AccountDetail.builder()
                .username(username)
                .expired(expired)
                .locked(locked)
                .loggedIn(false)
                .accountStatus(status.getCode())
                .createdDate(LocalDateTime.now())
                .expiryDate(LocalDateTime.now().plusDays(90))
                .build();
    }

    private UserAccessibleCaseload buildUserAccessibleCaseload(String caseloadId, Caseload caseload, String username) {
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
}
