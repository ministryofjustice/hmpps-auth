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
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.digital.hmpps.oauth2server.verify.UsernameTokenHelperTest.USER_TOKEN_BASE64;

@RunWith(MockitoJUnitRunner.class)
public class VerifyEmailServiceTest {
    @Mock
    private UserEmailRepository userEmailRepository;
    @Mock
    private UserService userService;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private TelemetryClient telemetryClient;
    @Mock
    private NotificationClientApi notificationClient;
    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    private VerifyEmailService verifyEmailService;

    @Before
    public void setUp() {
        verifyEmailService = new VerifyEmailService(userEmailRepository, userService, jdbcTemplate, telemetryClient, notificationClient, "templateId");
    }

    @Test
    public void getEmail() {
        final var userEmail = new UserEmail("bob");
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        final var userEmailOptional = verifyEmailService.getEmail("user");
        assertThat(userEmailOptional).get().isEqualTo(userEmail);
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
        final var userEmail = new UserEmail("bob");
        userEmail.setVerified(true);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        assertThat(verifyEmailService.isNotVerified("user")).isFalse();
    }

    @Test
    public void requestVerification_firstNameMissing() throws NotificationClientException {
        final var verification = verifyEmailService.requestVerification("user", "email", "url");
        verify(notificationClient).sendEmail(eq("templateId"), eq("email"), mapCaptor.capture(), eq(null));
        final var params = mapCaptor.getValue();
        assertThat(params).containsEntry("firstName", "user").containsEntry("verifyLink", verification);
    }

    @Test
    public void requestVerification_firstNamePresent() throws NotificationClientException {
        when(userService.getUserByUsername(anyString())).thenReturn(Optional.of(getStaffUserAccountForBob()));
        final var verification = verifyEmailService.requestVerification("user", "email", "url");
        verify(notificationClient).sendEmail(eq("templateId"), eq("email"), mapCaptor.capture(), eq(null));
        final var params = mapCaptor.getValue();
        assertThat(params).containsEntry("firstName", "bob").containsEntry("verifyLink", verification);
    }

    @Test
    public void requestVerification_verifyUserInToken() throws NotificationClientException {
        final var verification = verifyEmailService.requestVerification("user", "email", "url");
        assertThat(verification).startsWith("url/");
        final var usernameToken = new UsernameTokenHelper().readUsernameTokenFromEncodedString(verification.substring("url/".length()));
        assertThat(usernameToken).get().extracting("username").isEqualTo(Collections.singletonList("user"));
    }

    @Test
    public void requestVerification_verifyToken() throws NotificationClientException {
        final var userEmail = new UserEmail("someuser");
        when(userEmailRepository.findById("user")).thenReturn(Optional.of(userEmail));
        final var verification = verifyEmailService.requestVerification("user", "email", "url");
        final var usernameToken = new UsernameTokenHelper().readUsernameTokenFromEncodedString(verification);
        assertThat(usernameToken).get().extracting("token").isEqualTo(Collections.singletonList(userEmail.getVerificationToken()));
    }

    @Test
    public void requestVerification_sendFailure() throws NotificationClientException {
        when(notificationClient.sendEmail(anyString(), anyString(), anyMap(), isNull())).thenThrow(new NotificationClientException("message"));

        assertThatThrownBy(() -> verifyEmailService.requestVerification("user", "email", "url")).hasMessage("message");
    }

    private StaffUserAccount getStaffUserAccountForBob() {
        final var staffUserAccount = new StaffUserAccount();
        final var staff = new Staff();
        staff.setFirstName("bob");
        staffUserAccount.setStaff(staff);
        return staffUserAccount;
    }

    @Test
    public void confirmEmail_happyPath() {
        final var userEmail = new UserEmail("bob");
        userEmail.setVerificationToken("token");
        userEmail.setTokenExpiry(LocalDateTime.now().plusHours(1));
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        final var result = verifyEmailService.confirmEmail(USER_TOKEN_BASE64);

        assertThat(result).isEmpty();
        verify(userEmailRepository).save(userEmail);
        assertThat(userEmail.isVerified()).isTrue();
        assertThat(userEmail.getVerificationToken()).isNull();
        assertThat(userEmail.getTokenExpiry()).isNull();
    }

    @Test
    public void confirmEmail_userNotFound() {
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.empty());
        final var result = verifyEmailService.confirmEmail(USER_TOKEN_BASE64);
        assertThat(result).get().isEqualTo("notfound");
    }

    @Test
    public void confirmEmail_invalid() {
        final var result = verifyEmailService.confirmEmail("bob");
        assertThat(result).get().isEqualTo("invalid");
    }

    @Test
    public void confirmEmail_userAlreadyVerified() {
        final var userEmail = new UserEmail("bob");
        userEmail.setVerified(true);
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        final var result = verifyEmailService.confirmEmail(USER_TOKEN_BASE64);
        assertThat(result).get().isEqualTo("verifiedAlready");
    }

    @Test
    public void confirmEmail_tokenMismatch() {
        final var userEmail = new UserEmail("bob");
        userEmail.setVerificationToken("wrong");
        userEmail.setTokenExpiry(LocalDateTime.now().plusHours(1));
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        final var result = verifyEmailService.confirmEmail(USER_TOKEN_BASE64);
        assertThat(result).get().isEqualTo("tokenMismatch");
    }

    @Test
    public void confirmEmail_expired() {
        final var userEmail = new UserEmail("bob");
        userEmail.setVerificationToken("token");
        userEmail.setTokenExpiry(LocalDateTime.now().minusSeconds(1));
        when(userEmailRepository.findById(anyString())).thenReturn(Optional.of(userEmail));
        final var result = verifyEmailService.confirmEmail(USER_TOKEN_BASE64);
        assertThat(result).get().isEqualTo("expired");
    }
}
