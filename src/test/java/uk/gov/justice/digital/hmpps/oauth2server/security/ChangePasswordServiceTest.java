package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.LockedException;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private TelemetryClient telemetryClient;

    private ChangePasswordService changePasswordService;

    @Before
    public void setUp() {
        changePasswordService = new ChangePasswordService(userTokenRepository, userEmailRepository, userService, alterUserService, telemetryClient);
    }

    @Test
    public void createToken() {
    }

    @Test
    public void setPassword_UserUnlocked() {
        when(userService.getUserByUsername(anyString())).thenReturn(getStaffUserAccountForBob());
        final var user = new UserEmail("uesr");
        user.setLocked(true);
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        changePasswordService.setPassword("bob", "pass");

        assertThat(user.isLocked()).isFalse();
    }

    @Test
    public void setPassword_AlterUser() {
        when(userService.getUserByUsername(anyString())).thenReturn(getStaffUserAccountForBob());
        final var user = new UserEmail("uesr");
        user.setLocked(true);
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        changePasswordService.setPassword("bob", "pass");

        verify(alterUserService).changePassword("uesr", "pass");
    }

    @Test
    public void setPassword_SaveAndDelete() {
        when(userService.getUserByUsername(anyString())).thenReturn(getStaffUserAccountForBob());
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
    public void setPassword_NotFound() {
        when(userService.getUserByUsername(anyString())).thenReturn(Optional.empty());

        final var user = new UserEmail("uesr");
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> changePasswordService.setPassword("bob", "pass")).isInstanceOf(LockedException.class);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void setPassword_LockedAccount() {
        final var staffUserAccount = getStaffUserAccountForBob();
        staffUserAccount.get().getAccountDetail().setAccountStatus("LOCKED");
        when(userService.getUserByUsername(anyString())).thenReturn(staffUserAccount);

        final var user = new UserEmail("uesr");
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> changePasswordService.setPassword("bob", "pass")).isInstanceOf(LockedException.class);
    }

    private Optional<StaffUserAccount> getStaffUserAccountForBob() {
        final var staffUserAccount = new StaffUserAccount();
        final var staff = new Staff();
        staff.setFirstName("bOb");
        staff.setStatus("ACTIVE");
        staffUserAccount.setStaff(staff);
        final var detail = new AccountDetail("user", "OPEN", "profile");
        staffUserAccount.setAccountDetail(detail);
        return Optional.of(staffUserAccount);
    }
}
