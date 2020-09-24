package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
public class LoginController {
    final private List<ClientRegistration> clientRegistrations;

    public LoginController(final Optional<InMemoryClientRegistrationRepository> clientRegistrationRepository) {
        clientRegistrations = clientRegistrationRepository.map(registrations -> StreamSupport
                .stream(registrations.spliterator(), false)
                .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @GetMapping("/login")
    public ModelAndView loginPage(@RequestParam(required = false) final String error) {
        final var modelAndView = new ModelAndView("login", Collections.singletonMap("oauth2Clients", clientRegistrations));
        // send bad request if password wrong so that browser won't offer to save the password
        if (StringUtils.isNotBlank(error)) {
            modelAndView.setStatus(HttpStatus.BAD_REQUEST);
        }
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
