package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ChangePasswordServiceTest {
    @Mock
    private UserEmailRepository userEmailRepository;
    @Mock
    private UserTokenRepository userTokenRepository;
    @Mock
    private UserService userService;
    @Mock
    private AlterUserService alterUserService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TelemetryClient telemetryClient;

    private ChangePasswordService changePasswordService;

    @Before
    public void setUp() {
        changePasswordService = new ChangePasswordService(userTokenRepository, userEmailRepository, userService, alterUserService, passwordEncoder, telemetryClient, 10);
    }

    @Test
    public void createToken() {
    }

    @Test
    public void setPassword_AlterUser() {
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());
        final var user = new UserEmail("uesr");
        user.setLocked(true);
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        changePasswordService.setPassword("bob", "pass");

        verify(alterUserService).changePassword("uesr", "pass");
    }

    @Test
    public void setPassword_AuthUser() {
        final var user = new UserEmail("uesr", null, false, false);
        user.setEnabled(true);
        user.setMaster(true);
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        changePasswordService.setPassword("bob", "pass");

        verify(alterUserService, never()).changePassword(anyString(), anyString());
    }

    @Test
    public void setPassword_SaveAndDelete() {
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());
        final var user = new UserEmail("uesr");
        user.setLocked(true);
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        changePasswordService.setPassword("bob", "pass");

        // need to ensure that the token has been removed as don't want them to be able to change password multiple times
        verify(userTokenRepository).delete(userToken);
        verify(userEmailRepository).save(user);
    }

    @Test
    public void setPassword_AuthUserPasswordSet() {
        final var user = new UserEmail("uesr", null, false, false);
        user.setEnabled(true);
        user.setMaster(true);
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedpassword");
        changePasswordService.setPassword("bob", "pass");

        assertThat(user.getPassword()).isEqualTo("hashedpassword");
        assertThat(user.getPasswordExpiry()).isAfterOrEqualTo(LocalDateTime.now().plusDays(9));
        verify(passwordEncoder).encode("pass");
    }

    @Test
    public void setPassword_NotFound() {
        when(userService.findUser(anyString())).thenReturn(Optional.empty());

        final var user = new UserEmail("uesr");
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> changePasswordService.setPassword("bob", "pass")).isInstanceOf(LockedException.class);
    }

    @Test
    public void setPassword_LockedAccount() {
        final var staffUserAccount = getStaffUserAccountForBob();
        staffUserAccount.map(StaffUserAccount.class::cast).get().getAccountDetail().setAccountStatus("LOCKED");
        when(userService.findUser(anyString())).thenReturn(staffUserAccount);

        final var user = new UserEmail("uesr");
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> changePasswordService.setPassword("bob", "pass")).isInstanceOf(LockedException.class);
    }

    @Test
    public void setPassword_DisabledAccount() {
        final var userEmail = buildAuthUser();
        userEmail.map(UserEmail.class::cast).get().setEnabled(false);
        when(userService.findUser(anyString())).thenReturn(userEmail);

        final var user = new UserEmail("uesr");
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> changePasswordService.setPassword("bob", "pass")).isInstanceOf(LockedException.class);
    }

    @Test
    public void setPassword_LockedAuthAccount() {
        final var user = new UserEmail("uesr", null, false, true);
        user.setEnabled(true);
        user.setMaster(true);
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> changePasswordService.setPassword("bob", "pass")).isInstanceOf(LockedException.class);
    }

    @Test
    public void setPassword_AuthPasswordSameAsCurrent() {
        final var user = new UserEmail("uesr", null, false, false);
        user.setEnabled(true);
        user.setMaster(true);
        user.setPassword("oldencryptedpassword");
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(Boolean.TRUE);
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> changePasswordService.setPassword("bob", "pass")).isInstanceOf(ReusedPasswordException.class);

        verify(passwordEncoder).matches("pass", "oldencryptedpassword");
    }

    private Optional<UserPersonDetails> buildAuthUser() {
        final var userEmail = new UserEmail("user", "email", true, false);
        userEmail.setPerson(new Person("user", "first", "last"));
        userEmail.setEnabled(true);
        return Optional.of(userEmail);
    }

    private Optional<UserPersonDetails> getStaffUserAccountForBob() {
        final var staffUserAccount = new StaffUserAccount();
        final var staff = new Staff();
        staff.setFirstName("bOb");
        staff.setStatus("ACTIVE");
        staffUserAccount.setStaff(staff);
        final var detail = new AccountDetail("user", "OPEN", "profile", null);
        staffUserAccount.setAccountDetail(detail);
        return Optional.of(staffUserAccount);
    }
}
