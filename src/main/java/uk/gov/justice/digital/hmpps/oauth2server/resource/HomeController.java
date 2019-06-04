package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.digital.hmpps.oauth2server.landing.LandingService;

import java.util.stream.Collectors;

@Controller
public class HomeController {
    private final LandingService landingService;

    public HomeController(final LandingService landingService) {
        this.landingService = landingService;
    }

    @GetMapping("/")
    public ModelAndView home(final Authentication authentication) {
        final var services = landingService.findAllServices();

        // create a list of services that the user can see
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
