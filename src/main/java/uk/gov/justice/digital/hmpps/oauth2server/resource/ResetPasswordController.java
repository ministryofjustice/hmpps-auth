package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService;
import uk.gov.service.notify.NotificationClientException;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

import static uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.RESET;

@Slf4j
@Controller
@Validated
public class ResetPasswordController extends AbstractPasswordController {
    private final ResetPasswordService resetPasswordService;
    private final TokenService tokenService;
    private final TelemetryClient telemetryClient;
    private final boolean smokeTestEnabled;

    public ResetPasswordController(final ResetPasswordService resetPasswordService,
                                   final TokenService tokenService, final UserService userService,
                                   final TelemetryClient telemetryClient, @Value("${application.smoketest.enabled}") final boolean smokeTestEnabled,
                                   final @Value("${application.authentication.blacklist}") Set<String> passwordBlacklist) {
        super(resetPasswordService, tokenService, userService, telemetryClient, "resetPassword", "setPassword", passwordBlacklist);
        this.resetPasswordService = resetPasswordService;
        this.tokenService = tokenService;
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
            final var resetLink = resetPasswordService.requestResetPassword(username, request.getRequestURL().append("-confirm?token=").toString());
            final var modelAndView = new ModelAndView("resetPasswordSent");
            if (resetLink.isPresent()) {
                log.info("Reset password success for {}", username);
                telemetryClient.trackEvent("ResetPasswordRequestSuccess", Map.of("username", username), null);
                if (smokeTestEnabled) {
                    modelAndView.addObject("resetLink", resetLink.get());
                }
            } else {
                log.info("Reset password failed, no link provided for {}", username);
                telemetryClient.trackEvent("ResetPasswordRequestFailure", Map.of("username", username, "error", "nolink"), null);
                if (smokeTestEnabled) {
                    modelAndView.addObject("resetLinkMissing", true);
                }
            }
            return modelAndView;

        } catch (final NotificationClientException e) {
            log.error("Failed to send reset password due to", e);
            telemetryClient.trackEvent("ResetPasswordRequestFailure",
                    Map.of("username", username, "error", e.getClass().getSimpleName()), null);
            return new ModelAndView("resetPassword", "error", "other");
        }
    }

    @GetMapping("/reset-password-confirm/{token}")
    @Deprecated
    public ModelAndView resetPasswordConfirmInPath(@PathVariable final String token) {
        // can be removed after go live on the below method instead
        return resetPasswordConfirm(token);
    }

    @GetMapping("/reset-password-confirm")
    public ModelAndView resetPasswordConfirm(@RequestParam final String token) {
        final var userTokenOptional = tokenService.checkToken(RESET, token);
        return userTokenOptional.map(s -> new ModelAndView("resetPassword", "error", s)).
                orElseGet(() -> new ModelAndView("setPassword", "token", token));
    }

    @PostMapping("/set-password")
    public ModelAndView setPassword(@RequestParam final String token,
                                    @RequestParam final String newPassword, @RequestParam final String confirmPassword) {
        final var modelAndView = processSetPassword(RESET, token, newPassword, confirmPassword);

        return modelAndView.orElse(new ModelAndView("redirect:/reset-password-success"));
    }
}
