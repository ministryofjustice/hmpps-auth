package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.EmailType;
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException;
import uk.gov.service.notify.NotificationClientException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Controller
@Validated
public class VerifyEmailController {
    private final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler;
    private final VerifyEmailService verifyEmailService;
    private final UserService userService;

    private final TelemetryClient telemetryClient;
    private final boolean smokeTestEnabled;

    public VerifyEmailController(final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler,
                                 final VerifyEmailService verifyEmailService,
                                 final UserService userService, final TelemetryClient telemetryClient, @Value("${application.smoketest.enabled}") final boolean smokeTestEnabled) {
        this.verifyEmailService = verifyEmailService;
        this.jwtAuthenticationSuccessHandler = jwtAuthenticationSuccessHandler;
        this.userService = userService;
        this.telemetryClient = telemetryClient;
        this.smokeTestEnabled = smokeTestEnabled;
    }

    @GetMapping("/verify-email")
    public ModelAndView verifyEmailRequest(final Principal principal, final HttpServletRequest request, final HttpServletResponse response,
                                           @RequestParam(required = false) final String error) throws IOException, ServletException {

        final var modelAndView = new ModelAndView("verifyEmail");
        if (StringUtils.isNotBlank(error)) {
            modelAndView.addObject("error", error);
        }

        // Firstly check to see if they have an email address
        final var username = principal.getName();
        final var optionalEmail = verifyEmailService.getEmail(username);
        if (optionalEmail.isPresent()) {
            final var email = optionalEmail.get();
            if (email.isVerified()) {
                // no work to do here, so forward on
                proceedToOriginalUrl(request, response);
                return null;
            }
            // need to re-verify the email address
            modelAndView.addObject("suggestion", email.getEmail());
            return modelAndView;
        }

        // retrieve email addresses that are currently in use
        final var existingEmailAddresses = verifyEmailService.getExistingEmailAddresses(username);

        modelAndView.addObject("candidates", existingEmailAddresses);

        return modelAndView;
    }

    @GetMapping("/verify-email-continue")
    public void verifyEmailContinue(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        proceedToOriginalUrl(request, response);
    }

    @GetMapping("/verify-email-skip")
    public void verifyEmailSkip(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        telemetryClient.trackEvent("VerifyEmailRequestSkip", Collections.emptyMap(), null);
        proceedToOriginalUrl(request, response);
    }

    @PostMapping("/verify-email")
    public ModelAndView verifyEmail(@RequestParam(required = false) final String candidate, @RequestParam final String email, @RequestParam final EmailType emailType,
                                    final Principal principal, final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {
        final var username = principal.getName();

        // candidate will either be an email address from the selection or 'other' meaning free text
        if (StringUtils.isEmpty(candidate)) {
            return verifyEmailRequest(principal, request, response, "noselection");
        }

        final var chosenEmail = StringUtils.trim(StringUtils.isBlank(candidate) || "other".equals(candidate) || "change".equals(candidate) ? email : candidate);

        if (userService.isSameAsCurrentVerifiedEmail(username, chosenEmail, emailType)) {
            return new ModelAndView("verifyEmailAlready", "emailType", emailType);
        }

        try {
            final var confirmUrl = emailType == EmailType.PRIMARY ? "-confirm?token=" : "-secondary-confirm?token=";
            final var verifyLink = requestVerificationForUser(username, chosenEmail, request.getRequestURL().append(confirmUrl).toString(), emailType);

            final var modelAndView = new ModelAndView("verifyEmailSent");
            if (smokeTestEnabled) {
                modelAndView.addObject("verifyLink", verifyLink);
            }
            modelAndView.addObject("email", chosenEmail);
            return modelAndView;
        } catch (final VerifyEmailException e) {
            log.info("Validation failed for email address due to {}", e.getReason());
            telemetryClient.trackEvent("VerifyEmailRequestFailure", Map.of("username", username, "reason", e.getReason()), null);
            return createChangeOrVerifyEmailError(chosenEmail, e.getReason(), candidate);
        } catch (final NotificationClientException e) {
            log.error("Failed to send email due to", e);
            return createChangeOrVerifyEmailError(chosenEmail, "other", candidate);
        }
    }

    private String requestVerificationForUser(final String username, final String emailInput, final String url, final EmailType emailType) throws NotificationClientException, VerifyEmailException {

        final var userPersonDetails = userService.findMasterUserPersonDetails(username).orElseThrow();
        final var firstName = userPersonDetails.getFirstName();

        return verifyEmailService.requestVerification(username, emailInput, firstName, url, emailType);
    }

    private ModelAndView createChangeOrVerifyEmailError(final String chosenEmail, final String reason, final String type) {
        final var view = StringUtils.equals(type, "change") ? "changeEmail" : "verifyEmail";
        return new ModelAndView(view)
                .addObject("email", chosenEmail)
                .addObject("error", reason);
    }

    private void proceedToOriginalUrl(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        jwtAuthenticationSuccessHandler.proceed(request, response, SecurityContextHolder.getContext().getAuthentication());
    }

    @GetMapping("/verify-email-confirm")
    public ModelAndView verifyEmailConfirm(@RequestParam final String token) {
        final var errorOptional = verifyEmailService.confirmEmail(token);
        if (errorOptional.isPresent()) {
            final var error = errorOptional.get();
            log.info("Failed to verify email due to: {}", error);
            return new ModelAndView("verifyEmailFailure", "error", error);
        }
        return new ModelAndView("verifyEmailSuccess");
    }

    @GetMapping("/verify-email-secondary-confirm")
    public ModelAndView verifySecondaryEmailConfirm(@RequestParam final String token) {
        final var errorOptional = verifyEmailService.confirmSecondaryEmail(token);
        if (errorOptional.isPresent()) {
            final var error = errorOptional.get();
            log.info("Failed to verify secondary email due to: {}", error);
            return new ModelAndView("verifyEmailFailure", "error", error);
        }
        return new ModelAndView("verifyEmailSuccess");
    }

}
