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
    public ModelAndView verifyEmailRequest(final Principal principal, final HttpServletRequest request, final HttpServletResponse response,
                                           @RequestParam(required = false) final String error)
            throws IOException, ServletException {
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

    @PostMapping("/verify-email")
    public ModelAndView verifyEmail(@RequestParam(required = false) final String candidate, @RequestParam final String email,
                                    final Principal principal, final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {
        final var username = principal.getName();

        if (StringUtils.isEmpty(candidate)) {
            return verifyEmailRequest(principal, request, response, "noselection");
        }

        final var chosenEmail = StringUtils.trim(StringUtils.isBlank(candidate) || "other".equals(candidate) ? email : candidate);

        if (StringUtils.isBlank(chosenEmail)) {
            return createVerifyEmailError(chosenEmail, "blank");
        }

        final var atIndex = StringUtils.indexOf(chosenEmail, '@');
        if (atIndex == -1 || !chosenEmail.matches(".*@.*\\..*")) {
            return createVerifyEmailError(chosenEmail, "format");
        }
        final var firstCharacter = chosenEmail.charAt(0);
        final var lastCharacter = chosenEmail.charAt(chosenEmail.length() - 1);
        if (firstCharacter == '.' || firstCharacter == '@' ||
                lastCharacter == '.' || lastCharacter == '@') {
            return createVerifyEmailError(chosenEmail, "firstlast");
        }
        if (chosenEmail.matches(".*\\.@.*") || chosenEmail.matches(".*@\\..*")) {
            return createVerifyEmailError(chosenEmail, "together");
        }
        if (StringUtils.countMatches(chosenEmail, '@') > 1) {
            return createVerifyEmailError(chosenEmail, "at");
        }
        if (StringUtils.containsWhitespace(chosenEmail)) {
            return createVerifyEmailError(chosenEmail, "white");
        }
        if (!chosenEmail.matches("[0-9A-Za-z@.'_\\-+]*")) {
            return createVerifyEmailError(chosenEmail, "characters");
        }
        if (!referenceCodesService.isValidEmailDomain(chosenEmail.substring(atIndex + 1))) {
            return createVerifyEmailError(chosenEmail, "domain");
        }

        try {
            final var verifyLink = verifyEmailService.requestVerification(username, chosenEmail, request.getRequestURL().append("-confirm?token=").toString());

            final var modelAndView = new ModelAndView("verifyEmailSent");
            if (smokeTestEnabled) {
                modelAndView.addObject("verifyLink", verifyLink);
            }
            modelAndView.addObject("email", chosenEmail);
            return modelAndView;
        } catch (final NotificationClientException e) {
            log.error("Failed to send email due to", e);
            return createVerifyEmailError(chosenEmail, "other");
        }
    }

    private ModelAndView createVerifyEmailError(final String chosenEmail, final String format) {
        final var modelAndView = new ModelAndView("verifyEmail", "email", chosenEmail);
        modelAndView.addObject("error", format);
        return modelAndView;
    }

    private void proceedToOriginalUrl(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        jwtAuthenticationSuccessHandler.proceed(request, response, SecurityContextHolder.getContext().getAuthentication());
    }

    @GetMapping("/verify-email-confirm/{token}")
    @Deprecated
    public ModelAndView verifyEmailConfirmInPath(@PathVariable final String token) {
        // can be removed after go live on the below method instead
        return verifyEmailConfirm(token);
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

}
