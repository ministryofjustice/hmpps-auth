package uk.gov.justice.digital.hmpps.oauth2server.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import uk.gov.justice.digital.hmpps.oauth2server.security.AlterUserService;
import uk.gov.justice.digital.hmpps.oauth2server.security.ReusedPasswordException;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordServiceImpl.NotificationClientRuntimeException;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;
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
        resetPasswordService = new ResetPasswordServiceImpl(userEmailRepository, userTokenRepository, userService,
                alterUserService, notificationClient,
                "resetTemplate", "resetUnavailableTemplate", "resetUnavailableEmailNotFoundTemplate", "reset-confirm", passwordEncoder, 10);
    }

    @Test
    public void requestResetPassword_noUserEmail() throws NotificationClientRuntimeException {
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.empty());
        final var optional = resetPasswordService.requestResetPassword("user", "url");
        assertThat(optional).isEmpty();
    }

    @Test
    public void requestResetPassword_noEmail() throws NotificationClientRuntimeException {
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(new UserEmail("user")));
        final var optional = resetPasswordService.requestResetPassword("user", "url");
        assertThat(optional).isEmpty();
    }

    @Test
    public void requestResetPassword_noNomisUser() throws NotificationClientException {
        final var userEmail = new UserEmail("USER", "email", true, false);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        when(userService.findUser(anyString())).thenReturn(Optional.empty());

        final var optional = resetPasswordService.requestResetPassword("user", "url");
        verify(notificationClient).sendEmail(eq("resetUnavailableTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optional).isEmpty();
        assertThat(mapCaptor.getValue()).containsExactly(entry("firstName", "USER"));
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
        assertThat(mapCaptor.getValue()).containsExactly(entry("firstName", "Bob"));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void requestResetPassword_authLocked() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", true, true);
        userEmail.setPerson(new Person("user", "Bob", "Smith"));
        userEmail.setMaster(true);
        userEmail.setEnabled(true);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));

        final var optionalLink = resetPasswordService.requestResetPassword("user", "url");
        verify(notificationClient).sendEmail(eq("resetTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optionalLink).isPresent();
        assertThat(mapCaptor.getValue()).containsOnly(entry("firstName", "Bob"), entry("resetLink", optionalLink.get()));
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
        assertThat(mapCaptor.getValue()).containsExactly(entry("firstName", "Bob"));
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
        assertThat(mapCaptor.getValue()).containsOnly(entry("firstName", "Bob"), entry("resetLink", optionalLink.get()));
    }

    @Test
    public void requestResetPassword_existingToken() throws NotificationClientRuntimeException {
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
        assertThat(mapCaptor.getValue()).containsOnly(entry("firstName", "Bob"), entry("resetLink", optionalLink.get()));
    }

    @Test
    public void requestResetPassword_uppercaseUsername() throws NotificationClientRuntimeException {
        final var userEmail = new UserEmail("SOMEUSER", "email", false, true);
        when(userEmailRepository.findById(any())).thenReturn(Optional.of(userEmail));
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());

        resetPasswordService.requestResetPassword("someuser", "url");
        verify(userEmailRepository).findById("SOMEUSER");
        verify(userService).findUser("SOMEUSER");
    }

    @Test
    public void requestResetPassword_verifyNotification() throws NotificationClientRuntimeException {
        final var userEmail = new UserEmail("someuser", "email", false, true);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());
        final var captor = ArgumentCaptor.forClass(UserToken.class);

        final var linkOptional = resetPasswordService.requestResetPassword("user", "url");
        verify(userTokenRepository).save(captor.capture());
        final var value = captor.getValue();
        assertThat(linkOptional).get().isEqualTo(String.format("url-confirm?token=%s", value.getToken()));
        assertThat(value.getTokenType()).isEqualTo(TokenType.RESET);
        assertThat(value.getUserEmail().getEmail()).isEqualTo("email");
    }

    @Test
    public void requestResetPassword_sendFailure() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", false, true);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());

        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), isNull())).thenThrow(new NotificationClientException("message"));

        assertThatThrownBy(() -> resetPasswordService.requestResetPassword("user", "url")).hasMessageContaining("NotificationClientException: message");
    }

    @Test
    public void requestResetPassword_emailAddressNotFound() throws NotificationClientException {
        when(userEmailRepository.findByEmail(any())).thenReturn(Collections.emptyList());

        final var optional = resetPasswordService.requestResetPassword("someuser@somewhere", "url");
        verify(notificationClient).sendEmail(eq("resetUnavailableEmailNotFoundTemplate"), eq("someuser@somewhere"), mapCaptor.capture(), isNull());
        assertThat(optional).isEmpty();
        assertThat(mapCaptor.getValue()).isEmpty();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void requestResetPassword_multipleEmailAddresses() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", false, false);
        userEmail.setPerson(new Person("user", "Bob", "Smith"));
        userEmail.setMaster(true);
        userEmail.setEnabled(true);
        when(userEmailRepository.findByEmail(any())).thenReturn(List.of(userEmail, userEmail));

        final var optional = resetPasswordService.requestResetPassword("someuser@somewhere", "http://url");
        verify(notificationClient).sendEmail(eq("resetTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optional).isPresent();
        assertThat(mapCaptor.getValue()).containsOnly(entry("firstName", "Bob"), entry("resetLink", optional.get()));
    }

    @Test
    public void requestResetPassword_multipleEmailAddresses_verifyToken() {
        final var userEmail = new UserEmail("someuser", "email", false, false);
        userEmail.setPerson(new Person("user", "Bob", "Smith"));
        userEmail.setMaster(true);
        userEmail.setEnabled(true);
        when(userEmailRepository.findByEmail(any())).thenReturn(List.of(userEmail, userEmail));
        final var captor = ArgumentCaptor.forClass(UserToken.class);

        final var linkOptional = resetPasswordService.requestResetPassword("someuser@somewhere", "http://url");
        verify(userTokenRepository).save(captor.capture());
        final var value = captor.getValue();
        assertThat(linkOptional).get().isEqualTo(String.format("http://url-select?token=%s", value.getToken()));
    }

    @Test
    public void requestResetPassword_multipleEmailAddresses_noneCanBeReset() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", false, false);
        userEmail.setPerson(new Person("user", "Bob", "Smith"));
        userEmail.setMaster(true);
        when(userEmailRepository.findByEmail(any())).thenReturn(List.of(userEmail, userEmail));

        final var optional = resetPasswordService.requestResetPassword("someuser@somewhere", "http://url");
        verify(notificationClient).sendEmail(eq("resetUnavailableTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optional).isEmpty();
        assertThat(mapCaptor.getValue()).containsOnly(entry("firstName", "Bob"));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void requestResetPassword_byEmail() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser", "email", false, true);
        when(userEmailRepository.findByEmail(anyString())).thenReturn(List.of(userEmail));
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());

        final var optionalLink = resetPasswordService.requestResetPassword("user@where", "url");
        verify(notificationClient).sendEmail(eq("resetTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optionalLink).isPresent();
        assertThat(mapCaptor.getValue()).containsOnly(entry("firstName", "Bob"), entry("resetLink", optionalLink.get()));
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

    @Test
    public void resetPassword() throws NotificationClientException {
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());
        final var user = new UserEmail("user");
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.setPassword("bob", "pass");

        verify(userTokenRepository).delete(userToken);
        verify(userEmailRepository).save(user);
        verify(alterUserService).changePasswordWithUnlock("user", "pass");
        verify(notificationClient).sendEmail("reset-confirm", null, Map.of("firstName", "user", "username", "user"), null);

    }

    @Test
    public void resetPassword_authUser() throws NotificationClientException {
        final var user = new UserEmail("user");
        user.setEnabled(true);
        user.setMaster(true);
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.setPassword("bob", "pass");

        verify(userTokenRepository).delete(userToken);
        verify(userEmailRepository).save(user);
        verify(alterUserService, never()).changePasswordWithUnlock(anyString(), anyString());
        verify(notificationClient).sendEmail("reset-confirm", null, Map.of("firstName", "user", "username", "user"), null);

    }

    @Test
    public void resetPassword_UserUnlocked() throws NotificationClientException {
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());
        final var user = new UserEmail("user");
        user.setLocked(true);
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.setPassword("bob", "pass");

        assertThat(user.isLocked()).isFalse();
        verify(notificationClient).sendEmail("reset-confirm", null, Map.of("firstName", "user", "username", "user"), null);

    }

    @Test
    public void resetPassword_authUser_UserUnlocked() throws NotificationClientException {
        final var user = new UserEmail("user");
        user.setEnabled(true);
        user.setMaster(true);
        user.setLocked(true);
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.setPassword("bob", "pass");

        assertThat(user.isLocked()).isFalse();
        verify(notificationClient).sendEmail("reset-confirm", null, Map.of("firstName", "user", "username", "user"), null);

    }

    @Test
    public void resetPassword_EmailVerified() {
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());
        final var user = new UserEmail("user");
        user.setLocked(true);
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.setPassword("bob", "pass");

        assertThat(user.isVerified()).isTrue();
    }

    @Test
    public void resetPassword_authUser_passwordSet() {
        final var user = new UserEmail("user");
        user.setEnabled(true);
        user.setMaster(true);
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

        final var user = new UserEmail("user");
        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> resetPasswordService.setPassword("bob", "pass")).isInstanceOf(LockedException.class);
    }

    @Test
    public void resetPasswordLockedAccount_authUser() {
        final var user = new UserEmail("user");
        user.setMaster(true);

        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> resetPasswordService.setPassword("bob", "pass")).isInstanceOf(LockedException.class);
    }

    @Test
    public void resetPassword_authUserPasswordSameAsCurrent() {
        final var user = new UserEmail("user", "email", true, false);
        user.setMaster(true);
        user.setEnabled(true);
        user.setPassword("oldencryptedpassword");
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(Boolean.TRUE);

        final var userToken = new UserToken(TokenType.RESET, user);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> resetPasswordService.setPassword("bob", "pass")).isInstanceOf(ReusedPasswordException.class);

        verify(passwordEncoder).matches("pass", "oldencryptedpassword");
    }

    @Test
    public void moveTokenToAccount_missingUsername() {
        assertThatThrownBy(() -> resetPasswordService.moveTokenToAccount("token", "  ")).hasMessageContaining("failed with reason: missing");
    }

    @Test
    public void moveTokenToAccount_usernameNotFound() {
        assertThatThrownBy(() -> resetPasswordService.moveTokenToAccount("token", "noone")).hasMessageContaining("failed with reason: notfound");
    }

    @Test
    public void moveTokenToAccount_differentEmail() {
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(new UserEmail("user", "email", true, false)));
        when(userTokenRepository.findById("token")).thenReturn(Optional.of(new UserToken(TokenType.RESET, new UserEmail("other", "emailother", true, false))));
        assertThatThrownBy(() -> resetPasswordService.moveTokenToAccount("token", "noone")).hasMessageContaining("failed with reason: email");
    }

    @Test
    public void moveTokenToAccount_disabled() {
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(new UserEmail("user", "email", true, false)));
        when(userTokenRepository.findById("token")).thenReturn(Optional.of(new UserToken(TokenType.RESET, new UserEmail("other", "email", true, false))));
        assertThatThrownBy(() -> resetPasswordService.moveTokenToAccount("token", "noone")).extracting("reason").containsOnly("locked");
    }

    @Test
    public void moveTokenToAccount_sameUserAccount() {
        final var user = new UserEmail("USER", "email", true, false);
        user.setEnabled(true);
        user.setMaster(true);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(user));
        when(userTokenRepository.findById("token")).thenReturn(Optional.of(new UserToken(TokenType.RESET, user)));
        final var newToken = resetPasswordService.moveTokenToAccount("token", "USER");
        assertThat(newToken).isEqualTo("token");
    }

    @Test
    public void moveTokenToAccount_differentAccount() {
        final var user = new UserEmail("USER", "email", true, false);
        user.setEnabled(true);
        user.setMaster(true);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(user));
        final var userToken = new UserToken(TokenType.RESET, new UserEmail("other", "email", true, false));
        when(userTokenRepository.findById("token")).thenReturn(Optional.of(userToken));
        final var newToken = resetPasswordService.moveTokenToAccount("token", "USER");
        assertThat(newToken).hasSize(36);
        verify(userTokenRepository).delete(userToken);
        verify(userTokenRepository).save(any());
    }
}
