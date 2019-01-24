package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
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
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService;
import uk.gov.service.notify.NotificationClientException;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResetPasswordControllerTest {
    @Mock
    private ResetPasswordService resetPasswordService;
    @Mock
    private TokenService tokenService;
    @Mock
    private UserService userService;
    @Mock
    private TelemetryClient telemetryClient;
    @Mock
    private HttpServletRequest request;
    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    private ResetPasswordController controller;

    @Before
    public void setUp() {
        controller = new ResetPasswordController(resetPasswordService, tokenService, userService, telemetryClient, true, Set.of("password1"));
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
    public void resetPasswordRequest_successNoLinkTelemetry() throws NotificationClientException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.empty());
        controller.resetPasswordRequest("user", request);
        verify(telemetryClient).trackEvent(eq("ResetPasswordRequestFailure"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).containsOnly(entry("username", "user"), entry("error", "nolink"));
    }

    @Test
    public void resetPasswordRequest_successLinkTelemetry() throws NotificationClientException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.of("somelink"));
        final var modelAndView = controller.resetPasswordRequest("user", request);
        verify(telemetryClient).trackEvent(eq("ResetPasswordRequestSuccess"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).containsOnly(entry("username", "user"));
    }

    @Test
    public void resetPasswordRequest_success() throws NotificationClientException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.empty());
        final var modelAndView = new ResetPasswordController(resetPasswordService, tokenService, userService, telemetryClient, false, null).resetPasswordRequest("user", request);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPasswordSent");
        assertThat(modelAndView.getModel()).isEmpty();
    }

    @Test
    public void resetPasswordRequest_failed() throws NotificationClientException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenThrow(new NotificationClientException("failure message"));
        final var modelAndView = new ResetPasswordController(resetPasswordService, tokenService, userService, telemetryClient, false, null).resetPasswordRequest("user", request);
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
        final var modelAndView = controller.setPassword("d", "password123456", "password123456");
        assertThat(modelAndView.getViewName()).isEqualTo("redirect:/reset-password-success");
    }

    @Test
    public void setPassword_Failure() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"));
        final var modelAndView = controller.setPassword("sometoken", "new", "confirm");
        assertThat(modelAndView.getViewName()).isEqualTo("resetPassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("error", "expired"));
    }

    private void setupCheckTokenValid() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty());
    }

    private void setupCheckAndGetTokenValid() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty());
        when(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(new UserToken(TokenType.RESET, new UserEmail("user"))));
    }

    private void setupGetUserCallForProfile() {
        final var user = Optional.of(new StaffUserAccount());
        user.get().setAccountDetail(new AccountDetail());
        when(userService.getUserByUsername(anyString())).thenReturn(user);
    }
}
