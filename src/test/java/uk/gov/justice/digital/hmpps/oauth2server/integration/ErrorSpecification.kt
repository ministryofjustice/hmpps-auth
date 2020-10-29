package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class ErrorSpecification : AbstractAuthSpecification() {

  @Page
  private lateinit var errorPage: ErrorPage

  @Test
  fun `Error page shown when error occurs`() {
    goTo(loginPage)
    goTo("login;someerror")
    errorPage
      .isAt()
  }

  @Test
  fun `Accept error`() {
    goTo(errorPage)
      .confirm()
    loginPage
      .isAtPage()
  }

  @PageUrl("/error")
  class ErrorPage : AuthPage<ErrorPage>(
    "Error: HMPPS Digital Services - Error",
    "Sorry, there is a problem with the service"
  ) {
    @FindBy(css = "#continue")
    private lateinit var continueButton: FluentWebElement

    fun confirm(): ErrorPage {
      Assertions.assertThat(continueButton.text()).isEqualTo("OK, continue")
      continueButton.click()
      return this
    }

    override fun isAt() {
      Assertions.assertThat(window().title()).isEqualTo(title)
      if (headingStartsWith) {
        Assertions.assertThat(headingText.text()).startsWith(heading)
      } else {
        Assertions.assertThat(headingText.text()).isEqualTo(heading)
      }
    }
  }
}
