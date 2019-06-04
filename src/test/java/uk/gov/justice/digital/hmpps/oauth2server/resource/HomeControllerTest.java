package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service;
import uk.gov.justice.digital.hmpps.oauth2server.landing.LandingService;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HomeControllerTest {
    @Mock
    private LandingService landingService;

    @Test
    public void home() {
        final var homeController = new HomeController(landingService);
        final var modelAndView = homeController.home(authenticationWithRole());
        assertThat(modelAndView.getViewName()).isEqualTo("landing");
    }

    @Test
    public void home_dynamicUrls_model() {
        final var services = List.of(
                createService("DM", "ROLE_LICENCE_DM"), // single role
                createService("LIC", "ROLE_LICENCE_CA,ROLE_LICENCE_DM,ROLE_LICENCE_RO"), // multiple role
                createService("NOMIS", null), // available to all roles
                createService("OTHER", "ROLE_OTHER")); // not available
        when(landingService.findAllServices()).thenReturn(services);
        final var homeController = new HomeController(landingService);
        final var modelAndView = homeController.home(authenticationWithRole("ROLE_LICENCE_DM"));
        //noinspection unchecked
        final var allocatedServices = (List<Service>) modelAndView.getModel().get("services");
        assertThat(allocatedServices).extracting(Service::getCode).containsExactly("DM", "LIC", "NOMIS");
    }

    @Test
    public void home_dynamicUrls_view() {
        final var services = List.of(createService("DM", "ROLE_LICENCE_DM"));
        when(landingService.findAllServices()).thenReturn(services);
        final var homeController = new HomeController(landingService);
        final var modelAndView = homeController.home(authenticationWithRole("ROLE_LICENCE_DM"));
        assertThat(modelAndView.getViewName()).isEqualTo("landing");
    }

    private Service createService(final String code, final String roles) {
        return new Service(code, "NAME", "Description", roles, "http://some.url", true);
    }

    private Authentication authenticationWithRole(final String... roles) {
        final var authorities = Arrays.stream(roles).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        return new UsernamePasswordAuthenticationToken("user", "pass", authorities);
    }
}
