package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import org.assertj.core.data.MapEntry;
import org.assertj.core.util.Arrays;
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
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.security.PasswordValidationFailureException;
import uk.gov.justice.digital.hmpps.oauth2server.security.ReusedPasswordException;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AbstractPasswordControllerTest {
    @Mock
    private ResetPasswordService resetPasswordService;
    @Mock
    private TokenService tokenService;
    @Mock
    private UserService userService;
    @Mock
    private VerifyEmailService verifyEmailService;
    @Mock
    private TelemetryClient telemetryClient;
    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    private ResetPasswordController controller;

    @Before
    public void setUp() {
        controller = new ResetPasswordController(resetPasswordService, tokenService, userService, verifyEmailService, telemetryClient, true, Set.of("password1"));
    }

    @Test
    public void setPassword_Success() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile("TAG_ADMIN");
        final var modelAndView = controller.setPassword("d", "password123456", "password123456", null);
        assertThat(modelAndView.getViewName()).isEqualTo("redirect:/reset-password-success");
    }

    @Test
    public void setPassword_Success_Telemetry() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile("TAG_ADMIN");
        controller.setPassword("d", "password123456", "password123456", null);
        verify(telemetryClient).trackEvent(eq("ResetPasswordSuccess"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).containsExactly(entry("username", "user"));
    }

    @Test
    public void setPassword_InitialSuccess_Telemetry() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile("TAG_ADMIN");
        controller.setPassword("d", "password123456", "password123456", true);
        verify(telemetryClient).trackEvent(eq("InitialPasswordSuccess"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).containsExactly(entry("username", "user"));
    }

    @Test
    public void setPassword_Failure() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"));
        final var modelAndView = controller.setPassword("sometoken", "new", "confirm", null);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("error", "expired"));
    }

    @Test
    public void setPassword_NotAlphanumeric() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var modelAndView = controller.setPassword("d", "@fewfewfew1", "@fewfewfew1", null);
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "d"), entry("isAdmin", Boolean.FALSE), entry("error", Boolean.TRUE), listEntry("errornew", "alphanumeric"));
    }

    @Test
    public void setPassword_NotAlphanumeric_Telemetry() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        controller.setPassword("d", "@fewfewfew1", "@fewfewfew1", null);
        verify(telemetryClient).trackEvent(eq("ResetPasswordFailure"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).containsOnly(entry("username", "user"), entry("reason", "{errornew=[alphanumeric]}"));
    }

    @Test
    public void setPassword_NewBlank() {
        setupCheckAndGetTokenValid();
        final var modelAndView = controller.setPassword("d", "", "", null);
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "d"), entry("isAdmin", Boolean.FALSE), entry("error", Boolean.TRUE), listEntry("errornew", "newmissing"), listEntry("errorconfirm", "confirmmissing"));
    }

    @Test
    public void setPassword_ConfirmNewBlank() {
        setupCheckAndGetTokenValid();
        final var modelAndView = controller.setPassword("d", "a", "", null);
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "d"), entry("isAdmin", Boolean.FALSE), entry("error", Boolean.TRUE), listEntry("errorconfirm", "confirmmissing"));
    }

    @Test
    public void setPassword_Length() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var modelAndView = controller.setPassword("d", "qwerqw12", "qwerqw12", null);
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "d"), entry("isAdmin", Boolean.FALSE), entry("error", Boolean.TRUE), listEntry("errornew", "length9"));
    }

    @Test
    public void setPassword_LengthAdmin() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile("TAG_ADMIN");
        final var modelAndView = controller.setPassword("d", "qwerqwerqwe12", "qwerqwerqwe12", null);
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "d"), entry("isAdmin", Boolean.TRUE), entry("error", Boolean.TRUE), listEntry("errornew", "length14"));
    }

    @Test
    public void setPassword_TooLong() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var modelAndView = controller.setPassword("d", "abcdefghij123456789012345678901", "abcdefghij123456789012345678901", null);
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "d"), entry("isAdmin", Boolean.FALSE), entry("error", Boolean.TRUE), listEntry("errornew", "long"));
    }

    @Test
    public void setPassword_Blacklist() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var modelAndView = controller.setPassword("token", "passWORD1", "passWORD1", null);
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "token"), entry("isAdmin", Boolean.FALSE), entry("error", Boolean.TRUE), listEntry("errornew", "blacklist"));
    }

    @Test
    public void setPassword_ContainsUsername() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var modelAndView = controller.setPassword("token", "someuser12", "someuser12", null);
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "token"), entry("isAdmin", Boolean.FALSE), entry("error", Boolean.TRUE), listEntry("errornew", "username"));
    }

    @Test
    public void setPassword_FourDistinct() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var modelAndView = controller.setPassword("d", "as1as1as1", "as1as1as1", null);
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "d"), entry("isAdmin", Boolean.FALSE), entry("error", Boolean.TRUE), listEntry("errornew", "four"));
    }

    @Test
    public void setPassword_MissingDigits() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var modelAndView = controller.setPassword("daaa", "asdasdasdb", "asdasdasdb", null);
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "daaa"), entry("isAdmin", Boolean.FALSE), entry("error", Boolean.TRUE), listEntry("errornew", "nodigits"));
    }

    @Test
    public void setPassword_OnlyDigits() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var modelAndView = controller.setPassword("d", "1231231234", "1231231234", null);
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "d"), entry("isAdmin", Boolean.FALSE), entry("error", Boolean.TRUE), listEntry("errornew", "alldigits"));
    }

    @Test
    public void setPassword_MultipleFailures() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var modelAndView = controller.setPassword("user", "password", "new", null);
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "user"), entry("isAdmin", Boolean.FALSE), entry("error", Boolean.TRUE), listEntry("errorconfirm", "mismatch"), listEntry("errornew", "nodigits", "length9"));
    }

    @Test
    public void setPassword_Mismatch() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var modelAndView = controller.setPassword("user", "password2", "new", null);
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "user"), entry("isAdmin", Boolean.FALSE), entry("error", Boolean.TRUE), listEntry("errorconfirm", "mismatch"));
    }

    @Test
    public void setPassword_ValidationFailure() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        doThrow(new PasswordValidationFailureException()).when(resetPasswordService).setPassword(anyString(), anyString());
        final var modelAndView = controller.setPassword("user", "password2", "password2", null);
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "user"), entry("isAdmin", Boolean.FALSE), entry("error", Boolean.TRUE), entry("errornew", "validation"));
    }

    @Test
    public void setPassword_OtherException() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var exception = new RuntimeException();
        doThrow(exception).when(resetPasswordService).setPassword(anyString(), anyString());
        assertThatThrownBy(
                () -> controller.setPassword("user", "password2", "password2", null)
        ).isEqualTo(exception);
    }

    @Test
    public void setPassword_ReusedPassword() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        doThrow(new ReusedPasswordException()).when(resetPasswordService).setPassword(anyString(), anyString());
        final var modelAndView = controller.setPassword("user", "password2", "password2", null);
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "user"), entry("isAdmin", Boolean.FALSE), entry("error", Boolean.TRUE), entry("errornew", "reused"));
    }

    @Test
    public void setPassword_LockedAccount() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        doThrow(new LockedException("wrong")).when(resetPasswordService).setPassword(anyString(), anyString());
        final var modelAndView = controller.setPassword("user", "password2", "password2", null);
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "user"), entry("isAdmin", Boolean.FALSE), entry("error", Boolean.TRUE), entry("errornew", "state"));
    }

    private void setupCheckAndGetTokenValid() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty());
        when(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(new UserToken(TokenType.RESET, UserEmail.of("user"))));
    }

    private void setupGetUserCallForProfile(final String profile) {
        final var user = new StaffUserAccount();
        final var detail = new AccountDetail();
        detail.setProfile(profile);
        user.setAccountDetail(detail);
        when(userService.findUser(anyString())).thenReturn(Optional.of(user));
    }

    private MapEntry<String, List<Object>> listEntry(final String key, final Object... values) {
        return MapEntry.entry(key, Arrays.asList(values));
    }
}
