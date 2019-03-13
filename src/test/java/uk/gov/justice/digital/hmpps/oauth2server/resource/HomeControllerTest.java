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
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HomeControllerTest {
    @Mock
    private LandingService landingService;

    @Test
    public void home() {
        final var homeController = new HomeController(landingService, "bob", "");
        final var modelAndView = homeController.home(authenticationWithRole());
        assertThat(modelAndView.getViewName()).isEqualTo("index");
    }

    @Test
    public void home_NewNomisUrlOnly() {
        final var homeController = new HomeController(landingService, "bob", "");
        final var modelAndView = homeController.home(authenticationWithRole());
        assertThat(modelAndView.getModel()).containsExactly(entry("nnUrl", "bob"));
    }

    @Test
    public void home_HdcUrlOnly_CA() {
        final var homeController = new HomeController(landingService, "  ", "bob");
        final var modelAndView = homeController.home(authenticationWithRole("ROLE_LICENCE_CA"));
        assertThat(modelAndView.getModel()).containsExactly(entry("hdcUrl", "bob"));
    }

    @Test
    public void home_HdcUrlOnly_RO() {
        final var homeController = new HomeController(landingService, "  ", "bob");
        final var modelAndView = homeController.home(authenticationWithRole("ROLE_LICENCE_RO"));
        assertThat(modelAndView.getModel()).containsExactly(entry("hdcUrl", "bob"));
    }

    @Test
    public void home_HdcUrlOnly_DM() {
        final var homeController = new HomeController(landingService, "  ", "bob");
        final var modelAndView = homeController.home(authenticationWithRole("ROLE_LICENCE_DM"));
        assertThat(modelAndView.getModel()).containsExactly(entry("hdcUrl", "bob"));
    }

    @Test
    public void home_HdcNoRole() {
        final var homeController = new HomeController(landingService, "  ", "bob");
        final var modelAndView = homeController.home(authenticationWithRole("JOE"));
        assertThat(modelAndView.getModel()).hasSize(0);
    }

    @Test
    public void home_AllUrls() {
        final var homeController = new HomeController(landingService, "nn", "bob");
        final var modelAndView = homeController.home(authenticationWithRole("ROLE_LICENCE_DM"));
        assertThat(modelAndView.getModel()).contains(entry("hdcUrl", "bob"), entry("nnUrl", "nn"));
    }

    @Test
    public void home_dynamicUrls_model() {
        final var services = List.of(createService(), createService());
        when(landingService.findAllServices()).thenReturn(services);
        final var homeController = new HomeController(landingService, "nn", "bob");
        final var modelAndView = homeController.home(authenticationWithRole("ROLE_LICENCE_DM"));
        assertThat(modelAndView.getModel()).contains(entry("services", services));
    }

    @Test
    public void home_dynamicUrls_view() {
        final var services = List.of(createService(), createService());
        when(landingService.findAllServices()).thenReturn(services);
        final var homeController = new HomeController(landingService, "nn", "bob");
        final var modelAndView = homeController.home(authenticationWithRole("ROLE_LICENCE_DM"));
        assertThat(modelAndView.getViewName()).isEqualTo("landing");
    }

    private Service createService() {
        return new Service("CODE", "NAME", "Description", "SOME_ROLE", "http://some.url", true);
    }

    private Authentication authenticationWithRole(final String... roles) {
        final var authorities = Arrays.stream(roles).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        return new UsernamePasswordAuthenticationToken("user", "pass", authorities);
    }
}
