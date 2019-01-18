package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordService;
import uk.gov.service.notify.NotificationClientException;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResetPasswordControllerTest {
    @Mock
    private ResetPasswordService resetPasswordService;
    @Mock
    private TelemetryClient telemetryClient;
    @Mock
    private HttpServletRequest request;

    private ResetPasswordController controller;

    @Before
    public void setUp() {
        controller = new ResetPasswordController(resetPasswordService, telemetryClient, true);
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
        assertThat(modelAndView.getModel()).containsExactly(MapEntry.entry("error", "missing"));
    }

    @Test
    public void resetPasswordRequest_successSmokeWithLink() throws NotificationClientException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.of("url"));
        final var modelAndView = controller.resetPasswordRequest("user", request);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPasswordSent");
        assertThat(modelAndView.getModel()).containsExactly(MapEntry.entry("resetLink", "url"));
    }

    @Test
    public void resetPasswordRequest_successSmokeNoLink() throws NotificationClientException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.empty());
        final var modelAndView = controller.resetPasswordRequest("user", request);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPasswordSent");
        assertThat(modelAndView.getModel()).containsExactly(MapEntry.entry("resetLinkMissing", Boolean.TRUE));
    }

    @Test
    public void resetPasswordRequest_success() throws NotificationClientException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenReturn(Optional.empty());
        final var modelAndView = new ResetPasswordController(resetPasswordService, telemetryClient, false).resetPasswordRequest("user", request);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPasswordSent");
        assertThat(modelAndView.getModel()).isEmpty();
    }

    @Test
    public void resetPasswordRequest_failed() throws NotificationClientException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("someurl"));
        when(resetPasswordService.requestResetPassword(anyString(), anyString())).thenThrow(new NotificationClientException("failure message"));
        final var modelAndView = new ResetPasswordController(resetPasswordService, telemetryClient, false).resetPasswordRequest("user", request);
        assertThat(modelAndView.getViewName()).isEqualTo("resetPassword");
        assertThat(modelAndView.getModel()).containsExactly(MapEntry.entry("error", "other"));
    }
}
