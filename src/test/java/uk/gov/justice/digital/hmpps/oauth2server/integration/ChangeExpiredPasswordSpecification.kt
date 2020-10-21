package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class ChangeExpiredPasswordSpecification : AbstractAuthSpecification() {

  @Page
  lateinit var changeExpiredPasswordPage: ChangeExpiredPasswordPage

  @Test
  fun `Attempt change password without credentials`() {
    goTo(loginPage).loginExpiredUser("EXPIRED_USER", "password123456")
    changeExpiredPasswordPage
      .isAtPage()
      .inputAndConfirmNewPassword("", "")
      .checkError("Enter your new password\nEnter your new password again")
  }
}

@PageUrl("/change-password")
open class ChangeExpiredPasswordPage : AuthPage<ChangeExpiredPasswordPage>(
  "HMPPS Digital Services - Change Password",
  "Your password has expired"
) {
  @FindBy(css = "input[id='new-password']")
  private lateinit var newPassword: FluentWebElement

  @FindBy(css = "input[id='confirm-password']")
  private lateinit var confirmPassword: FluentWebElement

  @FindBy(css = "input[type='submit']")
  private lateinit var savePassword: FluentWebElement

  fun inputAndConfirmNewPassword(password: String, confirmPassword: String): ChangeExpiredPasswordPage {
    this.newPassword.fill().withText(password)
    this.confirmPassword.fill().withText(confirmPassword)
    savePassword.submit()
    return this
  }
}
