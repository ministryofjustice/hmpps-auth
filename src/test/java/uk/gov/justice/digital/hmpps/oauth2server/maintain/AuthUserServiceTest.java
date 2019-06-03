package uk.gov.justice.digital.hmpps.oauth2server.maintain;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService.AmendUserException;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService.CreateUserException;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthUserServiceTest {
    @Mock
    private UserEmailRepository userEmailRepository;
    @Mock
    private UserTokenRepository userTokenRepository;
    @Mock
    private NotificationClientApi notificationClient;
    @Mock
    private TelemetryClient telemetryClient;
    @Mock
    private VerifyEmailService verifyEmailService;

    private AuthUserService authUserService;

    @Before
    public void setUp() {
        authUserService = new AuthUserService(userTokenRepository, userEmailRepository, notificationClient, telemetryClient, verifyEmailService, "licences");
    }

    @Test
    public void createUser_usernameLength() {
        assertThatThrownBy(() -> authUserService.createUser("user", "email", "first", "last", Collections.emptySet(), "url", "bob")).
                isInstanceOf(CreateUserException.class).hasMessage("Create user failed for field username with reason: length");
    }

    @Test
    public void createUser_usernameFormat() {
        assertThatThrownBy(() -> authUserService.createUser("user-name", "email", "first", "last", Collections.emptySet(), "url", "bob")).
                isInstanceOf(CreateUserException.class).hasMessage("Create user failed for field username with reason: format");
    }

    @Test
    public void createUser_firstNameLength() {
        assertThatThrownBy(() -> authUserService.createUser("userme", "email", "s", "last", Collections.emptySet(), "url", "bob")).
                isInstanceOf(CreateUserException.class).hasMessage("Create user failed for field firstName with reason: length");
    }

    @Test
    public void createUser_lastNameLength() {
        assertThatThrownBy(() -> authUserService.createUser("userme", "email", "se", "x", Collections.emptySet(), "url", "bob")).
                isInstanceOf(CreateUserException.class).hasMessage("Create user failed for field lastName with reason: length");
    }

    @Test
    public void createUser_emailValidation() throws VerifyEmailException {
        doThrow(new VerifyEmailException("reason")).when(verifyEmailService).validateEmailAddress(anyString());
        assertThatThrownBy(() -> authUserService.createUser("userme", "email", "se", "xx", Collections.emptySet(), "url", "bob")).
                isInstanceOf(VerifyEmailException.class).hasMessage("Verify email failed with reason: reason");

        verify(verifyEmailService).validateEmailAddress("email");
    }

    @Test
    public void createUser_successLinkReturned() throws VerifyEmailException, CreateUserException, NotificationClientException {
        final var link = authUserService.createUser("userme", "email", "se", "xx", Collections.emptySet(), "url?token=", "bob");

        assertThat(link).startsWith("url?token=").hasSize("url?token=".length() + 36);
    }

    @Test
    public void createUser_trackSuccess() throws VerifyEmailException, CreateUserException, NotificationClientException {
        authUserService.createUser("userme", "email", "se", "xx", Collections.emptySet(), "url?token=", "bob");
        verify(telemetryClient).trackEvent("AuthUserCreateSuccess", Map.of("username", "USERME", "admin", "bob"), null);
    }

    @Test
    public void createUser_saveTokenRepository() throws VerifyEmailException, CreateUserException, NotificationClientException {
        final var link = authUserService.createUser("userme", "email", "se", "xx", Collections.emptySet(), "url?token=", "bob");

        final var captor = ArgumentCaptor.forClass(UserToken.class);
        verify(userTokenRepository).save(captor.capture());

        assertThat(captor.getValue().getTokenType()).isEqualTo(TokenType.RESET);
        assertThat(captor.getValue().getToken()).isEqualTo(link.substring("url?token=".length()));
        assertThat(captor.getValue().getTokenExpiry()).isBetween(LocalDateTime.now().plusDays(6), LocalDateTime.now().plusDays(8));
    }

    @Test
    public void createUser_saveEmailRepository() throws VerifyEmailException, CreateUserException, NotificationClientException {
        authUserService.createUser("userMe", "eMail", "first", "last", Collections.emptySet(), "url?token=", "bob");

        final var captor = ArgumentCaptor.forClass(UserEmail.class);
        verify(userEmailRepository).save(captor.capture());

        final var user = captor.getValue();
        assertThat(user.getName()).isEqualTo("first last");
        assertThat(user.getEmail()).isEqualTo("email");
        assertThat(user.getUsername()).isEqualTo("USERME");
        assertThat(user.getPassword()).isNull();
        assertThat(user.isMaster()).isTrue();
        assertThat(user.isVerified()).isFalse();
        assertThat(user.isCredentialsNonExpired()).isFalse();
        assertThat(user.getAuthorities()).isEmpty();
    }

    @Test
    public void createUser_mergeRoles() throws VerifyEmailException, CreateUserException, NotificationClientException {
        authUserService.createUser("userMe", "eMail", "first", "last", Set.of("ROLE_LICENCE_VARY", "ROLE_DISALLOWED"), "url?token=", "bob");

        final var captor = ArgumentCaptor.forClass(UserEmail.class);
        verify(userEmailRepository).save(captor.capture());

        final var user = captor.getValue();
        assertThat(user.getAuthorities()).containsOnly(new Authority("ROLE_LICENCE_VARY"));
    }

    @Test
    public void createUser_mergeRoles_addRolePrefix() throws VerifyEmailException, CreateUserException, NotificationClientException {
        authUserService.createUser("userMe", "eMail", "first", "last", Set.of("LICENCE_VARY", "DISALLOWED"), "url?token=", "bob");

        final var captor = ArgumentCaptor.forClass(UserEmail.class);
        verify(userEmailRepository).save(captor.capture());

        final var user = captor.getValue();
        assertThat(user.getAuthorities()).containsOnly(new Authority("ROLE_LICENCE_VARY"));
    }

    @Test
    public void createUser_callNotify() throws VerifyEmailException, CreateUserException, NotificationClientException {
        final var link = authUserService.createUser("userme", "email", "first", "last", Collections.emptySet(), "url?token=", "bob");

        verify(notificationClient).sendEmail("licences", "email", Map.of("resetLink", link, "firstName", "first", "username", "USERME"), null);
    }

    @Test
    public void amendUser_emailValidation() throws VerifyEmailException {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUserEmailUser());
        doThrow(new VerifyEmailException("reason")).when(verifyEmailService).validateEmailAddress(anyString());
        assertThatThrownBy(() -> authUserService.amendUser("userme", "email", "url?token=", "bob")).
                isInstanceOf(VerifyEmailException.class).hasMessage("Verify email failed with reason: reason");

        verify(verifyEmailService).validateEmailAddress("email");
    }

    @Test
    public void amendUser_successLinkReturned() throws VerifyEmailException, CreateUserException, NotificationClientException, AmendUserException {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUserEmailUser());
        final var link = authUserService.amendUser("userme", "email", "url?token=", "bob");

        assertThat(link).startsWith("url?token=").hasSize("url?token=".length() + 36);
    }

    @Test
    public void amendUser_trackSuccess() throws VerifyEmailException, AmendUserException, NotificationClientException {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUserEmailUser());
        authUserService.amendUser("userme", "email", "url?token=", "bob");
        verify(telemetryClient).trackEvent("AuthUserAmendSuccess", Map.of("username", "SOMEUSER", "admin", "bob"), null);
    }

    @Test
    public void amendUser_saveTokenRepository() throws VerifyEmailException, AmendUserException, NotificationClientException {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUserEmailUser());
        final var link = authUserService.amendUser("userme", "email", "url?token=", "bob");

        final var captor = ArgumentCaptor.forClass(UserToken.class);
        verify(userTokenRepository).save(captor.capture());

        assertThat(captor.getValue().getTokenType()).isEqualTo(TokenType.RESET);
        assertThat(captor.getValue().getToken()).isEqualTo(link.substring("url?token=".length()));
        assertThat(captor.getValue().getTokenExpiry()).isBetween(LocalDateTime.now().plusDays(6), LocalDateTime.now().plusDays(8));
    }

    @Test
    public void amendUser_saveEmailRepository() throws VerifyEmailException, AmendUserException, NotificationClientException {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUserEmailUser());
        authUserService.amendUser("userMe", "eMail", "url?token=", "bob");

        final var captor = ArgumentCaptor.forClass(UserEmail.class);
        verify(userEmailRepository).save(captor.capture());

        final var user = captor.getValue();
        assertThat(user.getEmail()).isEqualTo("email");
    }

    @Test
    public void amendUser_verifiedEmail() throws VerifyEmailException, AmendUserException, NotificationClientException {
        final UserEmail user = new UserEmail("SOMEUSER");
        user.setVerified(true);
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> authUserService.amendUser("userme", "email", "url?token=", "bob")).
                isInstanceOf(AmendUserException.class).
                hasMessageContaining("reason: notinitial");
    }

    @Test
    public void amendUser_passwordSet() throws VerifyEmailException, AmendUserException, NotificationClientException {
        final UserEmail user = new UserEmail("SOMEUSER");
        user.setPassword("some pass");
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> authUserService.amendUser("userme", "email", "url?token=", "bob")).
                isInstanceOf(AmendUserException.class).
                hasMessageContaining("reason: notinitial");
    }

    private Optional<UserEmail> createUserEmailUser() {
        return Optional.of(new UserEmail("SOMEUSER"));
    }
}
