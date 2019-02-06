package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.junit.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginControllerTest {
    private final LoginController controller = new LoginController();

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
}
