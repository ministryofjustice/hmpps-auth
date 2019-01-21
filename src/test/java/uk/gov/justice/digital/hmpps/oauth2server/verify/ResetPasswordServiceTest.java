package uk.gov.justice.digital.hmpps.oauth2server.verify;

import com.microsoft.applicationinsights.TelemetryClient;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import uk.gov.justice.digital.hmpps.oauth2server.security.ChangePasswordService;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResetPasswordServiceTest {
    @Mock
    private UserEmailRepository userEmailRepository;
    @Mock
    private UserTokenRepository userTokenRepository;
    @Mock
    private UserService userService;
    @Mock
    private ChangePasswordService changePasswordService;
    @Mock
    private TelemetryClient telemetryClient;
    @Mock
    private NotificationClientApi notificationClient;
    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    private ResetPasswordService resetPasswordService;

    @Before
    public void setUp() {
        resetPasswordService = new ResetPasswordService(userEmailRepository, userTokenRepository, userService, changePasswordService, telemetryClient, notificationClient, "resetTemplate", "resetUnavailableTemplate");
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
        when(userService.getUserByUsername(anyString())).thenReturn(Optional.empty());

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
        staffUserAccount.get().getStaff().setStatus("inactive");
        when(userService.getUserByUsername(anyString())).thenReturn(staffUserAccount);

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
        accountOptional.get().getAccountDetail().setAccountStatus("LOCKED");
        when(userService.getUserByUsername(anyString())).thenReturn(accountOptional);

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
        accountOptional.get().getAccountDetail().setAccountStatus("LOCKED");
        when(userService.getUserByUsername(anyString())).thenReturn(accountOptional);

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
        accountOptional.get().getAccountDetail().setAccountStatus("EXPIRED & LOCKED(TIMED)");
        when(userService.getUserByUsername(anyString())).thenReturn(accountOptional);

        final var optionalLink = resetPasswordService.requestResetPassword("user", "url");
        verify(notificationClient).sendEmail(eq("resetTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optionalLink).isPresent();
        assertThat(mapCaptor.getValue()).containsOnly(MapEntry.entry("firstName", "Bob"), MapEntry.entry("resetLink", optionalLink.get()));
    }

    @Test
    public void requestResetPassword_existingToken() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", true, false);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        when(userService.getUserByUsername(anyString())).thenReturn(getStaffUserAccountForBob());
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
        when(userService.getUserByUsername(anyString())).thenReturn(getStaffUserAccountForBob());

        final var optionalLink = resetPasswordService.requestResetPassword("user", "url");
        verify(notificationClient).sendEmail(eq("resetTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optionalLink).isPresent();
        assertThat(mapCaptor.getValue()).containsOnly(MapEntry.entry("firstName", "Bob"), MapEntry.entry("resetLink", optionalLink.get()));
    }

    @Test
    public void requestResetPassword_uppercaseUsername() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", false, true);
        when(userEmailRepository.findById(any())).thenReturn(Optional.of(userEmail));
        when(userService.getUserByUsername(anyString())).thenReturn(getStaffUserAccountForBob());

        resetPasswordService.requestResetPassword("someuser", "url");
        verify(userEmailRepository).findById("SOMEUSER");
        verify(userService).getUserByUsername("SOMEUSER");
    }

    @Test
    public void requestResetPassword_verifyNotification() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", false, true);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        when(userService.getUserByUsername(anyString())).thenReturn(getStaffUserAccountForBob());
        final var captor = ArgumentCaptor.forClass(UserToken.class);

        final var linkOptional = resetPasswordService.requestResetPassword("user", "url");
        verify(userTokenRepository).save(captor.capture());
        final var value = captor.getValue();
        assertThat(linkOptional).get().isEqualTo(String.format("url/%s", value.getToken()));
        assertThat(value.getTokenType()).isEqualTo(TokenType.RESET);
        assertThat(value.getUserEmail().getEmail()).isEqualTo("email");
    }

    @Test
    public void requestResetPassword_sendFailure() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", false, true);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        when(userService.getUserByUsername(anyString())).thenReturn(getStaffUserAccountForBob());

        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), isNull())).thenThrow(new NotificationClientException("message"));

        assertThatThrownBy(() -> resetPasswordService.requestResetPassword("user", "url")).hasMessage("message");
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

    @Test
    public void getToken_notfound() {
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.empty());
        assertThat(resetPasswordService.getToken("token")).isEmpty();
    }

    @Test
    public void getToken_WrongType() {
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(new UserToken(TokenType.VERIFIED, null)));
        assertThat(resetPasswordService.getToken("token")).isEmpty();
    }

    @Test
    public void getToken() {
        final var userToken = new UserToken(TokenType.RESET, null);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        assertThat(resetPasswordService.getToken("token")).get().isSameAs(userToken);
    }

    @Test
    public void checkToken() {
        final var userToken = new UserToken(TokenType.RESET, null);
        userToken.setUserEmail(new UserEmail("user"));
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        assertThat(resetPasswordService.checkToken("token")).isEmpty();
    }

    @Test
    public void checkToken_invalid() {
        final var userToken = new UserToken(TokenType.CHANGE, null);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        assertThat(resetPasswordService.checkToken("token")).get().isEqualTo("invalid");
    }

    @Test
    public void checkToken_expiredTelemetryUsername() {
        final var userToken = new UserToken(TokenType.RESET, null);
        userToken.setUserEmail(new UserEmail("user"));
        userToken.setTokenExpiry(LocalDateTime.now().minusHours(1));
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.checkToken("token");
        verify(telemetryClient).trackEvent(eq("ResetPasswordFailure"), mapCaptor.capture(), isNull());
        final var value = mapCaptor.getValue();
        assertThat(value).containsOnly(MapEntry.entry("username", "user"), MapEntry.entry("reason", "expired"));
    }

    @Test
    public void checkToken_expired() {
        final var userToken = new UserToken(TokenType.RESET, new UserEmail("joe"));
        userToken.setTokenExpiry(LocalDateTime.now().minusHours(1));
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        assertThat(resetPasswordService.checkToken("token")).get().isEqualTo("expired");
    }

    @Test
    public void resetPassword() {
        when(userService.getUserByUsername(anyString())).thenReturn(getStaffUserAccountForBob());
        final var user = new UserEmail("uesr");
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.resetPassword("bob", "pass");

        verify(userTokenRepository).delete(userToken);
        verify(userEmailRepository).save(user);
        verify(changePasswordService).changePasswordWithUnlock("uesr", "pass");
    }

    @Test
    public void resetPassword_UserUnlocked() {
        when(userService.getUserByUsername(anyString())).thenReturn(getStaffUserAccountForBob());
        final var user = new UserEmail("uesr");
        user.setLocked(true);
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.resetPassword("bob", "pass");

        assertThat(user.isLocked()).isFalse();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void resetPasswordLockedAccount() {
        final var staffUserAccount = getStaffUserAccountForBob();
        staffUserAccount.get().getStaff().setStatus("inactive");
        when(userService.getUserByUsername(anyString())).thenReturn(staffUserAccount);

        final var user = new UserEmail("uesr");
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> resetPasswordService.resetPassword("bob", "pass")).isInstanceOf(LockedException.class);
    }

}
