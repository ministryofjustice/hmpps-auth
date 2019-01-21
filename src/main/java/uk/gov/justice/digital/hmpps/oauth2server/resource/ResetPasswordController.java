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
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountProfile;
import uk.gov.justice.digital.hmpps.oauth2server.security.PasswordValidationFailureException;
import uk.gov.justice.digital.hmpps.oauth2server.security.ReusedPasswordException;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordService;
import uk.gov.service.notify.NotificationClientException;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@Validated
public class ResetPasswordController {
    private final ResetPasswordService resetPasswordService;
    private final UserService userService;
    private final TelemetryClient telemetryClient;
    private final boolean smokeTestEnabled;

    public ResetPasswordController(final ResetPasswordService resetPasswordService,
                                   final UserService userService,
                                   final TelemetryClient telemetryClient, @Value("${application.smoketest.enabled}") final boolean smokeTestEnabled) {
        this.resetPasswordService = resetPasswordService;
        this.userService = userService;
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

    @GetMapping("/reset-password-confirm/{token}")
    public ModelAndView resetPasswordConfirm(@PathVariable final String token) {
        final var userTokenOptional = resetPasswordService.checkToken(token);
        return userTokenOptional.map(s -> new ModelAndView("resetPassword", "error", s)).
                orElseGet(() -> new ModelAndView("setPassword", "token", token));
    }

    @PostMapping("/set-password")
    public ModelAndView setPassword(@RequestParam final String token,
                                    @RequestParam final String newPassword, @RequestParam final String confirmPassword) {
        final var userTokenOptional = resetPasswordService.checkToken(token);
        if (userTokenOptional.isPresent()) {
            return new ModelAndView("resetPassword", "error", userTokenOptional.get());
        }
        // token checked already by service, so can just get it here
        //noinspection OptionalGetWithoutIsPresent
        final var userToken = resetPasswordService.getToken(token).get();
        final var username = userToken.getUserEmail().getUsername();

        final var modelAndView = new ModelAndView("setPassword", "token", token);

        final var validationResult = validate(username, newPassword, confirmPassword);
        if (!validationResult.isEmpty()) {
            telemetryClient.trackEvent("ResetPasswordFailure", Map.of("username", username, "reason", validationResult.toString()), null);
            modelAndView.addAllObjects(validationResult);
            modelAndView.addObject("error", Boolean.TRUE);
            return modelAndView;
        }

        try {
            resetPasswordService.resetPassword(token, newPassword);

        } catch (final Exception e) {
            if (e instanceof PasswordValidationFailureException) {
                return trackAndReturnToSetPassword(username, modelAndView, "validation");
            }
            if (e instanceof ReusedPasswordException) {
                return trackAndReturnToSetPassword(username, modelAndView, "reused");
            }
            // let any other exception bubble up
            throw e;
        }

        log.info("Successfully changed password for {}", username);
        return new ModelAndView("redirect:/reset-password-success");
    }

    private ModelAndView trackAndReturnToSetPassword(final String username, final ModelAndView modelAndView, final String reason) {
        telemetryClient.trackEvent("ResetPasswordFailure", Map.of("username", username, "reason", reason), null);
        modelAndView.addObject("errornew", reason);
        modelAndView.addObject("error", Boolean.TRUE);
        return modelAndView;
    }

    private Map<String, Object> validate(final String username, final String newPassword, final String confirmPassword) {
        final Map<String, Object> builder = new HashMap<>();
        if (StringUtils.isBlank(newPassword)) {
            builder.put("errornew", "newmissing");
        }
        if (StringUtils.isBlank(confirmPassword)) {
            builder.put("errorconfirm", "confirmmissing");
        }

        // Bomb out now as either new password or confirm new password is missing
        if (!builder.isEmpty()) {
            return builder;
        }

        // user must be present in order for authenticate to work above
        //noinspection OptionalGetWithoutIsPresent
        final var user = userService.getUserByUsername(username).get();

        // Ensuring alphanumeric will ensure that we can't get SQL Injection attacks - since for oracle the password
        // cannot be used in a prepared statement
        if (!StringUtils.isAlphanumeric(newPassword)) {
            builder.put("errornew", "alphanumeric");
        }
        final var digits = StringUtils.getDigits(newPassword);
        if (digits.length() == 0) {
            builder.put("errornew", "nodigits");
        }
        if (digits.length() == newPassword.length()) {
            builder.put("errornew", "alldigits");
        }
        if (StringUtils.containsIgnoreCase(newPassword, username)) {
            builder.put("errornew", "username");
        }
        if (newPassword.chars().distinct().count() < 4) {
            builder.put("errornew", "four");
        }

        if (!StringUtils.equals(newPassword, confirmPassword)) {
            builder.put("errorconfirm", "mismatch");
        }
        if (user.getAccountDetail().getAccountProfile() == AccountProfile.TAG_ADMIN) {
            if (newPassword.length() < 14) {
                builder.put("errornew", "length14");
            }
        } else if (newPassword.length() < 9) {
            builder.put("errornew", "length9");
        }

        return builder;
    }
}
