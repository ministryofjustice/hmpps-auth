package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class HomeControllerTest {
    @Test
    public void home() {
        final var homeController = new HomeController("bob", "");
        final var modelAndView = homeController.home();
        assertThat(modelAndView.getViewName()).isEqualTo("index");
    }

    @Test
    public void home_NewNomisUrlOnly() {
        final var homeController = new HomeController("bob", "");
        final var modelAndView = homeController.home();
        assertThat(modelAndView.getModel()).containsExactly(entry("nnUrl", "bob"));
    }

    @Test
    public void home_HdcUrlOnly() {
        final var homeController = new HomeController("  ", "bob");
        final var modelAndView = homeController.home();
        assertThat(modelAndView.getModel()).containsExactly(entry("hdcUrl", "bob"));
    }

    @Test
    public void home_AllUrls() {
        final var homeController = new HomeController("nn", "bob");
        final var modelAndView = homeController.home();
        assertThat(modelAndView.getModel()).contains(entry("hdcUrl", "bob"), entry("nnUrl", "nn"));
    }
}
