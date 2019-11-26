package uk.gov.justice.digital.hmpps.oauth2server.verify;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.security.NomisUserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException;
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
public class VerifyEmailServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserTokenRepository userTokenRepository;
    @Mock
    private NomisUserService nomisUserService;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private TelemetryClient telemetryClient;
    @Mock
    private NotificationClientApi notificationClient;
    @Mock
    private ReferenceCodesService referenceCodesService;
    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    private VerifyEmailService verifyEmailService;

    @Before
    public void setUp() {
        verifyEmailService = new VerifyEmailService(userRepository, userTokenRepository, nomisUserService, jdbcTemplate, telemetryClient, notificationClient, referenceCodesService, "templateId");
    }

    @Test
    public void getEmail() {
        final var user = User.builder().username("bob").email("joe@bob.com").build();
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        final var userOptional = verifyEmailService.getEmail("user");
        assertThat(userOptional).get().isEqualTo(user);
    }

    @Test
    public void getEmail_NoEmailSet() {
        final var user = User.of("bob");
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        final var userOptionalOptional = verifyEmailService.getEmail("user");
        assertThat(userOptionalOptional).isEmpty();
    }

    @Test
    public void isNotVerified_userMissing() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        assertThat(verifyEmailService.isNotVerified("user")).isTrue();
        verify(userRepository).findByUsername("user");
    }

    @Test
    public void isNotVerified_userFoundNotVerified() {
        final var user = User.of("bob");
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        assertThat(verifyEmailService.isNotVerified("user")).isTrue();
    }

    @Test
    public void isNotVerified_userFoundVerified() {
        final var user = User.builder().username("bob").email("joe@bob.com").build();
        user.setVerified(true);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        assertThat(verifyEmailService.isNotVerified("user")).isFalse();
    }

    @Test
    public void requestVerification_firstNameMissing() throws NotificationClientException, VerifyEmailException {
        when(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(Boolean.TRUE);
        final var verification = verifyEmailService.requestVerification("user", "email@john.com", "url");
        verify(notificationClient).sendEmail(eq("templateId"), eq("email@john.com"), mapCaptor.capture(), eq(null));
        final var params = mapCaptor.getValue();
        assertThat(params).containsEntry("firstName", "user").containsEntry("verifyLink", verification);
    }

    @Test
    public void requestVerification_firstNamePresent() throws NotificationClientException, VerifyEmailException {
        when(nomisUserService.getNomisUserByUsername(anyString())).thenReturn(Optional.of(getStaffUserAccountForBob()));
        when(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(Boolean.TRUE);
        final var verification = verifyEmailService.requestVerification("user", "eMail@john.COM", "url");
        verify(notificationClient).sendEmail(eq("templateId"), eq("email@john.com"), mapCaptor.capture(), eq(null));
        final var params = mapCaptor.getValue();
        assertThat(params).containsEntry("firstName", "Bob").containsEntry("verifyLink", verification);
    }

    @Test
    public void requestVerification_existingToken() throws NotificationClientException, VerifyEmailException {
        final var user = User.of("someuser");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        final var existingUserToken = user.createToken(TokenType.VERIFIED);
        when(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(Boolean.TRUE);
        verifyEmailService.requestVerification("user", "email@john.com", "url");
        assertThat(user.getTokens()).hasSize(1).extracting(UserToken::getToken).doesNotContain(existingUserToken.getToken());
    }

    @Test
    public void requestVerification_verifyToken() throws NotificationClientException, VerifyEmailException {
        final var user = User.of("someuser");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(Boolean.TRUE);
        final var verification = verifyEmailService.requestVerification("user", "email@john.com", "url");
        final var value = user.getTokens().stream().findFirst().orElseThrow();
        assertThat(verification).isEqualTo("url" + value.getToken());
    }

    @Test
    public void requestVerification_saveEmail() throws NotificationClientException, VerifyEmailException {
        final var user = User.of("someuser");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(Boolean.TRUE);
        verifyEmailService.requestVerification("user", "eMail@john.COM", "url");
        verify(userRepository).save(user);
        assertThat(user.getEmail()).isEqualTo("email@john.com");
        assertThat(user.isVerified()).isFalse();
    }

    @Test
    public void requestVerification_sendFailure() throws NotificationClientException {
        when(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(Boolean.TRUE);
        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), isNull())).thenThrow(new NotificationClientException("message"));

        assertThatThrownBy(() -> verifyEmailService.requestVerification("user", "email@john.com", "url")).hasMessage("message");
    }

    @Test
    public void requestVerification_formatEmailInput() throws NotificationClientException, VerifyEmailException {
        when(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(Boolean.TRUE);
        verifyEmailService.requestVerification("user", "some.u’ser@SOMEwhere.COM", "url");
        verify(notificationClient).sendEmail(eq("templateId"), eq("some.u'ser@somewhere.com"), anyMap(), eq(null));
    }

    @Test
    public void verifyEmail_NoAtSign() {
        verifyEmailFailure("a", "format");
    }

    @Test
    public void verifyEmail_MultipleAtSigns() {
        verifyEmailFailure("a@b.fred@joe.com", "at");
    }

    @Test
    public void verifyEmail_NoExtension() {
        verifyEmailFailure("a@bee", "format");
    }

    @Test
    public void verifyEmail_FirstLastStopFirst() {
        verifyEmailFailure(".a@bee.com", "firstlast");
    }

    @Test
    public void verifyEmail_FirstLastStopLast() {
        verifyEmailFailure("a@bee.com.", "firstlast");
    }

    @Test
    public void verifyEmail_FirstLastAtFirst() {
        verifyEmailFailure("@a@bee.com", "firstlast");
    }

    @Test
    public void verifyEmail_FirstLastAtLast() {
        verifyEmailFailure("a@bee.com@", "firstlast");
    }

    @Test
    public void verifyEmail_TogetherAtBefore() {
        verifyEmailFailure("a@.com", "together");
    }

    @Test
    public void verifyEmail_TogetherAtAfter() {
        verifyEmailFailure("a.@joe.com", "together");
    }

    @Test
    public void verifyEmail_White() {
        verifyEmailFailure("a@be\te.com", "white");
    }

    @Test
    public void verifyEmail_InvalidDomain() {
        verifyEmailFailure("a@b.com", "domain");
        verify(referenceCodesService).isValidEmailDomain("b.com");
    }

    @Test
    public void verifyEmail_InvalidCharacters() {
        verifyEmailFailure("a@b.&com", "characters");
    }

    private void verifyEmailFailure(final String email, final String reason) {
        assertThatThrownBy(() -> verifyEmailService.validateEmailAddress(email)).
                isInstanceOf(VerifyEmailException.class).extracting("reason").isEqualTo(reason);
    }

    private StaffUserAccount getStaffUserAccountForBob() {
        final var staffUserAccount = new StaffUserAccount();
        final var staff = new Staff();
        staff.setFirstName("bOb");
        staffUserAccount.setStaff(staff);
        return staffUserAccount;
    }

    @Test
    public void confirmEmail_happyPath() {
        final var user = User.of("bob");
        final var userToken = user.createToken(TokenType.VERIFIED);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        final var result = verifyEmailService.confirmEmail("token");

        assertThat(result).isEmpty();
        verify(userRepository).save(user);
        assertThat(user.isVerified()).isTrue();
    }

    @Test
    public void confirmEmail_invalid() {
        final var result = verifyEmailService.confirmEmail("bob");
        assertThat(result).get().isEqualTo("invalid");
    }

    @Test
    public void confirmEmail_expired() {
        final var user = User.of("bob");
        final var userToken = user.createToken(TokenType.VERIFIED);
        userToken.setTokenExpiry(LocalDateTime.now().minusSeconds(1));
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        final var result = verifyEmailService.confirmEmail("token");
        assertThat(result).get().isEqualTo("expired");
    }
}
