package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.digital.hmpps.oauth2server.security.ChangePasswordService;
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.CHANGE;

@Slf4j
@Controller
@Validated
public class ChangePasswordController extends AbstractPasswordController {
    private final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final TelemetryClient telemetryClient;

    public ChangePasswordController(final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler,
                                    final AuthenticationManager authenticationManager,
                                    final ChangePasswordService changePasswordService,
                                    final TokenService tokenService, final UserService userService,
                                    final TelemetryClient telemetryClient,
                                    final @Value("${application.authentication.blacklist}") Set<String> passwordBlacklist) {
        super(changePasswordService, tokenService, userService, telemetryClient, "redirect:/login?error=%s", "changePassword", passwordBlacklist);
        this.jwtAuthenticationSuccessHandler = jwtAuthenticationSuccessHandler;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.telemetryClient = telemetryClient;
    }

    @GetMapping("/change-password")
    public ModelAndView changePasswordRequest(@RequestParam final String token) {
        return createModelWithTokenAndAddIsAdmin(CHANGE, token, "changePassword");
    }

    @PostMapping("/change-password")
    public ModelAndView changePassword(@RequestParam final String token,
                                       @RequestParam final String newPassword, @RequestParam final String confirmPassword,
                                       final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {

        final var userToken = tokenService.getToken(CHANGE, token);

        final var modelAndView = processSetPassword(CHANGE, "Change", token, newPassword, confirmPassword);
        if (modelAndView.isPresent()) {
            return modelAndView.get();
        }

        // will be error if unable to get token here as set password process has been successful
        final var username = userToken.orElseThrow().getUser().getUsername();

        // authentication with new password
        try {
            final var successToken = authenticate(username, newPassword);
            // success, so forward on
            telemetryClient.trackEvent("ChangePasswordAuthenticateSuccess", Map.of("username", username), null);
            jwtAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, successToken);
            // return here is not required, since the success handler will have redirected
            return null;
        } catch (final AuthenticationException e) {
            final var reason = e.getClass().getSimpleName();
            log.info("Caught unexpected {} after change password", reason, e);
            telemetryClient.trackEvent("ChangePasswordAuthenticateFailure", Map.of("username", username, "reason", reason), null);
            // this should have succeeded, but unable to login
            // need to tell user that the change password request has been successful though
            return new ModelAndView("redirect:/login?error=changepassword");
        }
    }

    private Authentication authenticate(final String username, final String password) {
        final var token = new UsernamePasswordAuthenticationToken(username.toUpperCase(), password);
        return authenticationManager.authenticate(token);
    }
}
