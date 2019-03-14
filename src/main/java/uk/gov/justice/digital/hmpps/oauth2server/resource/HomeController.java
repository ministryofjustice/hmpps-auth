package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.digital.hmpps.oauth2server.landing.LandingService;

import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class HomeController {
    private static final Set<String> LICENCES_ROLES = Set.of("ROLE_LICENCE_CA", "ROLE_LICENCE_RO", "ROLE_LICENCE_DM");

    private final String nnUrl;
    private final String hdcUrl;

    private final LandingService landingService;

    public HomeController(final LandingService landingService,
                          @Value("${application.nn-endpoint-url}") final String nnUrl,
                          @Value("${application.hdc-endpoint-url}") final String hdcUrl) {
        this.landingService = landingService;
        this.nnUrl = nnUrl;
        this.hdcUrl = hdcUrl;
    }

    @GetMapping("/")
    public ModelAndView home(final Authentication authentication) {
        final var services = landingService.findAllServices();

        // fallback to current behaviour if no services defined
        if (services.isEmpty()) {
            final var canUseLicences = authentication.getAuthorities().stream().anyMatch((auth) -> LICENCES_ROLES.contains(auth.getAuthority()));

            final var modelAndView = new ModelAndView("index");
            if (StringUtils.isNotBlank(nnUrl)) {
                modelAndView.addObject("nnUrl", nnUrl);
            }
            if (canUseLicences && StringUtils.isNotBlank(hdcUrl)) {
                modelAndView.addObject("hdcUrl", hdcUrl);
            }
            return modelAndView;
        }

        // otherwise create a list of services that the user can see
        final var allowedServices = services.stream().
                filter((s) -> s.getRoles().isEmpty() || authentication.getAuthorities().stream().anyMatch((a) -> s.getRoles().contains(a.getAuthority()))).
                collect(Collectors.toList());
        return new ModelAndView("landing", "services", allowedServices);
    }

    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }

}
