package uk.gov.justice.digital.hmpps.oauth2server.verify;

import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.security.AlterUserService;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ResetPasswordServiceTest {
    @Mock
    private UserEmailRepository userEmailRepository;
    @Mock
    private UserTokenRepository userTokenRepository;
    @Mock
    private UserService userService;
    @Mock
    private AlterUserService alterUserService;
    @Mock
    private NotificationClientApi notificationClient;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    private ResetPasswordService resetPasswordService;

    @Before
    public void setUp() {
        resetPasswordService = new ResetPasswordService(userEmailRepository, userTokenRepository, userService, alterUserService, notificationClient, "resetTemplate", "resetUnavailableTemplate", passwordEncoder, 10);
    }

    @Test
    public void requestResetPassword_noUserEmail() throws NotificationClientException {
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.empty());
        final var optional = resetPasswordService.requestResetPassword("user", "url");
        assertThat(optional).isEmpty();
    }

    @Test
    public void requestResetPassword_noEmail() throws NotificationClientException {
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(new UserEmail("user")));
        final var optional = resetPasswordService.requestResetPassword("user", "url");
        assertThat(optional).isEmpty();
    }

    @Test
    public void requestResetPassword_noNomisUser() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", true, false);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        when(userService.findUser(anyString())).thenReturn(Optional.empty());

        final var optional = resetPasswordService.requestResetPassword("user", "url");
        verify(notificationClient).sendEmail(eq("resetUnavailableTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optional).isEmpty();
        assertThat(mapCaptor.getValue()).containsExactly(MapEntry.entry("firstName", "USER"));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void requestResetPassword_inactive() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", true, false);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        final var staffUserAccount = getStaffUserAccountForBob();
        ((StaffUserAccount) staffUserAccount.get()).getStaff().setStatus("inactive");
        when(userService.findUser(anyString())).thenReturn(staffUserAccount);

        final var optional = resetPasswordService.requestResetPassword("user", "url");
        verify(notificationClient).sendEmail(eq("resetUnavailableTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optional).isEmpty();
        assertThat(mapCaptor.getValue()).containsExactly(MapEntry.entry("firstName", "Bob"));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void requestResetPassword_authLocked() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", true, true);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        final var accountOptional = getStaffUserAccountForBob();
        ((StaffUserAccount) accountOptional.get()).getAccountDetail().setAccountStatus("LOCKED");
        when(userService.findUser(anyString())).thenReturn(accountOptional);

        final var optionalLink = resetPasswordService.requestResetPassword("user", "url");
        verify(notificationClient).sendEmail(eq("resetTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optionalLink).isPresent();
        assertThat(mapCaptor.getValue()).containsOnly(MapEntry.entry("firstName", "Bob"), MapEntry.entry("resetLink", optionalLink.get()));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void requestResetPassword_notAuthLocked() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", true, false);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        final var accountOptional = getStaffUserAccountForBob();
        ((StaffUserAccount) accountOptional.get()).getAccountDetail().setAccountStatus("LOCKED");
        when(userService.findUser(anyString())).thenReturn(accountOptional);

        final var optional = resetPasswordService.requestResetPassword("user", "url");
        verify(notificationClient).sendEmail(eq("resetUnavailableTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optional).isEmpty();
        assertThat(mapCaptor.getValue()).containsExactly(MapEntry.entry("firstName", "Bob"));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void requestResetPassword_userLocked() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", true, false);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        final var accountOptional = getStaffUserAccountForBob();
        ((StaffUserAccount) accountOptional.get()).getAccountDetail().setAccountStatus("EXPIRED & LOCKED(TIMED)");
        when(userService.findUser(anyString())).thenReturn(accountOptional);

        final var optionalLink = resetPasswordService.requestResetPassword("user", "url");
        verify(notificationClient).sendEmail(eq("resetTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optionalLink).isPresent();
        assertThat(mapCaptor.getValue()).containsOnly(MapEntry.entry("firstName", "Bob"), MapEntry.entry("resetLink", optionalLink.get()));
    }

    @Test
    public void requestResetPassword_existingToken() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", true, false);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());
        final var userToken = new UserToken(TokenType.RESET, userEmail);
        when(userTokenRepository.findByTokenTypeAndUserEmail(any(), any())).thenReturn(Optional.of(userToken));

        resetPasswordService.requestResetPassword("user", "url");
        verify(userTokenRepository).delete(userToken);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void requestResetPassword_verifyToken() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", false, true);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());

        final var optionalLink = resetPasswordService.requestResetPassword("user", "url");
        verify(notificationClient).sendEmail(eq("resetTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optionalLink).isPresent();
        assertThat(mapCaptor.getValue()).containsOnly(MapEntry.entry("firstName", "Bob"), MapEntry.entry("resetLink", optionalLink.get()));
    }

    @Test
    public void requestResetPassword_uppercaseUsername() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", false, true);
        when(userEmailRepository.findById(any())).thenReturn(Optional.of(userEmail));
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());

        resetPasswordService.requestResetPassword("someuser", "url");
        verify(userEmailRepository).findById("SOMEUSER");
        verify(userService).findUser("SOMEUSER");
    }

    @Test
    public void requestResetPassword_verifyNotification() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", false, true);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());
        final var captor = ArgumentCaptor.forClass(UserToken.class);

        final var linkOptional = resetPasswordService.requestResetPassword("user", "url");
        verify(userTokenRepository).save(captor.capture());
        final var value = captor.getValue();
        assertThat(linkOptional).get().isEqualTo("url" + value.getToken());
        assertThat(value.getTokenType()).isEqualTo(TokenType.RESET);
        assertThat(value.getUserEmail().getEmail()).isEqualTo("email");
    }

    @Test
    public void requestResetPassword_sendFailure() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", false, true);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());

        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), isNull())).thenThrow(new NotificationClientException("message"));

        assertThatThrownBy(() -> resetPasswordService.requestResetPassword("user", "url")).hasMessage("message");
    }

    private Optional<UserPersonDetails> getStaffUserAccountForBob() {
        final var staffUserAccount = new StaffUserAccount();
        final var staff = new Staff();
        staff.setFirstName("bOb");
        staff.setStatus("ACTIVE");
        staffUserAccount.setStaff(staff);
        final var detail = new AccountDetail("user", "OPEN", "profile");
        staffUserAccount.setAccountDetail(detail);
        return Optional.of(staffUserAccount);
    }

    @Test
    public void resetPassword() {
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());
        final var user = new UserEmail("uesr");
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.setPassword("bob", "pass");

        verify(userTokenRepository).delete(userToken);
        verify(userEmailRepository).save(user);
        verify(alterUserService).changePasswordWithUnlock("uesr", "pass");
    }

    @Test
    public void resetPassword_authUser() {
        final var user = new UserEmail("uesr");
        user.setEnabled(true);
        user.setMaster(true);
        when(userService.findUser(anyString())).thenReturn(Optional.of(user));
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.setPassword("bob", "pass");

        verify(userTokenRepository).delete(userToken);
        verify(userEmailRepository).save(user);
        verify(alterUserService, never()).changePasswordWithUnlock(anyString(), anyString());
    }

    @Test
    public void resetPassword_UserUnlocked() {
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());
        final var user = new UserEmail("uesr");
        user.setLocked(true);
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.setPassword("bob", "pass");

        assertThat(user.isLocked()).isFalse();
    }

    @Test
    public void resetPassword_authUser_UserUnlocked() {
        final var user = new UserEmail("uesr");
        user.setEnabled(true);
        user.setMaster(true);
        when(userService.findUser(anyString())).thenReturn(Optional.of(user));
        user.setLocked(true);
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.setPassword("bob", "pass");

        assertThat(user.isLocked()).isFalse();
    }

    @Test
    public void resetPassword_authUser_passwordSet() {
        final var user = new UserEmail("uesr");
        user.setEnabled(true);
        user.setMaster(true);
        when(userService.findUser(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedpassword");
        user.setLocked(true);
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.setPassword("bob", "pass");

        assertThat(user.getPassword()).isEqualTo("hashedpassword");
        assertThat(user.getPasswordExpiry()).isAfterOrEqualTo(LocalDateTime.now().plusDays(9));
        verify(passwordEncoder).encode("pass");
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void resetPasswordLockedAccount() {
        final var staffUserAccount = getStaffUserAccountForBob();
        ((StaffUserAccount) staffUserAccount.get()).getStaff().setStatus("inactive");
        when(userService.findUser(anyString())).thenReturn(staffUserAccount);

        final var user = new UserEmail("uesr");
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> resetPasswordService.setPassword("bob", "pass")).isInstanceOf(LockedException.class);
    }

    @Test
    public void resetPasswordLockedAccount_authUser() {
        final var user = new UserEmail("uesr");
        user.setMaster(true);
        when(userService.findUser(anyString())).thenReturn(Optional.of(user));

        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> resetPasswordService.setPassword("bob", "pass")).isInstanceOf(LockedException.class);
    }
}
