package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.LockedException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountProfile;
import uk.gov.justice.digital.hmpps.oauth2server.security.PasswordValidationFailureException;
import uk.gov.justice.digital.hmpps.oauth2server.security.ReusedPasswordException;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.PasswordService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class AbstractPasswordController {
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final UserService userService;
    private final TelemetryClient telemetryClient;

    private final String startAgainViewName;
    private final String failureViewName;

    public AbstractPasswordController(final PasswordService passwordService,
                                      final TokenService tokenService, final UserService userService,
                                      final TelemetryClient telemetryClient,
                                      final String startAgainViewName, final String failureViewName) {
        this.passwordService = passwordService;
        this.tokenService = tokenService;
        this.userService = userService;
        this.telemetryClient = telemetryClient;
        this.startAgainViewName = startAgainViewName;
        this.failureViewName = failureViewName;
    }

    protected Optional<ModelAndView> processSetPassword(final TokenType tokenType, final String token,
                                                        final String newPassword, final String confirmPassword) {
        final var userTokenOptional = tokenService.checkToken(tokenType, token);
        if (userTokenOptional.isPresent()) {
            return Optional.of(new ModelAndView(startAgainViewName, "error", userTokenOptional.get()));
        }
        // token checked already by service, so can just get it here
        //noinspection OptionalGetWithoutIsPresent
        final var userToken = tokenService.getToken(tokenType, token).get();
        final var username = userToken.getUserEmail().getUsername();

        final var validationResult = validate(username, newPassword, confirmPassword);
        if (!validationResult.isEmpty()) {
            final var modelAndView = new ModelAndView(failureViewName, "token", token);
            return trackAndReturn(tokenType, username, modelAndView, validationResult);
        }

        try {
            passwordService.setPassword(token, newPassword);

        } catch (final Exception e) {
            final var modelAndView = new ModelAndView(failureViewName, "token", token);

            if (e instanceof PasswordValidationFailureException) {
                return trackAndReturn(tokenType, username, modelAndView, "validation");
            }
            if (e instanceof ReusedPasswordException) {
                return trackAndReturn(tokenType, username, modelAndView, "reused");
            }
            if (e instanceof LockedException) {
                return trackAndReturn(tokenType, username, modelAndView, "state");
            }
            // let any other exception bubble up
            throw e;
        }

        log.info("Successfully changed password for {}", username);
        return Optional.empty();
    }

    private MultiValueMap<String, Object> validate(final String username, final String newPassword, final String confirmPassword) {
        final var builder = new LinkedMultiValueMap<String, Object>();
        if (StringUtils.isBlank(newPassword)) {
            builder.add("errornew", "newmissing");
        }
        if (StringUtils.isBlank(confirmPassword)) {
            builder.add("errorconfirm", "confirmmissing");
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
            builder.add("errornew", "alphanumeric");
        }
        final var digits = StringUtils.getDigits(newPassword);
        if (digits.length() == 0) {
            builder.add("errornew", "nodigits");
        }
        if (digits.length() == newPassword.length()) {
            builder.add("errornew", "alldigits");
        }
        if (StringUtils.containsIgnoreCase(newPassword, username)) {
            builder.add("errornew", "username");
        }
        if (newPassword.chars().distinct().count() < 4) {
            builder.add("errornew", "four");
        }

        if (!StringUtils.equals(newPassword, confirmPassword)) {
            builder.add("errorconfirm", "mismatch");
        }
        if (user.getAccountDetail().getAccountProfile() == AccountProfile.TAG_ADMIN) {
            if (newPassword.length() < 14) {
                builder.add("errornew", "length14");
            }
        } else if (newPassword.length() < 9) {
            builder.add("errornew", "length9");
        }

        return builder;
    }

    private Optional<ModelAndView> trackAndReturn(final TokenType tokenType, final String username, final ModelAndView modelAndView,
                                                  final MultiValueMap<String, Object> validationResult) {
        telemetryClient.trackEvent(String.format("%sPasswordFailure", tokenType.getDescription()),
                Map.of("username", username, "reason", validationResult.toString()), null);
        modelAndView.addAllObjects(validationResult);
        modelAndView.addObject("error", Boolean.TRUE);
        return Optional.of(modelAndView);
    }

    private Optional<ModelAndView> trackAndReturn(final TokenType tokenType, final String username, final ModelAndView modelAndView, final String reason) {
        telemetryClient.trackEvent(String.format("%sPasswordFailure", tokenType.getDescription()),
                Map.of("username", username, "reason", reason), null);
        modelAndView.addObject("errornew", reason);
        modelAndView.addObject("error", Boolean.TRUE);
        return Optional.of(modelAndView);
    }
}
