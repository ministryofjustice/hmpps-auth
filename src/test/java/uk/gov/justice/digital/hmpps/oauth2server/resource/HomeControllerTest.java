package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class HomeControllerTest {
    @Test
    public void home() {
        final var homeController = new HomeController("bob", "");
        final var modelAndView = homeController.home(authenticationWithRole());
        assertThat(modelAndView.getViewName()).isEqualTo("index");
    }

    @Test
    public void home_NewNomisUrlOnly() {
        final var homeController = new HomeController("bob", "");
        final var modelAndView = homeController.home(authenticationWithRole());
        assertThat(modelAndView.getModel()).containsExactly(entry("nnUrl", "bob"));
    }

    @Test
    public void home_HdcUrlOnly_CA() {
        final var homeController = new HomeController("  ", "bob");
        final var modelAndView = homeController.home(authenticationWithRole("ROLE_LICENCE_CA"));
        assertThat(modelAndView.getModel()).containsExactly(entry("hdcUrl", "bob"));
    }

    @Test
    public void home_HdcUrlOnly_RO() {
        final var homeController = new HomeController("  ", "bob");
        final var modelAndView = homeController.home(authenticationWithRole("ROLE_LICENCE_RO"));
        assertThat(modelAndView.getModel()).containsExactly(entry("hdcUrl", "bob"));
    }

    @Test
    public void home_HdcUrlOnly_DM() {
        final var homeController = new HomeController("  ", "bob");
        final var modelAndView = homeController.home(authenticationWithRole("ROLE_LICENCE_DM"));
        assertThat(modelAndView.getModel()).containsExactly(entry("hdcUrl", "bob"));
    }

    @Test
    public void home_HdcNoRole() {
        final var homeController = new HomeController("  ", "bob");
        final var modelAndView = homeController.home(authenticationWithRole("JOE"));
        assertThat(modelAndView.getModel()).hasSize(0);
    }

    @Test
    public void home_AllUrls() {
        final var homeController = new HomeController("nn", "bob");
        final var modelAndView = homeController.home(authenticationWithRole("ROLE_LICENCE_DM"));
        assertThat(modelAndView.getModel()).contains(entry("hdcUrl", "bob"), entry("nnUrl", "nn"));
    }

    private Authentication authenticationWithRole(final String... roles) {
        final var authorities = Arrays.stream(roles).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        return new UsernamePasswordAuthenticationToken("user", "pass", authorities);
    }
}
