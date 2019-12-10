package uk.gov.justice.digital.hmpps.oauth2server.maintain;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.*;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.OauthServiceRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService.AmendUserException;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService.CreateUserException;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource;
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck;
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException;
import uk.gov.justice.digital.hmpps.oauth2server.security.ReusedPasswordException;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthUserServiceTest {
    private static final Authentication PRINCIPAL = new UsernamePasswordAuthenticationToken("bob", "pass");
    private static final Set<GrantedAuthority> GRANTED_AUTHORITY_SUPER_USER = Set.of(new SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"));
    private static final Set<GrantedAuthority> SUPER_USER = Set.of(new SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"));
    private static final Set<GrantedAuthority> GROUP_MANAGER = Set.of(new SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"));

    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationClientApi notificationClient;
    @Mock
    private TelemetryClient telemetryClient;
    @Mock
    private VerifyEmailService verifyEmailService;
    @Mock
    private AuthUserGroupService authUserGroupService;
    @Mock
    private MaintainUserCheck maintainUserCheck;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OauthServiceRepository oauthServiceRepository;

    private AuthUserService authUserService;

    @Before
    public void setUp() {
        authUserService = new AuthUserService(userRepository, notificationClient, telemetryClient, verifyEmailService, authUserGroupService, maintainUserCheck, passwordEncoder, oauthServiceRepository, "licences", 90, 10);
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
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");
        final var link = authUserService.createUser("userme", "email", "se", "xx", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        assertThat(link).startsWith("url?token=").hasSize("url?token=".length() + 36);
    }

    @Test
    public void createUser_trackSuccess() throws VerifyEmailException, CreateUserException, NotificationClientException {
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");
        authUserService.createUser("userme", "email", "se", "xx", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        verify(telemetryClient).trackEvent("AuthUserCreateSuccess", Map.of("username", "USERME", "admin", "bob"), null);
    }

    @Test
    public void createUser_saveUserRepository() throws VerifyEmailException, CreateUserException, NotificationClientException {
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");
        final var link = authUserService.createUser("userme", "email", "se", "xx", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        final var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        final var user = captor.getValue();
        final var userToken = user.getTokens().stream().findFirst().orElseThrow();

        assertThat(userToken.getTokenType()).isEqualTo(TokenType.RESET);
        assertThat(userToken.getToken()).isEqualTo(link.substring("url?token=".length()));
        assertThat(userToken.getTokenExpiry()).isBetween(LocalDateTime.now().plusDays(6), LocalDateTime.now().plusDays(8));
    }

    @Test
    public void createUser_saveEmailRepository() throws VerifyEmailException, CreateUserException, NotificationClientException {
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");
        authUserService.createUser("userMe", "eMail", "first", "last", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        final var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

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
    public void createUser_formatEmailInput() throws VerifyEmailException, CreateUserException, NotificationClientException {
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");
        authUserService.createUser("userMe", "    SARAH.o’connor@gov.uk", "first", "last", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        final var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getEmail()).isEqualTo("sarah.o'connor@gov.uk");
    }

    @Test
    public void createUser_setGroup() throws VerifyEmailException, CreateUserException, NotificationClientException {
        when(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(List.of(new Group("SITE_1_GROUP_1", "desc")));
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");
        authUserService.createUser("userMe", "eMail", "first", "last", "SITE_1_GROUP_1", "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        final var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        final var user = captor.getValue();
        assertThat(user.getGroups()).extracting(Group::getGroupCode).containsOnly("SITE_1_GROUP_1");
    }

    @Test
    public void createUser_noRoles() throws VerifyEmailException, CreateUserException, NotificationClientException {
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");
        when(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(List.of(new Group("SITE_1_GROUP_1", "desc")));
        authUserService.createUser("userMe", "eMail", "first", "last", "SITE_1_GROUP_1", "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        final var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

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
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");
        authUserService.createUser("userMe", "eMail", "first", "last", "SITE_1_GROUP_1", "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        final var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

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
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");
        final var link = authUserService.createUser("userme", "email", "first", "last", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        verify(notificationClient).sendEmail("licences", "email", Map.of("resetLink", link, "firstName", "first", "supportLink", "nomis_support_link"), null);
    }

    @Test
    public void createUser_pecsUserGroupSupportLink() throws VerifyEmailException, CreateUserException, NotificationClientException {
        mockServiceOfNameWithSupportLink("BOOK_NOW", "book_now_support_link");
        when(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(List.of(new Group("PECS_GROUP", "desc")));

        authUserService.createUser("userMe", "eMail", "first", "last", "PECS_GROUP", "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        final var emailParamsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(anyString(), anyString(), emailParamsCaptor.capture(), any());
        assertThat(emailParamsCaptor.getValue().get("supportLink")).isEqualTo("book_now_support_link");
    }

    @Test
    public void createUser_nonPecsUserGroupSupportLink() throws VerifyEmailException, CreateUserException, NotificationClientException {
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");

        authUserService.createUser("userMe", "eMail", "first", "last", "", "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        final var emailParamsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(anyString(), anyString(), emailParamsCaptor.capture(), any());
        assertThat(emailParamsCaptor.getValue().get("supportLink")).isEqualTo("nomis_support_link");
    }

    @Test
    public void createUser_noGroupsSupportLink() throws VerifyEmailException, CreateUserException, NotificationClientException {
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");

        authUserService.createUser("userMe", "eMail", "first", "last", "", "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER);

        final var emailParamsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(anyString(), anyString(), emailParamsCaptor.capture(), any());
        assertThat(emailParamsCaptor.getValue().get("supportLink")).isEqualTo("nomis_support_link");
    }

    @Test
    public void amendUser_emailValidation() throws VerifyEmailException {
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUser());
        doThrow(new VerifyEmailException("reason")).when(verifyEmailService).validateEmailAddress(anyString());
        assertThatThrownBy(() -> authUserService.amendUser("userme", "email", "url?token=", "bob", PRINCIPAL.getAuthorities())).
                isInstanceOf(VerifyEmailException.class).hasMessage("Verify email failed with reason: reason");

        verify(verifyEmailService).validateEmailAddress("email");
    }

    @Test
    public void amendUser_groupValidation() throws AuthUserGroupRelationshipException {
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUser());
        doThrow(new AuthUserGroupRelationshipException("user", "reason")).when(maintainUserCheck).ensureUserLoggedInUserRelationship(anyString(), any(), any());
        assertThatThrownBy(() -> authUserService.amendUser("userme", "email", "url?token=", "bob", PRINCIPAL.getAuthorities())).
                isInstanceOf(AuthUserGroupRelationshipException.class).hasMessage("Unable to maintain user: user with reason: reason");
    }

    @Test
    public void amendUser_successLinkReturned() throws VerifyEmailException, NotificationClientException, AmendUserException, AuthUserGroupRelationshipException {
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUser());
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");
        final var link = authUserService.amendUser("userme", "email", "url?token=", "bob", PRINCIPAL.getAuthorities());

        assertThat(link).startsWith("url?token=").hasSize("url?token=".length() + 36);
    }

    @Test
    public void amendUser_trackSuccess() throws VerifyEmailException, AmendUserException, NotificationClientException, AuthUserGroupRelationshipException {
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUser());
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");
        authUserService.amendUser("userme", "email", "url?token=", "bob", PRINCIPAL.getAuthorities());

        verify(telemetryClient).trackEvent("AuthUserAmendSuccess", Map.of("username", "someuser", "admin", "bob"), null);
    }

    @Test
    public void amendUser_saveTokenRepository() throws VerifyEmailException, AmendUserException, NotificationClientException, AuthUserGroupRelationshipException {
        final var user = createUser();
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(user);
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");
        final var link = authUserService.amendUser("userme", "email", "url?token=", "bob", PRINCIPAL.getAuthorities());

        final var userToken = user.orElseThrow().getTokens().stream().findFirst().orElseThrow();
        assertThat(userToken.getTokenType()).isEqualTo(TokenType.RESET);
        assertThat(userToken.getToken()).isEqualTo(link.substring("url?token=".length()));
        assertThat(userToken.getTokenExpiry()).isBetween(LocalDateTime.now().plusDays(6), LocalDateTime.now().plusDays(8));
    }

    @Test
    public void amendUser_saveEmailRepository() throws VerifyEmailException, AmendUserException, NotificationClientException, AuthUserGroupRelationshipException {
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUser());
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");
        authUserService.amendUser("userMe", "eMail", "url?token=", "bob", PRINCIPAL.getAuthorities());

        final var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        final var user = captor.getValue();
        assertThat(user.getEmail()).isEqualTo("email");
    }

    @Test
    public void amendUser_formatEmailInput() throws VerifyEmailException, AmendUserException, NotificationClientException, AuthUserGroupRelationshipException {
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUser());
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");
        authUserService.amendUser("userMe", "    SARAH.o’connor@gov.uk", "url?token=", "bob", PRINCIPAL.getAuthorities());

        final var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getEmail()).isEqualTo("sarah.o'connor@gov.uk");
    }

    @Test
    public void amendUser_pecsUserGroupSupportLink() throws AmendUserException, AuthUserGroupRelationshipException, NotificationClientException, VerifyEmailException {
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userOfGroups("PECS_GROUP")));
        mockServiceOfNameWithSupportLink("BOOK_NOW", "book_now_support_link");

        authUserService.amendUser("ANY_USER_NAME", "ANY_USER-EMAIL", "ANY_URL", "ANY_ADMIN", GRANTED_AUTHORITY_SUPER_USER);

        final var emailParamsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(anyString(), anyString(), emailParamsCaptor.capture(), any());
        assertThat(emailParamsCaptor.getValue().get("supportLink")).isEqualTo("book_now_support_link");
    }

    @Test
    public void amendUser_nonPecsUserGroupSupportLink() throws AmendUserException, AuthUserGroupRelationshipException, NotificationClientException, VerifyEmailException {
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userOfGroups("NON_PECS_GROUP")));
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");

        authUserService.amendUser("ANY_USER_NAME", "ANY_USER-EMAIL", "ANY_URL", "ANY_ADMIN", GRANTED_AUTHORITY_SUPER_USER);

        final var emailParamsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(anyString(), anyString(), emailParamsCaptor.capture(), any());
        assertThat(emailParamsCaptor.getValue().get("supportLink")).isEqualTo("nomis_support_link");
    }

    @Test
    public void amendUser_onePecsGroupOfManySupportLink() throws AmendUserException, AuthUserGroupRelationshipException, NotificationClientException, VerifyEmailException {
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userOfGroups("NON_PECS_GROUP", "PECS_GROUP")));
        mockServiceOfNameWithSupportLink("BOOK_NOW", "book_now_support_link");

        authUserService.amendUser("ANY_USER_NAME", "ANY_USER-EMAIL", "ANY_URL", "ANY_ADMIN", GRANTED_AUTHORITY_SUPER_USER);

        final var emailParamsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(anyString(), anyString(), emailParamsCaptor.capture(), any());
        assertThat(emailParamsCaptor.getValue().get("supportLink")).isEqualTo("book_now_support_link");
    }

    @Test
    public void amendUser_noGroupSupportLink() throws AmendUserException, AuthUserGroupRelationshipException, NotificationClientException, VerifyEmailException {
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userOfGroups()));
        mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link");

        authUserService.amendUser("ANY_USER_NAME", "ANY_USER-EMAIL", "ANY_URL", "ANY_ADMIN", GRANTED_AUTHORITY_SUPER_USER);

        final var emailParamsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient).sendEmail(anyString(), anyString(), emailParamsCaptor.capture(), any());
        assertThat(emailParamsCaptor.getValue().get("supportLink")).isEqualTo("nomis_support_link");
    }

    private User userOfGroups(String... groupList) {
        final var USER_PERSON = new Person("ANY_FIRST_NAME", "ANY_LAST_NAME");
        Set<Group> groups = Arrays.stream(groupList).map(groupName -> new Group(groupName, "any desc")).collect(Collectors.toSet());
        return User.builder().groups(groups).email("ANY_EMAIL").person(USER_PERSON).username("ANY ANY").build();
    }

    private void mockServiceOfNameWithSupportLink(String serviceCode, String supportLink) {
        final var SERVICE = new Service(serviceCode, "service", "service", "ANY_ROLES", "ANY_URL", true, supportLink);
        when(oauthServiceRepository.findById(serviceCode)).thenReturn(Optional.of(SERVICE));
    }

    @Test
    public void getAuthUserByUsername() {
        final var createdUser = createUser();
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createdUser);

        final var user = authUserService.getAuthUserByUsername("   bob   ");

        assertThat(user).isPresent().get().isEqualTo(createdUser.orElseThrow());

        verify(userRepository).findByUsernameAndMasterIsTrue("BOB");
    }

    @Test
    public void findByEmailAndMasterIsTrue() {
        when(userRepository.findByEmailAndMasterIsTrueOrderByUsername(anyString())).thenReturn(List.of(User.of("someuser")));

        final var user = authUserService.findAuthUsersByEmail("  bob  ");

        assertThat(user).extracting(UserPersonDetails::getUsername).containsOnly("someuser");
    }

    @Test
    public void findAuthUsersByEmail_formatEmailAddress() {
        when(userRepository.findByEmailAndMasterIsTrueOrderByUsername(anyString())).thenReturn(List.of(User.of("someuser")));

        authUserService.findAuthUsersByEmail("  some.u’ser@SOMEwhere  ");

        verify(userRepository).findByEmailAndMasterIsTrueOrderByUsername("some.u'ser@somewhere");
    }

    @Test
    public void enableUser_superUser() throws MaintainUserCheck.AuthUserGroupRelationshipException {
        final var optionalUser = createUser();
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser);
        authUserService.enableUser("user", "admin", SUPER_USER);
        assertThat(optionalUser).get().extracting(User::isEnabled).isEqualTo(Boolean.TRUE);
        verify(userRepository).save(optionalUser.orElseThrow());
    }

    @Test
    public void enableUser_invalidGroup_GroupManager() throws MaintainUserCheck.AuthUserGroupRelationshipException {
        final var optionalUser = createUser();
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser);
        doThrow(new MaintainUserCheck.AuthUserGroupRelationshipException("someuser", "User not with your groups")).when(maintainUserCheck).ensureUserLoggedInUserRelationship(anyString(), anyCollection(), any(User.class));
        assertThatThrownBy(() -> authUserService.enableUser("someuser", "admin", GROUP_MANAGER)).
                isInstanceOf(MaintainUserCheck.AuthUserGroupRelationshipException.class).hasMessage("Unable to maintain user: someuser with reason: User not with your groups");
    }

    @Test
    public void enableUser_validGroup_groupManager() throws MaintainUserCheck.AuthUserGroupRelationshipException {
        final var user = User.of("user");
        final var group1 = new Group("group", "desc");
        user.setGroups(Set.of(group1, new Group("group2", "desc")));

        user.setAuthorities(new HashSet<>(List.of(new Authority("JOE", "bloggs"))));
        final var groupManager = User.of("groupManager");
        groupManager.setGroups(Set.of(new Group("group3", "desc"), group1));
        when(userRepository.findByUsernameAndMasterIsTrue(anyString()))
                .thenReturn(Optional.of(user));

        authUserService.enableUser("user", "admin", GROUP_MANAGER);

        assertThat(user).extracting(User::isEnabled).isEqualTo(Boolean.TRUE);
        verify(userRepository).save(user);
    }

    @Test
    public void enableUser_NotFound() {
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authUserService.enableUser("user", "admin", SUPER_USER)).isInstanceOf(EntityNotFoundException.class).hasMessageContaining("username user");
    }

    @Test
    public void enableUser_trackEvent() throws MaintainUserCheck.AuthUserGroupRelationshipException {
        final var optionalUser = createUser();
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser);
        authUserService.enableUser("someuser", "someadmin", SUPER_USER);
        verify(telemetryClient).trackEvent("AuthUserChangeEnabled", Map.of("username", "someuser", "admin", "someadmin", "enabled", "true"), null);
    }

    @Test
    public void enableUser_setLastLoggedIn() throws MaintainUserCheck.AuthUserGroupRelationshipException {
        final var optionalUser = createUser();
        final var user = optionalUser.orElseThrow();
        final var tooLongAgo = LocalDateTime.now().minusDays(95);
        user.setLastLoggedIn(tooLongAgo);
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser);
        authUserService.enableUser("someuser", "someadmin", SUPER_USER);
        assertThat(user.getLastLoggedIn()).isBetween(LocalDateTime.now().minusDays(84), LocalDateTime.now().minusDays(82));
    }

    @Test
    public void enableUser_leaveLastLoggedInAlone() throws MaintainUserCheck.AuthUserGroupRelationshipException {
        final var optionalUser = createUser();
        final var user = optionalUser.orElseThrow();
        final var fiveDaysAgo = LocalDateTime.now().minusDays(5);
        user.setLastLoggedIn(fiveDaysAgo);
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser);
        authUserService.enableUser("someuser", "someadmin", SUPER_USER);
        assertThat(user.getLastLoggedIn()).isEqualTo(fiveDaysAgo);
    }

    @Test
    public void disableUser_superUser() throws MaintainUserCheck.AuthUserGroupRelationshipException {
        final var optionalUser = createUser();
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser);
        authUserService.disableUser("user", "admin", SUPER_USER);
        assertThat(optionalUser).get().extracting(User::isEnabled).isEqualTo(Boolean.FALSE);
        verify(userRepository).save(optionalUser.orElseThrow());
    }

    @Test
    public void disableUser_invalidGroup_GroupManager() throws MaintainUserCheck.AuthUserGroupRelationshipException {
        final var optionalUser = createUser();
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser);
        doThrow(new MaintainUserCheck.AuthUserGroupRelationshipException("someuser", "User not with your groups")).when(maintainUserCheck).ensureUserLoggedInUserRelationship(anyString(), anyCollection(), any(User.class));
        assertThatThrownBy(() -> authUserService.disableUser("someuser", "admin", GROUP_MANAGER)).
                isInstanceOf(MaintainUserCheck.AuthUserGroupRelationshipException.class).hasMessage("Unable to maintain user: someuser with reason: User not with your groups");
    }

    @Test
    public void disableUser_validGroup_groupManager() throws MaintainUserCheck.AuthUserGroupRelationshipException {
        final var user = User.of("user");
        final var group1 = new Group("group", "desc");
        user.setGroups(Set.of(group1, new Group("group2", "desc")));
        user.setEnabled(true);

        user.setAuthorities(new HashSet<>(List.of(new Authority("JOE", "bloggs"))));
        final var groupManager = User.of("groupManager");
        groupManager.setGroups(Set.of(new Group("group3", "desc"), group1));
        when(userRepository.findByUsernameAndMasterIsTrue(anyString()))
                .thenReturn(Optional.of(user));

        authUserService.disableUser("user", "admin", GROUP_MANAGER);

        assertThat(user).extracting(User::isEnabled).isEqualTo(Boolean.FALSE);
        verify(userRepository).save(user);
    }

    @Test
    public void disableUser_trackEvent() throws MaintainUserCheck.AuthUserGroupRelationshipException {
        final var optionalUser = createUser();
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser);
        authUserService.disableUser("someuser", "someadmin", SUPER_USER);
        verify(telemetryClient).trackEvent("AuthUserChangeEnabled", Map.of("username", "someuser", "admin", "someadmin", "enabled", "false"), null);
    }

    @Test
    public void disableUser_notFound() {
        when(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authUserService.disableUser("user", "admin", SUPER_USER)).isInstanceOf(EntityNotFoundException.class).hasMessageContaining("username user");
    }

    private Optional<User> createUser() {
        return Optional.of(User.of("someuser"));
    }

    @Test
    public void findAuthUsers() {
        final var unpaged = Pageable.unpaged();
        authUserService.findAuthUsers("somename ", "somerole  ", "  somegroup", unpaged);
        final var captor = ArgumentCaptor.forClass(UserFilter.class);
        verify(userRepository).findAll(captor.capture(), eq(unpaged));
        assertThat(captor.getValue()).extracting("name", "roleCode", "groupCode").containsExactly("somename", "somerole", "somegroup");
    }

    @Test
    public void lockUser_alreadyExists() {
        final var user = User.builder().username("user").build();
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));

        authUserService.lockUser(user);

        assertThat(user.isLocked()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    public void lockUser_newUser() {
        final var user = getStaffUserAccountForBob();
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        authUserService.lockUser(user);

        final var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        final var savedUser = captor.getValue();
        assertThat(savedUser.isLocked()).isTrue();
        assertThat(savedUser.getUsername()).isEqualTo("bob");
        assertThat(savedUser.getSource()).isEqualTo(AuthSource.nomis);
    }

    @Test
    public void unlockUser_alreadyExists() {
        final var user = User.builder().username("user").locked(true).build();
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));

        authUserService.unlockUser(user);

        assertThat(user.isLocked()).isFalse();
        assertThat(user.isVerified()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    public void unlockUser_newUser() {
        final var user = getStaffUserAccountForBob();
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        authUserService.unlockUser(user);

        final var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        final var savedUser = captor.getValue();
        assertThat(savedUser.isLocked()).isFalse();
        assertThat(savedUser.isVerified()).isTrue();
        assertThat(savedUser.getUsername()).isEqualTo("bob");
        assertThat(savedUser.getSource()).isEqualTo(AuthSource.nomis);
    }

    @Test
    public void changePassword() {
        final var user = User.builder().username("user").build();
        when(passwordEncoder.encode(anyString())).thenReturn("hashedpassword");

        authUserService.changePassword(user, "pass");

        assertThat(user.getPassword()).isEqualTo("hashedpassword");
        assertThat(user.getPasswordExpiry()).isAfterOrEqualTo(LocalDateTime.now().plusDays(9));
        verify(passwordEncoder).encode("pass");
    }

    @Test
    public void changePassword_PasswordSameAsCurrent() {
        final var user = User.builder().username("user").build();
        user.setPassword("oldencryptedpassword");
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(Boolean.TRUE);

        assertThatThrownBy(() -> authUserService.changePassword(user, "pass")).isInstanceOf(ReusedPasswordException.class);

        verify(passwordEncoder).matches("pass", "oldencryptedpassword");
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
}
