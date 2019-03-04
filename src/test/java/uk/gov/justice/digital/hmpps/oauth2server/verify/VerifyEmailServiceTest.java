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
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
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
    private UserEmailRepository userEmailRepository;
    @Mock
    private UserTokenRepository userTokenRepository;
    @Mock
    private UserService userService;
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
        verifyEmailService = new VerifyEmailService(userEmailRepository, userTokenRepository, userService, jdbcTemplate, telemetryClient, notificationClient, referenceCodesService, "templateId");
    }

    @Test
    public void getEmail() {
        final var userEmail = new UserEmail("bob", "joe@bob.com", false, false);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        final var userEmailOptional = verifyEmailService.getEmail("user");
        assertThat(userEmailOptional).get().isEqualTo(userEmail);
    }

    @Test
    public void getEmail_NoEmailSet() {
        final var userEmail = new UserEmail("bob");
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        final var userEmailOptional = verifyEmailService.getEmail("user");
        assertThat(userEmailOptional).isEmpty();
    }

    @Test
    public void isNotVerified_userMissing() {
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.empty());
        assertThat(verifyEmailService.isNotVerified("user")).isTrue();
        verify(userEmailRepository).findById("user");
    }

    @Test
    public void isNotVerified_userFoundNotVerified() {
        final var userEmail = new UserEmail("bob");
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        assertThat(verifyEmailService.isNotVerified("user")).isTrue();
    }

    @Test
    public void isNotVerified_userFoundVerified() {
        final var userEmail = new UserEmail("bob", "joe@bob.com", false, false);
        userEmail.setVerified(true);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
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
        when(userService.findUser(anyString())).thenReturn(Optional.of(getStaffUserAccountForBob()));
        when(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(Boolean.TRUE);
        final var verification = verifyEmailService.requestVerification("user", "email@john.com", "url");
        verify(notificationClient).sendEmail(eq("templateId"), eq("email@john.com"), mapCaptor.capture(), eq(null));
        final var params = mapCaptor.getValue();
        assertThat(params).containsEntry("firstName", "Bob").containsEntry("verifyLink", verification);
    }

    @Test
    public void requestVerification_existingToken() throws NotificationClientException, VerifyEmailException {
        final var userEmail = new UserEmail("someuser");
        when(userEmailRepository.findById("user")).thenReturn(Optional.of(userEmail));
        final var userToken = new UserToken(TokenType.VERIFIED, userEmail);
        when(userTokenRepository.findByTokenTypeAndUserEmail(any(), any())).thenReturn(Optional.of(userToken));
        when(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(Boolean.TRUE);
        verifyEmailService.requestVerification("user", "email@john.com", "url");
        verify(userTokenRepository).delete(userToken);
    }

    @Test
    public void requestVerification_verifyToken() throws NotificationClientException, VerifyEmailException {
        final var userEmail = new UserEmail("someuser");
        when(userEmailRepository.findById("user")).thenReturn(Optional.of(userEmail));
        final var captor = ArgumentCaptor.forClass(UserToken.class);
        when(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(Boolean.TRUE);
        final var verification = verifyEmailService.requestVerification("user", "email@john.com", "url");
        verify(userTokenRepository).save(captor.capture());
        final var value = captor.getValue();
        assertThat(verification).isEqualTo("url" + value.getToken());
    }

    @Test
    public void requestVerification_sendFailure() throws NotificationClientException {
        when(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(Boolean.TRUE);
        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), isNull())).thenThrow(new NotificationClientException("message"));

        assertThatThrownBy(() -> verifyEmailService.requestVerification("user", "email@john.com", "url")).hasMessage("message");
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
                isInstanceOf(VerifyEmailException.class).extracting("reason").containsOnly(reason);
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
        final var userEmail = new UserEmail("bob");
        final var userToken = new UserToken(TokenType.VERIFIED, userEmail);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        final var result = verifyEmailService.confirmEmail("token");

        assertThat(result).isEmpty();
        verify(userEmailRepository).save(userEmail);
        assertThat(userEmail.isVerified()).isTrue();
    }

    @Test
    public void confirmEmail_invalid() {
        final var result = verifyEmailService.confirmEmail("bob");
        assertThat(result).get().isEqualTo("invalid");
    }

    @Test
    public void confirmEmail_expired() {
        final var userEmail = new UserEmail("bob");
        final var userToken = new UserToken(TokenType.VERIFIED, userEmail);
        userToken.setTokenExpiry(LocalDateTime.now().minusSeconds(1));
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        final var result = verifyEmailService.confirmEmail("token");
        assertThat(result).get().isEqualTo("expired");
    }
}
