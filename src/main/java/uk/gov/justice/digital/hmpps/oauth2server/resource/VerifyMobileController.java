package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyMobileService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyMobileService.VerifyMobileException;
import uk.gov.service.notify.NotificationClientException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@Validated
public class VerifyMobileController {
    private final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler;
    private final VerifyMobileService verifyMobileService;
    private final TelemetryClient telemetryClient;
    private final UserService userService;
    private final boolean smokeTestEnabled;

    public VerifyMobileController(final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler,
                                  final VerifyMobileService verifyMobileService,
                                  final TelemetryClient telemetryClient,
                                  final UserService userService, @Value("${application.smoketest.enabled}") final boolean smokeTestEnabled) {
        this.verifyMobileService = verifyMobileService;
        this.jwtAuthenticationSuccessHandler = jwtAuthenticationSuccessHandler;
        this.telemetryClient = telemetryClient;
        this.userService = userService;
        this.smokeTestEnabled = smokeTestEnabled;
    }

    @GetMapping("/verify-mobile-continue")
    public void verifyMobileContinue(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        proceedToOriginalUrl(request, response);
    }

    @PostMapping("/verify-mobile")
    public ModelAndView verifyMobile(@RequestParam final String mobile,
                                     final Principal principal) {
        final var username = principal.getName();
        final var currentMobile = userService.findUser(username).get().getMobile();
        try {
            final var verifyCode = verifyMobileService.requestVerification(username, mobile);

            final var modelAndView = new ModelAndView("verifyMobileSent");
            if (smokeTestEnabled) {
                modelAndView.addObject("verifyCode", verifyCode);
            }
            return modelAndView;
        } catch (final VerifyMobileException e) {
            log.info("Validation failed for mobile phone number due to {}", e.getReason());
            telemetryClient.trackEvent("VerifyMobileRequestFailure", Map.of("username", username, "reason", e.getReason()), null);
            return createChangeOrVerifyMobileError(e.getReason(), currentMobile);
        } catch (final NotificationClientException e) {
            log.error("Failed to send sms due to", e);
            return createChangeOrVerifyMobileError("other", currentMobile);
        }
    }

    private ModelAndView createChangeOrVerifyMobileError(final String reason, final String currentMobile) {
        final var modelAndView = new ModelAndView("changeMobile");
        modelAndView.addObject("error", reason);
        modelAndView.addObject("mobile", currentMobile);
        return modelAndView;
    }

    private void proceedToOriginalUrl(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        jwtAuthenticationSuccessHandler.proceed(request, response, SecurityContextHolder.getContext().getAuthentication());
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
        try {
            final var verifyCode = verifyMobileService.resendVerificationCode(username);

            final var modelAndView = new ModelAndView("verifyMobileSent");
            if (smokeTestEnabled) {
                modelAndView.addObject("verifyCode", verifyCode.get());
            }
            return modelAndView;

        } catch (final VerifyMobileException e) {
            log.info("Validation failed for mobile phone number due to {}", e.getReason());
            telemetryClient.trackEvent("VerifyMobileRequestFailure", Map.of("username", username, "reason", e.getReason()), null);
            return createChangeOrVerifyMobileError(e.getReason(), null);
        } catch (final NotificationClientException e) {
            log.error("Failed to send sms due to", e);
            return createChangeOrVerifyMobileError("other", null);
        }
    }
}
