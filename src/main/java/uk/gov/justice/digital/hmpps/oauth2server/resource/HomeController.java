package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Set;

@Controller
public class HomeController {
    private final String nnUrl;
    private final String hdcUrl;
    private static final Set<String> LICENCES_ROLES = Set.of("ROLE_LICENCE_CA", "ROLE_LICENCE_RO", "ROLE_LICENCE_DM");


    public HomeController(@Value("${application.nn-endpoint-url}") final String nnUrl,
                          @Value("${application.hdc-endpoint-url}") final String hdcUrl) {
        this.nnUrl = nnUrl;
        this.hdcUrl = hdcUrl;
    }

    @GetMapping("/")
    public ModelAndView home(final Authentication authentication) {
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

    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }

}
