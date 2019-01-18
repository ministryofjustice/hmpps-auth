package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordService;
import uk.gov.service.notify.NotificationClientException;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@Controller
@Validated
public class ResetPasswordController {
    private final ResetPasswordService resetPasswordService;
    private final TelemetryClient telemetryClient;
    private final boolean smokeTestEnabled;

    public ResetPasswordController(final ResetPasswordService resetPasswordService,
                                   final TelemetryClient telemetryClient, @Value("${application.smoketest.enabled}") final boolean smokeTestEnabled) {
        this.resetPasswordService = resetPasswordService;
        this.telemetryClient = telemetryClient;
        this.smokeTestEnabled = smokeTestEnabled;
    }

    @GetMapping("/reset-password")
    public String resetPasswordRequest() {
        return "resetPassword";
    }

    @GetMapping("/reset-password-success")
    public String resetPasswordSuccess() {
        return "resetPasswordSuccess";
    }

    @PostMapping("/reset-password")
    public ModelAndView resetPasswordRequest(@RequestParam(required = false) final String username,
                                             final HttpServletRequest request) {
        if (StringUtils.isBlank(username)) {
            telemetryClient.trackEvent("ResetPasswordRequestFailure", Map.of("error", "missing"), null);
            return new ModelAndView("resetPassword", "error", "missing");
        }

        try {
            final var resetLink = resetPasswordService.requestResetPassword(username, request.getRequestURL().append("-confirm").toString());
            final var modelAndView = new ModelAndView("resetPasswordSent");
            if (smokeTestEnabled) {
                if (resetLink.isPresent()) {
                    modelAndView.addObject("resetLink", resetLink.get());
                } else {
                    modelAndView.addObject("resetLinkMissing", true);
                }
            }
            telemetryClient.trackEvent("ResetPasswordRequestSuccess", Map.of("username", username), null);
            return modelAndView;

        } catch (final NotificationClientException e) {
            log.error("Failed to send reset password due to", e);
            telemetryClient.trackEvent("ResetPasswordRequestFailure",
                    Map.of("username", username, "error", e.getClass().getSimpleName()), null);
            return new ModelAndView("resetPassword", "error", "other");
        }
    }
}
