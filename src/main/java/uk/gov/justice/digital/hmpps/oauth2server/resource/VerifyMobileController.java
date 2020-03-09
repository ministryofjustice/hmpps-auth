package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyMobileService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyMobileService.VerifyMobileException;
import uk.gov.service.notify.NotificationClientException;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@Validated
public class VerifyMobileController {
    private final VerifyMobileService verifyMobileService;
    private final TelemetryClient telemetryClient;
    private final UserService userService;
    private final boolean smokeTestEnabled;

    public VerifyMobileController(final VerifyMobileService verifyMobileService,
                                  final TelemetryClient telemetryClient,
                                  final UserService userService, @Value("${application.smoketest.enabled}") final boolean smokeTestEnabled) {
        this.verifyMobileService = verifyMobileService;
        this.telemetryClient = telemetryClient;
        this.userService = userService;
        this.smokeTestEnabled = smokeTestEnabled;
    }

    @PostMapping("/change-mobile")
    public ModelAndView changeMobile(@RequestParam final String mobile, final Principal principal) {
        final var username = principal.getName();

        if (userService.isSameAsCurrentVerifiedMobile(username, mobile)) {
            return new ModelAndView("verifyMobileAlready");
        }
        try {
            verifyMobileService.requestVerification(username, mobile);
            return new ModelAndView("redirect:/verify-mobile");
        } catch (final VerifyMobileException e) {
            log.info("Validation failed for mobile phone number due to {}", e.getReason());
            telemetryClient.trackEvent("VerifyMobileRequestFailure", Map.of("username", username, "reason", e.getReason()), null);
            return createChangeOrVerifyMobileError(e.getReason(), mobile);
        } catch (final NotificationClientException e) {
            log.error("Failed to send sms due to", e);
            telemetryClient.trackEvent("VerifyMobileRequestFailure", Map.of("username", username, "reason", "notify"), null);
            return createChangeOrVerifyMobileError("other", mobile);
        }
    }

    @GetMapping("/verify-mobile")
    public ModelAndView verifyMobile(final Principal principal) {
        final var modelAndView = new ModelAndView("verifyMobileSent");
        if (smokeTestEnabled) {
            verifyMobileService.findMobileVerificationCode(principal.getName())
                    .ifPresent(c -> modelAndView.addObject("verifyCode", c));
        }
        return modelAndView;
    }

    private ModelAndView createChangeOrVerifyMobileError(final String reason, final String currentMobile) {
        return new ModelAndView("account/changeMobile")
                .addObject("error", reason)
                .addObject("mobile", currentMobile);
    }

    @PostMapping("/verify-mobile-confirm")
    public ModelAndView verifyMobileConfirm(@RequestParam final String code) throws NotificationClientException {
        final var errorOptional = verifyMobileService.confirmMobile(code);
        return errorOptional.map(error -> {
            log.info("Failed to verify mobile phone number due to: {}", error);
            final var modelAndView = new ModelAndView("verifyMobileSent", "error", error.get("error"));
            if (smokeTestEnabled) {
                modelAndView.addObject("verifyCode", error.get("verifyCode"));
            }
            return modelAndView;
        }).orElse(new ModelAndView("verifyMobileSuccess"));
    }

    @GetMapping("/mobile-resend")
    public ModelAndView mobileResendRequest(final Principal principal) {
        final var mobileVerified = verifyMobileService.mobileVerified(principal.getName());
        if (mobileVerified) {
            return new ModelAndView("verifyMobileAlready");
        }
        return new ModelAndView("verifyMobileResend");
    }

    @PostMapping("/verify-mobile-resend")
    public ModelAndView mobileResend(final Principal principal) {
        final var username = principal.getName();
        final var currentMobile = userService.getUser(username).getMobile();
        try {
            verifyMobileService.resendVerificationCode(username);
            return new ModelAndView("redirect:/verify-mobile");

        } catch (final VerifyMobileException e) {
            log.info("Validation failed for mobile phone number due to {}", e.getReason());
            telemetryClient.trackEvent("VerifyMobileRequestFailure", Map.of("username", username, "reason", e.getReason()), null);
            return createChangeOrVerifyMobileError(e.getReason(), currentMobile);
        } catch (final NotificationClientException e) {
            log.error("Failed to send sms due to", e);
            return createChangeOrVerifyMobileError("other", currentMobile);
        }
    }
}
