package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.justice.digital.hmpps.oauth2server.config.CookieRequestCache;

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
    private final CookieRequestCache cookieRequestCache;
    private ClientDetailsService clientDetailsService;

    public LoginController(final Optional<InMemoryClientRegistrationRepository> clientRegistrationRepository,
                           final CookieRequestCache cookieRequestCache,
                           final ClientDetailsService clientDetailsService) {
        clientRegistrations = clientRegistrationRepository.map(registrations -> StreamSupport
                .stream(registrations.spliterator(), false)
                .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
        this.cookieRequestCache = cookieRequestCache;
        this.clientDetailsService = clientDetailsService;
    }

    @GetMapping("/login")
    public ModelAndView loginPage(@RequestParam(required = false) final String error,
                                  final HttpServletRequest request, final HttpServletResponse response) {
        
        final var savedRequest = cookieRequestCache.getRequest(request, response);
        if (savedRequest != null) {
            final var redirectUrl = UriComponentsBuilder.fromUriString(savedRequest.getRedirectUrl()).build();
            final String clientId = redirectUrl.getQueryParams().getFirst("client_id");

            final var isOAuthLogin = redirectUrl.getPath().endsWith("/oauth/authorize") && clientId != null;

            if(isOAuthLogin) {
                final var clientDetails = clientDetailsService.loadClientByClientId(clientId);

                final Boolean skipToAzure = (Boolean) clientDetails.getAdditionalInformation().getOrDefault("skipToAzureField", false);

                if (skipToAzure) {
                    return new ModelAndView("redirect:/oauth2/authorization/" + clientRegistrations.get(0).getClientName());
                }
            }

        }

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
