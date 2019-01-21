package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.security.ChangePasswordService;
import uk.gov.justice.digital.hmpps.oauth2server.security.PasswordValidationFailureException;
import uk.gov.justice.digital.hmpps.oauth2server.security.ReusedPasswordException;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordService;
import uk.gov.service.notify.NotificationClientException;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResetPasswordControllerTest {
    @Mock
    private ResetPasswordService resetPasswordService;
    @Mock
    private ChangePasswordService changePasswordService;
    @Mock
    private UserService userService;
    @Mock
    private TelemetryClient telemetryClient;
    @Mock
    private HttpServletRequest request;

    private ResetPasswordController controller;

    @Before
    public void setUp() {
        controller = new ResetPasswordController(resetPasswordService, changePasswordService, userService, telemetryClient, true);
    }

    @Test
    public void resetPasswordRequest() {
        assertThat(controller.resetPasswordRequest()).isEqualTo("resetPassword");
    }

    @Test
    public void resetPasswordSuccess() {
        assertThat(controller.resetPasswordSuccess()).isEqualTo("resetPasswordSuccess");
    }

    @Test
    public void resetPasswordRequest_missing() {
        final var modelAndView = controller.resetPasswordRequest("   ", request);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPassword");
        assertThat(modelAndView.getModel()).containsExactly(entry("error", "missing"));
    }

    @Test
    public void resetPasswordRequest_successSmokeWithLink() throws NotificationClientException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.of("url"));
        final var modelAndView = controller.resetPasswordRequest("user", request);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPasswordSent");
        assertThat(modelAndView.getModel()).containsExactly(entry("resetLink", "url"));
    }

    @Test
    public void resetPasswordRequest_successSmokeNoLink() throws NotificationClientException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.empty());
        final var modelAndView = controller.resetPasswordRequest("user", request);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPasswordSent");
        assertThat(modelAndView.getModel()).containsExactly(entry("resetLinkMissing", Boolean.TRUE));
    }

    @Test
    public void resetPasswordRequest_success() throws NotificationClientException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.empty());
        final var modelAndView = new ResetPasswordController(resetPasswordService, changePasswordService, userService, telemetryClient, false).resetPasswordRequest("user", request);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPasswordSent");
        assertThat(modelAndView.getModel()).isEmpty();
    }

    @Test
    public void resetPasswordRequest_failed() throws NotificationClientException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenThrow(new NotificationClientException("failure message"));
        final var modelAndView = new ResetPasswordController(resetPasswordService, changePasswordService, userService, telemetryClient, false).resetPasswordRequest("user", request);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPassword");
        assertThat(modelAndView.getModel()).containsExactly(entry("error", "other"));
    }

    @Test
    public void resetPasswordConfirm_checkView() {
        setupCheckTokenValid();
        final var modelAndView = controller.resetPasswordConfirm("token");
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
    }

    @Test
    public void resetPasswordConfirm_checkModel() {
        setupCheckTokenValid();
        final var modelAndView = controller.resetPasswordConfirm("sometoken");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "sometoken"));
    }

    @Test
    public void resetPasswordConfirm_FailureCheckView() {
        when(resetPasswordService.checkToken(anyString())).thenReturn(Optional.of("invalid"));
        final var modelAndView = controller.resetPasswordConfirm("token");
        assertThat(modelAndView.getViewName()).isEqualTo("resetPassword");
    }

    @Test
    public void resetPasswordConfirm_FailureCheckModel() {
        when(resetPasswordService.checkToken(anyString())).thenReturn(Optional.of("expired"));
        final var modelAndView = controller.resetPasswordConfirm("sometoken");
        assertThat(modelAndView.getModel()).containsOnly(entry("error", "expired"));
    }

    @Test
    public void setPassword_Success() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile("TAG_ADMIN");
        final var modelAndView = controller.setPassword("d", "password123456", "password123456");
        assertThat(modelAndView.getViewName()).isEqualTo("redirect:/reset-password-success");
    }

    @Test
    public void setPassword_Failure() {
        when(resetPasswordService.checkToken(anyString())).thenReturn(Optional.of("expired"));
        final var modelAndView = controller.setPassword("sometoken", "new", "confirm");
        assertThat(modelAndView.getViewName()).isEqualTo("resetPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("error", "expired"));
    }

    @Test
    public void setPassword_NotAlphanumeric() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var modelAndView = controller.setPassword("d", "@fewfewfew1", "@fewfewfew1");
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(MapEntry.entry("token", "d"), MapEntry.entry("error", Boolean.TRUE), MapEntry.entry("errornew", "alphanumeric"));
    }

    @Test
    public void setPassword_NewBlank() {
        setupCheckAndGetTokenValid();
        final var modelAndView = controller.setPassword("d", "", "");
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "d"), entry("error", Boolean.TRUE), entry("errornew", "newmissing"), entry("errorconfirm", "confirmmissing"));
    }

    @Test
    public void setPassword_ConfirmNewBlank() {
        setupCheckAndGetTokenValid();
        final var modelAndView = controller.setPassword("d", "a", "");
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "d"), entry("error", Boolean.TRUE), entry("errorconfirm", "confirmmissing"));
    }

    @Test
    public void setPassword_Length() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var modelAndView = controller.setPassword("d", "qwerqw12", "qwerqw12");
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "d"), entry("error", Boolean.TRUE), entry("errornew", "length9"));
    }

    @Test
    public void setPassword_LengthAdmin() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile("TAG_ADMIN");
        final var modelAndView = controller.setPassword("d", "qwerqwerqwe12", "qwerqwerqwe12");
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "d"), entry("error", Boolean.TRUE), entry("errornew", "length14"));
    }

    @Test
    public void setPassword_ContainsUsername() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var modelAndView = controller.setPassword("token", "someuser12", "someuser12");
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "token"), entry("error", Boolean.TRUE), entry("errornew", "username"));
    }

    @Test
    public void setPassword_FourDistinct() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var modelAndView = controller.setPassword("d", "as1as1as1", "as1as1as1");
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "d"), entry("error", Boolean.TRUE), entry("errornew", "four"));
    }

    @Test
    public void setPassword_MissingDigits() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var modelAndView = controller.setPassword("daaa", "asdasdasdb", "asdasdasdb");
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "daaa"), entry("error", Boolean.TRUE), entry("errornew", "nodigits"));
    }

    @Test
    public void setPassword_OnlyDigits() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var modelAndView = controller.setPassword("d", "1231231234", "1231231234");
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "d"), entry("error", Boolean.TRUE), entry("errornew", "alldigits"));
    }

    @Test
    public void setPassword_Mismatch() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var modelAndView = controller.setPassword("user", "password1", "new");
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "user"), entry("error", Boolean.TRUE), entry("errorconfirm", "mismatch"));
    }

    @Test
    public void setPassword_ValidationFailure() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        doThrow(new PasswordValidationFailureException()).when(changePasswordService).changePasswordWithUnlock(anyString(), anyString());
        final var modelAndView = controller.setPassword("user", "password1", "password1");
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "user"), entry("error", Boolean.TRUE), entry("errornew", "validation"));
    }

    @Test
    public void setPassword_OtherException() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var exception = new RuntimeException();
        doThrow(exception).when(changePasswordService).changePasswordWithUnlock(anyString(), anyString());
        assertThatThrownBy(
                () -> controller.setPassword("user", "password1", "password1")
        ).isEqualTo(exception);
    }

    @Test
    public void setPassword_ReusedPassword() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        doThrow(new ReusedPasswordException()).when(changePasswordService).changePasswordWithUnlock(anyString(), anyString());
        final var modelAndView = controller.setPassword("user", "password1", "password1");
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "user"), entry("error", Boolean.TRUE), entry("errornew", "reused"));
    }


    private void setupCheckTokenValid() {
        when(resetPasswordService.checkToken(anyString())).thenReturn(Optional.empty());
    }

    private void setupCheckAndGetTokenValid() {
        when(resetPasswordService.checkToken(anyString())).thenReturn(Optional.empty());
        when(resetPasswordService.getToken(anyString())).thenReturn(Optional.of(new UserToken(TokenType.RESET, new UserEmail("user"))));
    }

    private void setupGetUserCallForProfile(final String profile) {
        final var user = Optional.of(new StaffUserAccount());
        final var detail = new AccountDetail();
        detail.setProfile(profile);
        user.get().setAccountDetail(detail);
        when(userService.getUserByUsername(anyString())).thenReturn(user);
    }
}
