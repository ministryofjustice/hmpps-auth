package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.assertj.core.data.MapEntry;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginControllerTest {
    private final LoginController controller = new LoginController(true);

    @Test
    public void loginPage_NoError() {
        final var modelAndView = controller.loginPage(null);
        assertThat(modelAndView.getViewName()).isEqualTo("login");
        assertThat(modelAndView.getStatus()).isNull();
    }

    @Test
    public void loginPage_CurrentError() {
        final var modelAndView = controller.loginPage("bad");
        assertThat(modelAndView.getViewName()).isEqualTo("login");
        assertThat(modelAndView.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void loginPage_ResetDisabled() {
        final var modelAndView = new LoginController(false).loginPage(null);
        assertThat(modelAndView.getModel()).containsExactly(MapEntry.entry("resetPasswordEnabled", Boolean.FALSE));
        assertThat(modelAndView.getStatus()).isNull();
    }

    @Test
    public void loginPage_ResetEnabled() {
        final var modelAndView = new LoginController(true).loginPage(null);
        assertThat(modelAndView.getModel()).containsExactly(MapEntry.entry("resetPasswordEnabled", Boolean.TRUE));
        assertThat(modelAndView.getStatus()).isNull();
    }
}
