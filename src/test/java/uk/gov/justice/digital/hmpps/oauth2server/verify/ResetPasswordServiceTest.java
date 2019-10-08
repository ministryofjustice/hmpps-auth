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
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
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
    private UserRepository userRepository;
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
        resetPasswordService = new ResetPasswordServiceImpl(userRepository, userTokenRepository, userService,
                alterUserService, notificationClient,
                "resetTemplate", "resetUnavailableTemplate", "resetUnavailableEmailNotFoundTemplate", "reset-confirm", passwordEncoder, 10);
    }

    @Test
    public void requestResetPassword_noUserEmail() throws NotificationClientRuntimeException {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        final var optional = resetPasswordService.requestResetPassword("user", "url");
        assertThat(optional).isEmpty();
    }

    @Test
    public void requestResetPassword_noEmail() throws NotificationClientRuntimeException {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(User.of("user")));
        final var optional = resetPasswordService.requestResetPassword("user", "url");
        assertThat(optional).isEmpty();
    }

    @Test
    public void requestResetPassword_noNomisUser() throws NotificationClientException {
        final var user = User.builder().username("USER").email("email").verified(true).build();
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(userService.findUser(anyString())).thenReturn(Optional.empty());

        final var optional = resetPasswordService.requestResetPassword("user", "url");
        verify(notificationClient).sendEmail(eq("resetUnavailableTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optional).isEmpty();
        assertThat(mapCaptor.getValue()).containsExactly(entry("firstName", "USER"));
    }

    @Test
    public void requestResetPassword_inactive() throws NotificationClientException {
        final var user = User.builder().username("someuser").email("email").verified(true).build();
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        final var staffUserAccount = getStaffUserAccountForBob();
        ((StaffUserAccount) staffUserAccount.orElseThrow()).getStaff().setStatus("inactive");
        when(userService.findUser(anyString())).thenReturn(staffUserAccount);

        final var optional = resetPasswordService.requestResetPassword("user", "url");
        verify(notificationClient).sendEmail(eq("resetUnavailableTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optional).isEmpty();
        assertThat(mapCaptor.getValue()).containsExactly(entry("firstName", "Bob"));
    }

    @Test
    public void requestResetPassword_authLocked() throws NotificationClientException {
        final var user = User.builder().username("someuser").email("email").verified(true).locked(true).build();
        user.setPerson(new Person("Bob", "Smith"));
        user.setMaster(true);
        user.setEnabled(true);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));

        final var optionalLink = resetPasswordService.requestResetPassword("user", "url");
        verify(notificationClient).sendEmail(eq("resetTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optionalLink).isPresent();
        assertThat(mapCaptor.getValue()).containsOnly(entry("firstName", "Bob"), entry("resetLink", optionalLink.get()));
    }

    @Test
    public void requestResetPassword_notAuthLocked() throws NotificationClientException {
        final var user = User.builder().username("someuser").email("email").verified(true).build();
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        final var accountOptional = getStaffUserAccountForBob();
        ((StaffUserAccount) accountOptional.orElseThrow()).getAccountDetail().setAccountStatus("LOCKED");
        when(userService.findUser(anyString())).thenReturn(accountOptional);

        final var optional = resetPasswordService.requestResetPassword("user", "url");
        verify(notificationClient).sendEmail(eq("resetUnavailableTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optional).isEmpty();
        assertThat(mapCaptor.getValue()).containsExactly(entry("firstName", "Bob"));
    }

    @Test
    public void requestResetPassword_userLocked() throws NotificationClientException {
        final var user = User.builder().username("someuser").email("email").verified(true).build();
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        final var accountOptional = getStaffUserAccountForBob();
        ((StaffUserAccount) accountOptional.orElseThrow()).getAccountDetail().setAccountStatus("EXPIRED & LOCKED(TIMED)");
        when(userService.findUser(anyString())).thenReturn(accountOptional);

        final var optionalLink = resetPasswordService.requestResetPassword("user", "url");
        verify(notificationClient).sendEmail(eq("resetTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optionalLink).isPresent();
        assertThat(mapCaptor.getValue()).containsOnly(entry("firstName", "Bob"), entry("resetLink", optionalLink.get()));
    }

    @Test
    public void requestResetPassword_existingToken() throws NotificationClientRuntimeException {
        final var user = User.builder().username("someuser").email("email").verified(true).build();
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());
        final var existingUserToken = user.createToken(TokenType.RESET);

        resetPasswordService.requestResetPassword("user", "url");
        assertThat(user.getTokens()).hasSize(1).extracting(UserToken::getToken).doesNotContain(existingUserToken.getToken());
    }

    @Test
    public void requestResetPassword_verifyToken() throws NotificationClientException {
        final var user = User.builder().username("someuser").email("email").locked(true).build();
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());

        final var optionalLink = resetPasswordService.requestResetPassword("user", "url");
        verify(notificationClient).sendEmail(eq("resetTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optionalLink).isPresent();
        assertThat(mapCaptor.getValue()).containsOnly(entry("firstName", "Bob"), entry("resetLink", optionalLink.get()));
    }

    @Test
    public void requestResetPassword_uppercaseUsername() throws NotificationClientRuntimeException {
        final var user = User.builder().username("SOMEUSER").email("email").locked(true).build();
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(user));
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());

        resetPasswordService.requestResetPassword("someuser", "url");
        verify(userRepository).findByUsername("SOMEUSER");
        verify(userService).findUser("SOMEUSER");
    }

    @Test
    public void requestResetPassword_verifyNotification() throws NotificationClientRuntimeException {
        final var user = User.builder().username("someuser").email("email").locked(true).build();
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());

        final var linkOptional = resetPasswordService.requestResetPassword("user", "url");
        final var value = user.getTokens().stream().findFirst().orElseThrow();
        assertThat(linkOptional).get().isEqualTo(String.format("url-confirm?token=%s", value.getToken()));
        assertThat(value.getTokenType()).isEqualTo(TokenType.RESET);
        assertThat(value.getUser().getEmail()).isEqualTo("email");
    }

    @Test
    public void requestResetPassword_sendFailure() throws NotificationClientException {
        final var user = User.builder().username("someuser").email("email").locked(true).build();
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());

        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), isNull())).thenThrow(new NotificationClientException("message"));

        assertThatThrownBy(() -> resetPasswordService.requestResetPassword("user", "url")).hasMessageContaining("NotificationClientException: message");
    }

    @Test
    public void requestResetPassword_emailAddressNotFound() throws NotificationClientException {
        when(userRepository.findByEmail(any())).thenReturn(Collections.emptyList());

        final var optional = resetPasswordService.requestResetPassword("someuser@somewhere", "url");
        verify(notificationClient).sendEmail(eq("resetUnavailableEmailNotFoundTemplate"), eq("someuser@somewhere"), mapCaptor.capture(), isNull());
        assertThat(optional).isEmpty();
        assertThat(mapCaptor.getValue()).isEmpty();
    }

    @Test
    public void requestResetPassword_multipleEmailAddresses() throws NotificationClientException {
        final var user = User.builder().username("someuser").email("email").build();
        user.setPerson(new Person("Bob", "Smith"));
        user.setMaster(true);
        user.setEnabled(true);
        when(userRepository.findByEmail(any())).thenReturn(List.of(user, user));

        final var optional = resetPasswordService.requestResetPassword("someuser@somewhere", "http://url");
        verify(notificationClient).sendEmail(eq("resetTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optional).isPresent();
        assertThat(mapCaptor.getValue()).containsOnly(entry("firstName", "Bob"), entry("resetLink", optional.get()));
    }

    @Test
    public void requestResetPassword_multipleEmailAddresses_verifyToken() {
        final var user = User.builder().username("someuser").email("email").build();
        user.setPerson(new Person("Bob", "Smith"));
        user.setMaster(true);
        user.setEnabled(true);
        when(userRepository.findByEmail(any())).thenReturn(List.of(user, user));

        final var linkOptional = resetPasswordService.requestResetPassword("someuser@somewhere", "http://url");
        final var userToken = user.getTokens().stream().findFirst().orElseThrow();
        assertThat(linkOptional).get().isEqualTo(String.format("http://url-select?token=%s", userToken.getToken()));
    }

    @Test
    public void requestResetPassword_multipleEmailAddresses_noneCanBeReset() throws NotificationClientException {
        final var user = User.builder().username("someuser").email("email").build();
        user.setPerson(new Person("Bob", "Smith"));
        user.setMaster(true);
        when(userRepository.findByEmail(any())).thenReturn(List.of(user, user));

        final var optional = resetPasswordService.requestResetPassword("someuser@somewhere", "http://url");
        verify(notificationClient).sendEmail(eq("resetUnavailableTemplate"), eq("email"), mapCaptor.capture(), isNull());
        assertThat(optional).isEmpty();
        assertThat(mapCaptor.getValue()).containsOnly(entry("firstName", "Bob"));
    }

    @Test
    public void requestResetPassword_byEmail() throws NotificationClientException {
        final var user = User.builder().username("someuser").email("email").locked(true).build();
        when(userRepository.findByEmail(anyString())).thenReturn(List.of(user));
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
        final var user = User.of("user");
        final var userToken = user.createToken(TokenType.RESET);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.setPassword("bob", "pass");

        assertThat(user.getTokens()).isEmpty();
        verify(userRepository).save(user);
        verify(alterUserService).changePasswordWithUnlock("user", "pass");
        verify(notificationClient).sendEmail("reset-confirm", null, Map.of("firstName", "user", "username", "user"), null);

    }

    @Test
    public void resetPassword_authUser() throws NotificationClientException {
        final var user = User.of("user");
        user.setEnabled(true);
        user.setMaster(true);
        final var userToken = user.createToken(TokenType.RESET);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.setPassword("bob", "pass");

        assertThat(user.getTokens()).isEmpty();
        verify(userRepository).save(user);
        verify(alterUserService, never()).changePasswordWithUnlock(anyString(), anyString());
        verify(notificationClient).sendEmail("reset-confirm", null, Map.of("firstName", "user", "username", "user"), null);

    }

    @Test
    public void resetPassword_UserUnlocked() throws NotificationClientException {
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());
        final var user = User.of("user");
        user.setLocked(true);
        final var userToken = user.createToken(TokenType.RESET);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.setPassword("bob", "pass");

        assertThat(user.isLocked()).isFalse();
        verify(notificationClient).sendEmail("reset-confirm", null, Map.of("firstName", "user", "username", "user"), null);

    }

    @Test
    public void resetPassword_authUser_UserUnlocked() throws NotificationClientException {
        final var user = User.of("user");
        user.setEnabled(true);
        user.setMaster(true);
        user.setLocked(true);
        final var userToken = user.createToken(TokenType.RESET);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.setPassword("bob", "pass");

        assertThat(user.isLocked()).isFalse();
        verify(notificationClient).sendEmail("reset-confirm", null, Map.of("firstName", "user", "username", "user"), null);

    }

    @Test
    public void resetPassword_EmailVerified() {
        when(userService.findUser(anyString())).thenReturn(getStaffUserAccountForBob());
        final var user = User.of("user");
        user.setLocked(true);
        final var userToken = user.createToken(TokenType.RESET);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.setPassword("bob", "pass");

        assertThat(user.isVerified()).isTrue();
    }

    @Test
    public void resetPassword_authUser_passwordSet() {
        final var user = User.of("user");
        user.setEnabled(true);
        user.setMaster(true);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedpassword");
        user.setLocked(true);
        final var userToken = user.createToken(TokenType.RESET);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        resetPasswordService.setPassword("bob", "pass");

        assertThat(user.getPassword()).isEqualTo("hashedpassword");
        assertThat(user.getPasswordExpiry()).isAfterOrEqualTo(LocalDateTime.now().plusDays(9));
        verify(passwordEncoder).encode("pass");
    }

    @Test
    public void resetPasswordLockedAccount() {
        final var staffUserAccount = getStaffUserAccountForBob();
        ((StaffUserAccount) staffUserAccount.orElseThrow()).getStaff().setStatus("inactive");
        when(userService.findUser(anyString())).thenReturn(staffUserAccount);

        final var user = User.of("user");
        final var userToken = user.createToken(TokenType.RESET);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> resetPasswordService.setPassword("bob", "pass")).isInstanceOf(LockedException.class);
    }

    @Test
    public void resetPasswordLockedAccount_authUser() {
        final var user = User.of("user");
        user.setMaster(true);

        final var userToken = user.createToken(TokenType.RESET);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));

        assertThatThrownBy(() -> resetPasswordService.setPassword("bob", "pass")).isInstanceOf(LockedException.class);
    }

    @Test
    public void resetPassword_authUserPasswordSameAsCurrent() {
        final var user = User.builder().username("user").email("email").verified(true).build();
        user.setMaster(true);
        user.setEnabled(true);
        user.setPassword("oldencryptedpassword");
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(Boolean.TRUE);

        final var userToken = user.createToken(TokenType.RESET);
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
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(User.builder().username("user").email("email").verified(true).build()));
        final var builtUser = User.builder().username("other").email("emailother").verified(true).build();
        when(userTokenRepository.findById("token")).thenReturn(Optional.of(builtUser.createToken(TokenType.RESET)));
        assertThatThrownBy(() -> resetPasswordService.moveTokenToAccount("token", "noone")).hasMessageContaining("failed with reason: email");
    }

    @Test
    public void moveTokenToAccount_disabled() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(User.builder().username("user").email("email").verified(true).build()));
        final var builtUser = User.builder().username("other").email("email").verified(true).build();
        when(userTokenRepository.findById("token")).thenReturn(Optional.of(builtUser.createToken(TokenType.RESET)));
        assertThatThrownBy(() -> resetPasswordService.moveTokenToAccount("token", "noone")).extracting("reason").containsOnly("locked");
    }

    @Test
    public void moveTokenToAccount_sameUserAccount() {
        final var user = User.builder().username("USER").email("email").verified(true).build();
        user.setEnabled(true);
        user.setMaster(true);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(userTokenRepository.findById("token")).thenReturn(Optional.of(user.createToken(TokenType.RESET)));
        final var newToken = resetPasswordService.moveTokenToAccount("token", "USER");
        assertThat(newToken).isEqualTo("token");
    }

    @Test
    public void moveTokenToAccount_differentAccount() {
        final var user = User.builder().username("USER").email("email").verified(true).build();
        user.setEnabled(true);
        user.setMaster(true);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        final var builtUser = User.builder().username("other").email("email").verified(true).build();
        final var userToken = builtUser.createToken(TokenType.RESET);
        when(userTokenRepository.findById("token")).thenReturn(Optional.of(userToken));
        final var newToken = resetPasswordService.moveTokenToAccount("token", "USER");
        assertThat(newToken).hasSize(36);
        verify(userTokenRepository).delete(userToken);
        assertThat(user.getTokens()).extracting(UserToken::getToken).containsExactly(newToken);
    }
}
