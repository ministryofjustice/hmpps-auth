package uk.gov.justice.digital.hmpps.oauth2server.maintain;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.*;
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
import java.util.List;
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
    @Mock
    private AuthUserGroupService authUserGroupService;

    private AuthUserService authUserService;

    private static final Set<GrantedAuthority> GRANTED_AUTHORITY_SUPER_USER = Set.of(new SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"));

    @Before
    public void setUp() {
        authUserService = new AuthUserService(userTokenRepository, userEmailRepository, notificationClient, telemetryClient, verifyEmailService, authUserGroupService, "licences");
    }

    @Test
    public void createUser_usernameLength() {
        assertThatThrownBy(() -> authUserService.createUser("user", "email", "first", "last", null, "url", "bob", GRANTED_AUTHORITY_SUPER_USER)).
                isInstanceOf(CreateUserException.class).hasMessage("Create user failed for field username with reason: length");
    }

    @Test
    public void createUser_usernameMaxLength() {
        assertThatThrownBy(() -> authUserService.createUser("ThisIsLongerThanTheAllowedUsernameLength", "email", "first", "last", null, "url", "bob", GRANTED_AUTHORITY_SUPER_USER)).
                isInstanceOf(CreateUserException.class).hasMessage("Create user failed for field username with reason: maxlength");
    }

    @Test
    public void createUser_usernameFormat() {
        assertThatThrownBy(() -> authUserService.createUser("user-name", "email", "first", "last", null, "url", "bob", GRANTED_AUTHORITY_SUPER_USER)).
                isInstanceOf(CreateUserException.class).hasMessage("Create user failed for field username with reason: format");
    }

    @Test
    public void createUser_firstNameLength() {
        assertThatThrownBy(() -> authUserService.createUser("userme", "email", "s", "last", null, "url", "bob", GRANTED_AUTHORITY_SUPER_USER)).
                isInstanceOf(CreateUserException.class).hasMessage("Create user failed for field firstName with reason: length");
    }

    @Test
    public void createUser_firstNameMaxLength() {
        assertThatThrownBy(() -> authUserService.createUser("userme", "email", "ThisFirstNameIsMoreThanFiftyCharactersInLengthAndInvalid", "last", null, "url", "bob", GRANTED_AUTHORITY_SUPER_USER)).
                isInstanceOf(CreateUserException.class).hasMessage("Create user failed for field firstName with reason: maxlength");
    }

    @Test
    public void createUser_lastNameLength() {
        assertThatThrownBy(() -> authUserService.createUser("userme", "email", "se", "x", null, "url", "bob", GRANTED_AUTHORITY_SUPER_USER)).
                isInstanceOf(CreateUserException.class).hasMessage("Create user failed for field lastName with reason: length");
    }

    @Test
    public void createUser_lastNameMaxLength() {
        assertThatThrownBy(() -> authUserService.createUser("userme", "email", "se", "ThisLastNameIsMoreThanFiftyCharactersInLengthAndInvalid", null, "url", "bob", GRANTED_AUTHORITY_SUPER_USER)).
                isInstanceOf(CreateUserException.class).hasMessage("Create user failed for field lastName with reason: maxlength");
    }

    @Test
    public void createUser_emailValidation() throws VerifyEmailException {
        doThrow(new VerifyEmailException("reason")).when(verifyEmailService).validateEmailAddress(anyString());
        assertThatThrownBy(() -> authUserService.createUser("userme", "email", "se", "xx", null, "url", "bob", GRANTED_AUTHORITY_SUPER_USER)).
                isInstanceOf(VerifyEmailException.class).hasMessage("Verify email failed with reason: reason");

        verify(verifyEmailService).validateEmailAddress("email");
    }

    @Test
    public void createUser_successLinkReturned() throws VerifyEmailException, CreateUserException, NotificationClientException {
        final var link = authUserService.createUser("userme", "email", "se", "xx", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        assertThat(link).startsWith("url?token=").hasSize("url?token=".length() + 36);
    }

    @Test
    public void createUser_trackSuccess() throws VerifyEmailException, CreateUserException, NotificationClientException {
        authUserService.createUser("userme", "email", "se", "xx", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);
        verify(telemetryClient).trackEvent("AuthUserCreateSuccess", Map.of("username", "USERME", "admin", "bob"), null);
    }

    @Test
    public void createUser_saveTokenRepository() throws VerifyEmailException, CreateUserException, NotificationClientException {
        final var link = authUserService.createUser("userme", "email", "se", "xx", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        final var captor = ArgumentCaptor.forClass(UserToken.class);
        verify(userTokenRepository).save(captor.capture());

        assertThat(captor.getValue().getTokenType()).isEqualTo(TokenType.RESET);
        assertThat(captor.getValue().getToken()).isEqualTo(link.substring("url?token=".length()));
        assertThat(captor.getValue().getTokenExpiry()).isBetween(LocalDateTime.now().plusDays(6), LocalDateTime.now().plusDays(8));
    }

    @Test
    public void createUser_saveEmailRepository() throws VerifyEmailException, CreateUserException, NotificationClientException {
        authUserService.createUser("userMe", "eMail", "first", "last", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

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
    public void createUser_setGroup() throws VerifyEmailException, CreateUserException, NotificationClientException {
        when(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(List.of(new Group("SITE_1_GROUP_1", "desc")));
        authUserService.createUser("userMe", "eMail", "first", "last", "SITE_1_GROUP_1", "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        final var captor = ArgumentCaptor.forClass(UserEmail.class);
        verify(userEmailRepository).save(captor.capture());

        final var user = captor.getValue();
        assertThat(user.getGroups()).extracting(Group::getGroupCode).containsOnly("SITE_1_GROUP_1");
    }

    @Test
    public void createUser_noRoles() throws VerifyEmailException, CreateUserException, NotificationClientException {
        when(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(List.of(new Group("SITE_1_GROUP_1", "desc")));
        authUserService.createUser("userMe", "eMail", "first", "last", "SITE_1_GROUP_1", "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        final var captor = ArgumentCaptor.forClass(UserEmail.class);
        verify(userEmailRepository).save(captor.capture());

        final var user = captor.getValue();
        assertThat(user.getAuthorities()).isEmpty();
    }

    @Test
    public void createUser_setRoles() throws VerifyEmailException, CreateUserException, NotificationClientException {
        final var group = new Group("SITE_1_GROUP_1", "desc");
        group.setAssignableRoles(Set.of(
                new GroupAssignableRole(new Authority("AUTH_AUTO", "Auth Name"), group, true),
                new GroupAssignableRole(new Authority("AUTH_MANUAL", "Auth Name"), group, false)));
        when(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(List.of(group));
        authUserService.createUser("userMe", "eMail", "first", "last", "SITE_1_GROUP_1", "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        final var captor = ArgumentCaptor.forClass(UserEmail.class);
        verify(userEmailRepository).save(captor.capture());

        final var user = captor.getValue();
        assertThat(user.getAuthorities()).extracting(Authority::getRoleCode).containsOnly("AUTH_AUTO");
    }

    @Test
    public void createUser_wrongGroup() {
        when(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(List.of(new Group("OTHER_GROUP", "desc")));
        assertThatThrownBy(() -> authUserService.createUser("userMe", "eMail", "first", "last", "SITE_2_GROUP_1", "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER))
                .isInstanceOf(CreateUserException.class).hasMessage("Create user failed for field groupCode with reason: notfound");
    }

    @Test
    public void createUser_missingGroup() {
        assertThatThrownBy(() -> authUserService.createUser("userMe", "eMail", "first", "last", "", "url?token=", "bob", Set.of()))
                .isInstanceOf(CreateUserException.class).hasMessage("Create user failed for field groupCode with reason: missing");
    }

    @Test
    public void createUser_callNotify() throws VerifyEmailException, CreateUserException, NotificationClientException {
        final var link = authUserService.createUser("userme", "email", "first", "last", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        verify(notificationClient).sendEmail("licences", "email", Map.of("resetLink", link, "firstName", "first"), null);
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
    public void amendUser_successLinkReturned() throws VerifyEmailException, NotificationClientException, AmendUserException {
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
    public void amendUser_verifiedEmail() {
        final var user = UserEmail.of("SOMEUSER");
        user.setVerified(true);
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> authUserService.amendUser("userme", "email", "url?token=", "bob")).
                isInstanceOf(AmendUserException.class).
                hasMessageContaining("reason: notinitial");
    }

    @Test
    public void amendUser_passwordSet() {
        final var user = UserEmail.of("SOMEUSER");
        user.setPassword("some pass");
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> authUserService.amendUser("userme", "email", "url?token=", "bob")).
                isInstanceOf(AmendUserException.class).
                hasMessageContaining("reason: notinitial");
    }

    private Optional<UserEmail> createUserEmailUser() {
        return Optional.of(UserEmail.of("SOMEUSER"));
    }

    @Test
    public void findAuthUsers() {
        final var unpaged = Pageable.unpaged();
        authUserService.findAuthUsers("somename ", "somerole  ", "  somegroup", unpaged);
        final var captor = ArgumentCaptor.forClass(UserEmailFilter.class);
        verify(userEmailRepository).findAll(captor.capture(), eq(unpaged));
        assertThat(captor.getValue()).extracting("name", "roleCode", "groupCode").containsExactly("somename", "somerole", "somegroup");
    }
}
