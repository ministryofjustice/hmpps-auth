package uk.gov.justice.digital.hmpps.oauth2server.resource;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler;
import uk.gov.justice.digital.hmpps.oauth2server.verify.ReferenceCodesService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService;
import uk.gov.service.notify.NotificationClientException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;

@Slf4j
@Controller
@Validated
public class VerifyEmailController {
    private final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler;
    private final VerifyEmailService verifyEmailService;
    private final ReferenceCodesService referenceCodesService;
    private final boolean smokeTestEnabled;

    public VerifyEmailController(final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler,
                                 final VerifyEmailService verifyEmailService,
                                 final ReferenceCodesService referenceCodesService,
                                 @Value("${application.smoketest.enabled}") final boolean smokeTestEnabled) {
        this.verifyEmailService = verifyEmailService;
        this.jwtAuthenticationSuccessHandler = jwtAuthenticationSuccessHandler;
        this.referenceCodesService = referenceCodesService;
        this.smokeTestEnabled = smokeTestEnabled;
    }

    @GetMapping("/verify-email")
    public ModelAndView verifyEmailRequest(final Principal principal, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
        final var modelAndView = new ModelAndView("verifyEmail");

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
            modelAndView.addObject("email", email.getEmail());
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

    @PostMapping("/verify-email")
    public ModelAndView verifyEmail(@RequestParam final String email, final Principal principal, final HttpServletRequest request) {
        final var username = principal.getName();

        final var atIndex = StringUtils.indexOf(email, '@');
        if (atIndex == -1 || !email.matches(".*@.*\\..*") || StringUtils.countMatches(email, '@') > 1) {
            final var modelAndView = new ModelAndView("verifyEmail", "email", email);
            modelAndView.addObject("error", "format");
            return modelAndView;
        }
        if (!email.matches("[0-9A-Za-z@.'_-]*")) {
            final var modelAndView = new ModelAndView("verifyEmail", "email", email);
            modelAndView.addObject("error", "characters");
            return modelAndView;
        }
        if (!referenceCodesService.isValidEmailDomain(email.substring(atIndex + 1))) {
            final var modelAndView = new ModelAndView("verifyEmail", "email", email);
            modelAndView.addObject("error", "domain");
            return modelAndView;
        }

        try {
            final var verifyLink = verifyEmailService.requestVerification(username, email, request.getRequestURL().append("-confirm").toString());

            final var modelAndView = new ModelAndView("verifyEmailSent");
            if (smokeTestEnabled) {
                modelAndView.addObject("verifyLink", verifyLink);
            }
            modelAndView.addObject("email", email);
            return modelAndView;
        } catch (final NotificationClientException e) {
            log.error("Failed to send email due to", e);
            final var modelAndView = new ModelAndView("verifyEmail", "email", email);
            modelAndView.addObject("error", "unknownerror");
            return modelAndView;
        }
    }

    private void proceedToOriginalUrl(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        jwtAuthenticationSuccessHandler.proceed(request, response, SecurityContextHolder.getContext().getAuthentication());
    }

    @GetMapping("/verify-email-confirm/{token}")
    public ModelAndView verifyEmailConfirm(@PathVariable final String token) {
        final var errorOptional = verifyEmailService.confirmEmail(token);
        if (errorOptional.isPresent()) {
            final var error = errorOptional.get();
            log.info("Failed to verify email due to: {}", error);
            return new ModelAndView("verifyEmailFailure", "error", error);
        }
        return new ModelAndView("verifyEmailSuccess");
    }

}
