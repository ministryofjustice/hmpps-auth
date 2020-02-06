package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class LoginControllerTest {
  private val controller = LoginController()
  @Test
  fun loginPage_NoError() {
    val modelAndView = controller.loginPage(null)
    assertThat(modelAndView.viewName).isEqualTo("login")
    assertThat(modelAndView.status).isNull()
  }

  @Test
  fun loginPage_CurrentError() {
    val modelAndView = controller.loginPage("bad")
    assertThat(modelAndView.viewName).isEqualTo("login")
    assertThat(modelAndView.status).isEqualTo(HttpStatus.BAD_REQUEST)
  }
}
