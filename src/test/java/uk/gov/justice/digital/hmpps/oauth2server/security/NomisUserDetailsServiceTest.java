package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.*;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus.*;


@ExtendWith(SpringExtension.class)
class NomisUserDetailsServiceTest {
    private static final long ROLE_ID = 1L;

    @Mock
    private NomisUserService userService;
    @Mock
    private EntityManager nomisEntityManager;

    private NomisUserDetailsService service;

    @BeforeEach
    void setup() {
        service = new NomisUserDetailsService(userService);
        ReflectionTestUtils.setField(service, "nomisEntityManager", nomisEntityManager);
    }

    @Test
    void testHappyUserPath() {

        final var user = buildStandardUser("ITAG_USER");
        when(userService.getNomisUserByUsername(user.getUsername())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonExpired()).isTrue();
        assertThat(itagUser.isAccountNonLocked()).isTrue();
        assertThat(itagUser.isCredentialsNonExpired()).isTrue();
        assertThat(itagUser.isEnabled()).isTrue();

        assertThat(((UserPersonDetails) itagUser).getName()).isEqualTo("Itag User");
    }

    @Test
    void testEntityDetached() {

        final var user = buildStandardUser("ITAG_USER");
        when(userService.getNomisUserByUsername(user.getUsername())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        verify(nomisEntityManager).detach(user);

        assertThat(((UserPersonDetails) itagUser).getName()).isEqualTo("Itag User");
    }

    @Test
    void testLockedUser() {

        final var user = buildLockedUser();
        when(userService.getNomisUserByUsername(user.getUsername())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonExpired()).isTrue();
        assertThat(itagUser.isAccountNonLocked()).isFalse();
        assertThat(itagUser.isCredentialsNonExpired()).isTrue();
        assertThat(itagUser.isEnabled()).isFalse();
    }

    @Test
    void testExpiredUser() {

        final var user = buildExpiredUser();
        when(userService.getNomisUserByUsername(user.getUsername())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonExpired()).isTrue();
        assertThat(itagUser.isAccountNonLocked()).isTrue();
        assertThat(itagUser.isCredentialsNonExpired()).isFalse();
        assertThat(itagUser.isEnabled()).isTrue();
    }

    @Test
    void testUserNotFound() {

        when(userService.getNomisUserByUsername(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("user")).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void testExpiredGraceUser() {

        final var user = buildExpiredGraceUser();
        when(userService.getNomisUserByUsername(user.getUsername())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonExpired()).isTrue();
        assertThat(itagUser.isAccountNonLocked()).isTrue();
        assertThat(itagUser.isCredentialsNonExpired()).isTrue();
        assertThat(itagUser.isEnabled()).isTrue();
    }

    @Test
    void testExpiredLockedUser() {

        final var user = buildExpiredLockedUser();
        when(userService.getNomisUserByUsername(user.getUsername())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonLocked()).isFalse();
        assertThat(itagUser.isCredentialsNonExpired()).isFalse();
        assertThat(itagUser.isEnabled()).isFalse();
    }

    @Test
    void testLockedTimedUser() {

        final var user = buildLockedTimedUser();
        when(userService.getNomisUserByUsername(user.getUsername())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isEnabled()).isFalse();
        assertThat(itagUser.isAccountNonExpired()).isTrue();
        assertThat(itagUser.isAccountNonLocked()).isFalse();
        assertThat(itagUser.isCredentialsNonExpired()).isTrue();
    }

    private NomisUserPersonDetails buildStandardUser(final String username) {
        final var staff = buildStaff();

        final var userAccount = NomisUserPersonDetails.builder()
                .username(username)
                .password("pass")
                .type("GENERAL")
                .staff(staff)
                .roles(List.of(UserCaseloadRole.builder()
                        .id(UserCaseloadRoleIdentity.builder().caseload("NWEB").roleId(ROLE_ID).username(username).build())
                        .role(Role.builder().code("ROLE1").id(ROLE_ID).build())
                        .build()))
                .accountDetail(buildAccountDetail(username, OPEN))
                .build();

        return userAccount;
    }

    private NomisUserPersonDetails buildExpiredUser() {
        final var userAccount = buildStandardUser("EXPIRED_USER");
        userAccount.setAccountDetail(buildAccountDetail("EXPIRED_USER", EXPIRED));
        return userAccount;
    }

    private NomisUserPersonDetails buildLockedUser() {
        final var userAccount = buildStandardUser("LOCKED_USER");
        userAccount.setAccountDetail(buildAccountDetail("LOCKED_USER", LOCKED));
        return userAccount;
    }

    private NomisUserPersonDetails buildExpiredLockedUser() {
        final var userAccount = buildStandardUser("EXPIRED_USER");
        userAccount.setAccountDetail(buildAccountDetail("EXPIRED_USER", EXPIRED_LOCKED));
        return userAccount;
    }

    private NomisUserPersonDetails buildLockedTimedUser() {
        final var userAccount = buildStandardUser("LOCKED_USER");
        userAccount.setAccountDetail(buildAccountDetail("LOCKED_USER", LOCKED_TIMED));
        return userAccount;
    }

    private NomisUserPersonDetails buildExpiredGraceUser() {
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

    private Staff buildStaff() {
        return Staff.builder()
                .staffId(1L)
                .firstName("ITAG")
                .lastName("USER")
                .status("ACTIVE")
                .build();
    }
}
