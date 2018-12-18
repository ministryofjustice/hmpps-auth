package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountProfile;
import uk.gov.justice.digital.hmpps.oauth2server.security.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Controller
@Validated
public class ChangePasswordController {
    private final UserStateAuthenticationFailureHandler userStateAuthenticationFailureHandler;
    private final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler;
    private final DaoAuthenticationProvider daoAuthenticationProvider;
    private final ChangePasswordService changePasswordService;
    private final UserService userService;
    private final TelemetryClient telemetryClient;

    public ChangePasswordController(final UserStateAuthenticationFailureHandler userStateAuthenticationFailureHandler,
                                    final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler,
                                    final DaoAuthenticationProvider daoAuthenticationProvider,
                                    final ChangePasswordService changePasswordService,
                                    final UserService userService,
                                    final TelemetryClient telemetryClient) {
        this.userStateAuthenticationFailureHandler = userStateAuthenticationFailureHandler;
        this.jwtAuthenticationSuccessHandler = jwtAuthenticationSuccessHandler;
        this.daoAuthenticationProvider = daoAuthenticationProvider;
        this.changePasswordService = changePasswordService;
        this.userService = userService;
        this.telemetryClient = telemetryClient;
    }

    @GetMapping("/change-password")
    public String changePasswordRequest() {
        return "changePassword";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam final String username, @RequestParam final String password,
                                 @RequestParam final String newPassword, @RequestParam final String confirmPassword,
                                 final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
        try {
            final var validationResult = validate(username, password, newPassword, confirmPassword);
            if (validationResult != null) {
                return validationResult;
            }
        } catch (final AuthenticationException e) {
            // unhandled authentication exception, so redirect and display on login page
            userStateAuthenticationFailureHandler.onAuthenticationFailure(request, response, e);
            return null;
        }

        try {
            changePasswordService.changePassword(username, newPassword);
        } catch (final Exception e) {
            if (e instanceof PasswordValidationFailureException) {
                return getChangePasswordRedirect(username, "new", "validation");
            }
            if (e instanceof ReusedPasswordException) {
                return getChangePasswordRedirect(username, "new", "reused");
            }
            // let any other exception bubble up
            throw e;
        }

        log.info("Successfully changed password for {}", username);

        // now try again authentication with new password
        try {
            final var successToken = authenticate(username, newPassword);
            // success, so forward on
            telemetryClient.trackEvent("ChangePasswordSuccess", Map.of("username", username), null);
            jwtAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, successToken);
            // return here is not required, since the success handler will have redirected
            return null;
        } catch (final AuthenticationException e) {
            final String reason = e.getClass().getSimpleName();
            log.info("Caught unexpected {} after change password", reason, e);
            telemetryClient.trackEvent("ChangePasswordFailure", Map.of("username", username, "reason", reason), null);
            // this should have succeeded, but unable to login
            // need to tell user that the change password request has been successful though
            //noinspection SpellCheckingInspection
            return getLoginRedirect("changepassword");
        }
    }

    private String validate(final String username, final String password, final String newPassword, final String confirmPassword) {
        if (StringUtils.isBlank(username)) {
            // something went wrong with login - get them to start again
            log.info("Missing required username for change password");
            return getLoginRedirect("missing");
        }

        if (StringUtils.isBlank(password)) {
            return getChangePasswordRedirect(username, "current", "missing");
        }

        // only way to check existing password is to try to authenticate the user
        try {
            authenticate(username, password);

            // also allow successful authenticate to change password
        } catch (final AuthenticationException e) {
            if (e instanceof BadCredentialsException) {
                return getChangePasswordRedirect(username, "current", "invalid");
            }
            // anything else apart from an account expired exception is unexpected
            if (!(e instanceof AccountExpiredException)) {
                throw e;
            }
        }

        // Ensuring alphanumeric will ensure that we can't get SQL Injection attacks - since for oracle the password
        // cannot be used in a prepared statement
        if (!StringUtils.isAlphanumeric(newPassword)) {
            return getChangePasswordRedirect(username, "new", "alphanumeric");
        }
        final var digits = StringUtils.getDigits(newPassword);
        if (digits.length() == 0) {
            return getChangePasswordRedirect(username, "new", "nodigits");
        }
        if (digits.length() == newPassword.length()) {
            return getChangePasswordRedirect(username, "new", "alldigits");
        }
        if (StringUtils.contains(newPassword, username)) {
            return getChangePasswordRedirect(username, "new", "username");
        }
        if (newPassword.chars().distinct().count() < 4) {
            return getChangePasswordRedirect(username, "new", "four");
        }

        if (!StringUtils.equals(newPassword, confirmPassword)) {
            return getChangePasswordRedirect(username, "new", "mismatch");
        }

        final var user = userService.getUserByUsername(username);
        //noinspection OptionalGetWithoutIsPresent
        if (user.get().getAccountDetail().getAccountProfile() == AccountProfile.TAG_ADMIN && newPassword.length() < 14) {
            return getChangePasswordRedirect(username, "new", "length14");
        }

        if (newPassword.length() < 9) {
            return getChangePasswordRedirect(username, "new", "length9");
        }

        return null;
    }

    private String getChangePasswordRedirect(final String username, final String field, final String reason) {
        telemetryClient.trackEvent("ChangePasswordFailure", Map.of("username", username, "reason", reason), null);
        return String.format("redirect:/change-password?error%s=%s&username=%s", field, reason, username);
    }

    private String getLoginRedirect(final String reason) {
        return String.format("redirect:/login?error&reason=%s", reason);
    }

    private Authentication authenticate(final String username, final String password) {
        final var token = new UsernamePasswordAuthenticationToken(username.toUpperCase(), password);
        return daoAuthenticationProvider.authenticate(token);
    }
}
