package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.digital.hmpps.oauth2server.model.Context;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

import static uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.RESET;

@Slf4j
@Controller
@Validated
public class InitialPasswordController extends AbstractPasswordController {
    private final TokenService tokenService;
    private final String hdcUrl;

    public InitialPasswordController(final ResetPasswordService resetPasswordService,
                                     final TokenService tokenService, final UserService userService,
                                     final TelemetryClient telemetryClient,
                                     final @Value("${application.authentication.blacklist}") Set<String> passwordBlacklist,
                                     @Value("${application.hdc-endpoint-url}") final String hdcUrl) {

        super(resetPasswordService, tokenService, userService, telemetryClient, "resetPassword", "setPassword", passwordBlacklist);
        this.tokenService = tokenService;
        this.hdcUrl = hdcUrl;
    }

    @GetMapping("/initial-password-success")
    public ModelAndView initialPasswordSuccess(final HttpServletRequest request) {
        final var url = StringUtils.defaultIfBlank(hdcUrl, String.format("%s/login", request.getContextPath()));
        return new ModelAndView("initialPasswordSuccess", "hdcUrl", url);
    }

    @GetMapping("/initial-password")
    public ModelAndView initialPassword(@RequestParam final String token) {
        final var userTokenOptional = tokenService.checkToken(RESET, token);
        return userTokenOptional.map(s -> new ModelAndView("resetPassword", "error", s)).
                orElseGet(() -> createModelWithTokenAndAddIsAdmin(RESET, token, "setPassword").addObject("context", Context.LICENCES.getContext()));
    }
}
