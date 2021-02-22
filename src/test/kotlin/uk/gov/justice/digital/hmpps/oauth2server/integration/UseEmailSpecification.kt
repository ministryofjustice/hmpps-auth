package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class UseEmailSpecification : AbstractAuthSpecification() {
  @Page
  private lateinit var useEmailPage: UseEmailPage

  @Page
  private lateinit var accountDetailsPage: AccountDetailsPage

  @Test
  fun `A user can change their user details`() {
    goTo(loginPage).loginAs("AUTH_CHANGE_TO_USERNAME")

    goTo(accountDetailsPage).isAtPage().checkUsername("AUTH_CHANGE_TO_USERNAME")
    goTo(useEmailPage).submit()

    accountDetailsPage.isAt()
    accountDetailsPage.checkEmailUsername("auth_change@justice.gov.uk")

    goTo(loginPage).loginAs("auth_change@justice.gov.uk")
  }
}

@PageUrl("/use-email")
open class UseEmailPage :
  AuthPage<UseEmailPage>("HMPPS Digital Services - Use email to sign in", "Use email address") {
  @FindBy(css = "input[type='submit']")
  private lateinit var saveButton: FluentWebElement

  fun submit(): UseEmailPage {
    saveButton.click()
    return this
  }
}
