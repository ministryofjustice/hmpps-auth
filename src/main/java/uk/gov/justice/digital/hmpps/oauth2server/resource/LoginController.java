package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class LoginController {
    private final boolean resetPasswordEnabled;

    public LoginController(@Value("${application.reset-password.enabled}") final boolean resetPasswordEnabled) {
        this.resetPasswordEnabled = resetPasswordEnabled;
    }

    @GetMapping("/login")
    public ModelAndView loginPage(@RequestParam(required = false) final String error) {
        final var modelAndView = new ModelAndView("login");
        // send bad request if password wrong so that browser won't offer to save the password
        if (StringUtils.isNotBlank(error)) {
            modelAndView.setStatus(HttpStatus.BAD_REQUEST);
        }
        modelAndView.addObject("resetPasswordEnabled", resetPasswordEnabled);
        return modelAndView;
    }

    @GetMapping(value = "/logout")
    public String logoutPage(final HttpServletRequest request, final HttpServletResponse response) {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/login?logout";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }

}
