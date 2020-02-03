package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.LockedException;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.service.DelegatingUserService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ChangePasswordServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserTokenRepository userTokenRepository;
    @Mock
    private UserService userService;
    @Mock
    private DelegatingUserService delegatingUserService;

    private ChangePasswordService changePasswordService;

    @Before
    public void setUp() {
        changePasswordService = new ChangePasswordService(userTokenRepository, userRepository, userService, delegatingUserService);
    }

    @Test
    public void setPassword_AlterUser() {
        final var staffUserAccountForBob = getStaffUserAccountForBob();
        when(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(staffUserAccountForBob));
        final var user = User.of("user");
        user.setLocked(true);
        final var userToken = user.createToken(TokenType.RESET);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        changePasswordService.setPassword("bob", "pass");

        verify(delegatingUserService).changePassword(staffUserAccountForBob, "pass");
    }

    @Test
    public void setPassword_AuthUser() {
        final var user = User.builder().username("user").email("email").build();
        user.setEnabled(true);
        user.setSource(AuthSource.auth);
        final var userToken = user.createToken(TokenType.RESET);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        changePasswordService.setPassword("bob", "pass");

        verify(delegatingUserService).changePassword(user, "pass");
    }

    @Test
    public void setPassword_SaveAndDelete() {
        when(userService.findMasterUserPersonDetails(anyString())).thenReturn(getStaffUserAccountForBobOptional());
        final var user = User.of("user");
        user.setLocked(true);
        final var userToken = user.createToken(TokenType.RESET);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        changePasswordService.setPassword("bob", "pass");

        // need to ensure that the token has been removed as don't want them to be able to change password multiple times
        assertThat(user.getTokens()).isEmpty();
        verify(userRepository).save(user);
    }

    @Test
    public void setPassword_AuthUserPasswordSet() {
        final var user = User.builder().username("user").build();
        user.setEnabled(true);
        user.setSource(AuthSource.auth);
        final var userToken = user.createToken(TokenType.RESET);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        changePasswordService.setPassword("bob", "pass");

        verify(delegatingUserService).changePassword(user, "pass");
    }

    @Test
    public void setPassword_LockedAccount() {
        final var staffUserAccount = getStaffUserAccountForBobOptional();
        staffUserAccount.map(NomisUserPersonDetails.class::cast).get().getAccountDetail().setAccountStatus("LOCKED");
        when(userService.findMasterUserPersonDetails(anyString())).thenReturn(staffUserAccount);

        final var user = User.of("user");
        final var userToken = user.createToken(TokenType.RESET);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> changePasswordService.setPassword("bob", "pass")).isInstanceOf(LockedException.class);
    }

    @Test
    public void setPassword_DisabledAccount() {
        final var optionalUser = buildAuthUser();
        optionalUser.map(User.class::cast).get().setEnabled(false);
        when(userService.findMasterUserPersonDetails(anyString())).thenReturn(optionalUser);

        final var user = User.of("user");
        final var userToken = user.createToken(TokenType.RESET);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> changePasswordService.setPassword("bob", "pass")).isInstanceOf(LockedException.class);
    }

    @Test
    public void setPassword_LockedAuthAccount() {
        final var user = User.builder().username("user").locked(true).build();
        user.setEnabled(true);
        user.setSource(AuthSource.auth);
        final var userToken = user.createToken(TokenType.RESET);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> changePasswordService.setPassword("bob", "pass")).isInstanceOf(LockedException.class);
    }

    private Optional<UserPersonDetails> buildAuthUser() {
        final var user = User.builder().username("user").email("email").verified(true).build();
        user.setPerson(new Person("first", "last"));
        user.setEnabled(true);
        return Optional.of(user);
    }

    private UserPersonDetails getStaffUserAccountForBob() {
        final var staffUserAccount = new NomisUserPersonDetails();
        final var staff = new Staff();
        staff.setFirstName("bOb");
        staff.setStatus("ACTIVE");
        staffUserAccount.setStaff(staff);
        final var detail = new AccountDetail("user", "OPEN", "profile", null);
        staffUserAccount.setAccountDetail(detail);
        staffUserAccount.setUsername("bob");
        return staffUserAccount;
    }

    private Optional<UserPersonDetails> getStaffUserAccountForBobOptional() {
        return Optional.of(getStaffUserAccountForBob());
    }
}
