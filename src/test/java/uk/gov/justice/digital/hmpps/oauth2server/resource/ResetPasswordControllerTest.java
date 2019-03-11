package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import com.weddini.throttling.ThrottlingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordServiceImpl.NotificationClientRuntimeException;
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordServiceImpl.ResetPasswordException;
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException;
import uk.gov.service.notify.NotificationClientException;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ResetPasswordControllerTest {
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
    @Mock
    private HttpServletRequest request;
    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    private ResetPasswordController controller;

    @Before
    public void setUp() {
        controller = new ResetPasswordController(resetPasswordService, tokenService, userService, verifyEmailService, telemetryClient, true, Set.of("password1"));
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
    public void resetPasswordRequest_successSmokeWithLink() throws NotificationClientRuntimeException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.of("url"));
        final var modelAndView = controller.resetPasswordRequest("user", request);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPasswordSent");
        assertThat(modelAndView.getModel()).containsExactly(entry("resetLink", "url"));
    }

    @Test
    public void resetPasswordRequest_successSmokeNoLink() throws NotificationClientRuntimeException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.empty());
        final var modelAndView = controller.resetPasswordRequest("user", request);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPasswordSent");
        assertThat(modelAndView.getModel()).containsExactly(entry("resetLinkMissing", Boolean.TRUE));
    }

    @Test
    public void resetPasswordRequest_successNoLinkTelemetry() throws NotificationClientRuntimeException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.empty());
        controller.resetPasswordRequest("user", request);
        verify(telemetryClient).trackEvent(eq("ResetPasswordRequestFailure"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).containsOnly(entry("username", "user"), entry("error", "nolink"));
    }

    @Test
    public void resetPasswordRequest_successVerifyServiceCall() throws NotificationClientRuntimeException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.empty());
        controller.resetPasswordRequest("user", request);
        verify(resetPasswordService).requestResetPassword("user", "someurl");
    }

    @Test
    public void resetPasswordRequest_successLinkTelemetry() throws NotificationClientRuntimeException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.of("somelink"));
        controller.resetPasswordRequest("user", request);
        verify(telemetryClient).trackEvent(eq("ResetPasswordRequestSuccess"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).containsOnly(entry("username", "user"));
    }

    @Test
    public void resetPasswordRequest_success() throws NotificationClientRuntimeException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.empty());
        final var modelAndView = new ResetPasswordController(resetPasswordService, tokenService, userService, verifyEmailService, telemetryClient, false, null).resetPasswordRequest("user", request);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPasswordSent");
        assertThat(modelAndView.getModel()).isEmpty();
    }

    @Test
    public void resetPasswordRequest_failed() throws NotificationClientRuntimeException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenThrow(new NotificationClientRuntimeException(new NotificationClientException("failure message")));
        final var modelAndView = controller.resetPasswordRequest("user", request);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPassword");
        assertThat(modelAndView.getModel()).containsExactly(entry("error", "other"));
    }

    @Test
    public void resetPasswordRequest_emailfailed() throws NotificationClientRuntimeException, VerifyEmailException {
        doThrow(new VerifyEmailException("reason")).when(verifyEmailService).validateEmailAddress(anyString());
        final var modelAndView = controller.resetPasswordRequest("user@somewhere", request);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("error", "email.reason"), entry("usernameOrEmail", "user@somewhere"));
    }

    @Test
    public void resetPasswordRequest_throttled() throws NotificationClientRuntimeException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenThrow(new ThrottlingException());
        final var modelAndView = controller.resetPasswordRequest("user", request);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPassword");
        assertThat(modelAndView.getModel()).containsExactly(entry("error", "throttled"));
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
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "sometoken"), entry("isAdmin", Boolean.FALSE));
    }

    @Test
    public void resetPasswordConfirm_checkModelAdminUser() {
        final var user = setupGetUserCallForProfile();
        user.getAccountDetail().setProfile("TAG_ADMIN");
        setupCheckAndGetTokenValid();
        final var modelAndView = controller.resetPasswordConfirm("sometoken");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "sometoken"), entry("isAdmin", Boolean.TRUE));
    }

    @Test
    public void resetPasswordConfirm_FailureCheckView() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"));
        final var modelAndView = controller.resetPasswordConfirm("token");
        assertThat(modelAndView.getViewName()).isEqualTo("resetPassword");
    }

    @Test
    public void resetPasswordConfirm_FailureCheckModel() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"));
        final var modelAndView = controller.resetPasswordConfirm("sometoken");
        assertThat(modelAndView.getModel()).containsOnly(entry("error", "expired"));
    }

    @Test
    public void setPassword_Success() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile();
        final var modelAndView = controller.setPassword("d", "password123456", "password123456", null);
        assertThat(modelAndView.getViewName()).isEqualTo("redirect:/reset-password-success");
    }

    @Test
    public void setPassword_Failure() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"));
        final var modelAndView = controller.setPassword("sometoken", "new", "confirm", null);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("error", "expired"));
    }

    @Test
    public void setPassword_SuccessWithContext() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile();
        final var modelAndView = controller.setPassword("d", "password123456", "password123456", "licences");
        assertThat(modelAndView.getViewName()).isEqualTo("redirect:/initial-password-success");
    }

    @Test
    public void setPassword_FailureWithContext() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"));
        final var modelAndView = controller.setPassword("sometoken", "new", "confirm", "licences");
        assertThat(modelAndView.getViewName()).isEqualTo("resetPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("error", "expired"), entry("context", "licences"));
    }

    @Test
    public void setPasswordSelect_checkView() {
        setupCheckTokenValid();
        final var modelAndView = controller.resetPasswordSelect("token");
        assertThat(modelAndView.getViewName()).isEqualTo("setPasswordSelect");
    }

    @Test
    public void setPasswordSelect_checkModel() {
        setupCheckTokenValid();
        final var modelAndView = controller.resetPasswordSelect("sometoken");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "sometoken"));
    }

    @Test
    public void setPasswordSelect_tokenInvalid_checkView() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"));
        final var modelAndView = controller.resetPasswordSelect("sometoken");
        assertThat(modelAndView.getViewName()).isEqualTo("resetPassword");
    }

    @Test
    public void setPasswordSelect_tokenInvalid_checkModel() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"));
        final var modelAndView = controller.resetPasswordSelect("sometoken");
        assertThat(modelAndView.getModel()).containsOnly(entry("error", "expired"));
    }

    @Test
    public void setPasswordChosen_tokenInvalid_checkView() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"));
        final var modelAndView = controller.resetPasswordChosen("sometoken", "user");
        assertThat(modelAndView.getViewName()).isEqualTo("resetPassword");
    }

    @Test
    public void setPasswordChosen_tokenInvalid_checkModel() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"));
        final var modelAndView = controller.resetPasswordChosen("sometoken", "user");
        assertThat(modelAndView.getModel()).containsOnly(entry("error", "expired"));
    }

    @Test
    public void setPasswordChosen_checkView() {
        when(resetPasswordService.moveTokenToAccount(anyString(), anyString())).thenReturn("token");
        final var modelAndView = controller.resetPasswordChosen("sometoken", "user");
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
    }

    @Test
    public void setPasswordChosen_checkModel() {
        when(resetPasswordService.moveTokenToAccount(anyString(), anyString())).thenReturn("token");
        final var modelAndView = controller.resetPasswordChosen("sometoken", "user");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "token"), entry("isAdmin", Boolean.FALSE));
    }

    @Test
    public void setPasswordChosen_validationFailure_checkView() {
        setupCheckAndGetTokenValid();
        when(resetPasswordService.moveTokenToAccount(anyString(), anyString())).thenThrow(new ResetPasswordException("reason"));
        final var modelAndView = controller.resetPasswordChosen("sometoken", "user");
        assertThat(modelAndView.getViewName()).isEqualTo("setPasswordSelect");
    }

    @Test
    public void setPasswordChosen_validationFailure_checkModel() {
        setupCheckAndGetTokenValid();
        when(resetPasswordService.moveTokenToAccount(anyString(), anyString())).thenThrow(new ResetPasswordException("reason"));
        final var modelAndView = controller.resetPasswordChosen("sometoken", "user");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "sometoken"), entry("username", "user"), entry("email", "email@somewhere.com"), entry("error", "reason"));
    }

    private void setupCheckTokenValid() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty());
    }

    private void setupCheckAndGetTokenValid() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty());
        when(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(new UserToken(TokenType.RESET, new UserEmail("user", "email@somewhere.com", true, false))));
    }

    private StaffUserAccount setupGetUserCallForProfile() {
        final var user = new StaffUserAccount();
        user.setAccountDetail(new AccountDetail());
        when(userService.findUser(anyString())).thenReturn(Optional.of(user));
        return user;
    }
}
