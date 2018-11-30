package uk.gov.justice.digital.hmpps.oauth2server.resource;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.justice.digital.hmpps.oauth2server.security.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Controller
@Validated
public class ChangePasswordController {
    private final UserStateAuthenticationFailureHandler userStateAuthenticationFailureHandler;
    private final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler;
    private final DaoAuthenticationProvider daoAuthenticationProvider;
    private final ChangePasswordService changePasswordService;

    public ChangePasswordController(final UserStateAuthenticationFailureHandler userStateAuthenticationFailureHandler, final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler, final DaoAuthenticationProvider daoAuthenticationProvider,
                                    final ChangePasswordService changePasswordService) {
        this.userStateAuthenticationFailureHandler = userStateAuthenticationFailureHandler;
        this.jwtAuthenticationSuccessHandler = jwtAuthenticationSuccessHandler;
        this.daoAuthenticationProvider = daoAuthenticationProvider;
        this.changePasswordService = changePasswordService;
    }

    @GetMapping("/changePassword")
    public String changePasswordRequest() {
        return "changePassword";
    }

    @PostMapping("/changePassword")
    public String changePassword(@RequestParam final String username, @RequestParam final String password,
                                 @RequestParam final String newPassword, @RequestParam final String confirmPassword,
                                 final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {

        final String validationResult = validate(username, password, newPassword, confirmPassword);
        if (validationResult != null) {
            return validationResult;
        }

        try {
            authenticate(username, password);
            // also allow successful authenticate to change password
        } catch (final AuthenticationException e) {
            // anything else apart from an account expired exception is unexpected
            if (!(e instanceof AccountExpiredException)) {
                // some other authentication reason, so redirect and display on login page
                userStateAuthenticationFailureHandler.onAuthenticationFailure(request, response, e);
                return null;
            }
        }

        try {
            changePasswordService.changePassword(username, newPassword);
        } catch (final Exception e) {
            if (e instanceof PasswordValidationFailureException) {
                return getChangePasswordRedirect(username, "validation");
            }
            if (e instanceof ReusedPasswordException) {
                return getChangePasswordRedirect(username, "reused");
            }
            // let any other exception bubble up
            throw e;
        }

        log.info("Successfully changed password for {}", username);

        // now try again authentication with new password
        try {
            final var successToken = authenticate(username, newPassword);
            // success, so forward on
            jwtAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, successToken);
            // return here is not required, since the success handler will have redirected
            return null;
        } catch (final AuthenticationException e) {
            log.info("Caught unexpected {} after change password", e.getClass().getName(), e);
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
            return getChangePasswordRedirect(username, "missing");
        }

        // TODO: need to calculate what type of user is changing password as admin has 14 characters instead
        if (newPassword.length() < 9) {
            return getChangePasswordRedirect(username, "length");
        }
        // Ensuring alphanumeric will ensure that we can't get SQL Injection attacks - since for oracle the password
        // cannot be used in a prepared statement
        if (!StringUtils.isAlphanumeric(newPassword)) {
            return getChangePasswordRedirect(username, "alphanumeric");
        }
        final String digits = StringUtils.getDigits(newPassword);
        if (digits.length() == 0) {
            return getChangePasswordRedirect(username, "digits");
        }
        if (digits.length() == newPassword.length()) {
            return getChangePasswordRedirect(username, "digits");
        }
        if (StringUtils.contains(newPassword, username)) {
            return getChangePasswordRedirect(username, "username");
        }
        if (newPassword.chars().distinct().count() < 4) {
            return getChangePasswordRedirect(username, "four");
        }

        if (!StringUtils.equals(newPassword, confirmPassword)) {
            return getChangePasswordRedirect(username, "mismatch");
        }
        return null;
    }

    private String getChangePasswordRedirect(final String username, final String reason) {
        return String.format("redirect:/changePassword?error&username=%s&reason=%s", username, reason);
    }

    private String getLoginRedirect(final String reason) {
        return String.format("redirect:/login?error&reason=%s", reason);
    }

    private Authentication authenticate(final String username, final String password) {
        final var token = new UsernamePasswordAuthenticationToken(username.toUpperCase(), password);
        return daoAuthenticationProvider.authenticate(token);
    }
}
