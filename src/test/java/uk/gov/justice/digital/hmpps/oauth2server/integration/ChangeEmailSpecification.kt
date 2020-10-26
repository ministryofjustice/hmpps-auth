package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class ChangeEmailSpecification : AbstractAuthSpecification() {

  @Page
  private lateinit var passwordPromptForEmailPage: PasswordPromptForEmailPage

  @Page
  private lateinit var changeEmailPage: ChangeEmailPage

  @Page
  private lateinit var verifyEmailSentPage: VerifyEmailSentPage

  @Page
  private lateinit var homePage: HomePage

  @Page
  private lateinit var verifyEmailConfirmPage: VerifyEmailConfirmPage

  @Page
  private lateinit var existingPasswordPage: ExistingPasswordPage

  @Test
  fun `Change email flow`() {
    goTo(loginPage)
      .loginExistingPasswordChangeEmail("AUTH_CHANGE_EMAIL", "password123456")
    goTo(passwordPromptForEmailPage)
      .isAtPage()
      .inputAndConfirmCurrentPassword("password123456")
    changeEmailPage
      .isAtPage()
      .inputAndConfirmNewEmail("dm_user@digital.justice.gov.uk")
    verifyEmailSentPage
      .isAt()
    val verifyLink = verifyEmailSentPage.getVerifyLink()
    verifyEmailSentPage.continueProcess()
    homePage
      .isAtPage()

    goTo(verifyLink)
    verifyEmailConfirmPage.isAt()

    goTo(verifyLink)
    verifyEmailConfirmPage.isAt()
  }

  @Test
  fun `Change email flow incorrect password`() {
    goTo(loginPage)
      .loginExistingPasswordChangeEmail("AUTH_CHANGE_EMAIL2", "password123456")
    goTo(passwordPromptForEmailPage)
      .isAtPage()
      .inputAndConfirmCurrentPassword("password1234567")
    existingPasswordPage
      .checkError(
        "Your password is incorrect. You will be locked out if you enter the wrong details 3 times."
      )
      .inputAndConfirmCurrentPassword("password123456")
    changeEmailPage
      .isAtPage()
      .inputAndConfirmNewEmail("dm_user1@digital.justice.gov.uk")
    verifyEmailSentPage
      .isAt()
    val verifyLink = verifyEmailSentPage.getVerifyLink()
    verifyEmailSentPage.continueProcess()
    homePage
      .isAtPage()

    goTo(verifyLink)
    verifyEmailConfirmPage.isAt()

    goTo(verifyLink)
    verifyEmailConfirmPage.isAt()
  }
}

@PageUrl("/existing-email")
open class PasswordPromptForEmailPage : AuthPage<PasswordPromptForEmailPage>(
  "HMPPS Digital Services - Change Email Request",
  "What is your current password?"
) {

  @FindBy(css = "input[id='password']")
  private lateinit var currentPassword: FluentWebElement

  @FindBy(css = "input[type='submit']")
  private lateinit var savePassword: FluentWebElement

  fun inputAndConfirmCurrentPassword(confirmPassword: String): PasswordPromptForEmailPage {
    this.currentPassword.fill().withText(confirmPassword)
    savePassword.submit()
    return this
  }
}

@PageUrl("/existing-password")
open class ExistingPasswordPage : AuthPage<ExistingPasswordPage>(
  "HMPPS Digital Services - Change Email Request",
  "What is your current password?"
) {

  @FindBy(css = "input[id='password']")
  private lateinit var currentPassword: FluentWebElement

  @FindBy(css = "input[type='submit']")
  private lateinit var savePassword: FluentWebElement

  fun inputAndConfirmCurrentPassword(confirmPassword: String): ExistingPasswordPage {
    this.currentPassword.fill().withText(confirmPassword)
    savePassword.submit()
    return this
  }
}

@PageUrl("/new-email")
open class ChangeEmailPage : AuthPage<ChangeEmailPage>(
  "HMPPS Digital Services - Change Email",
  "What is your new email address?"
) {

  @FindBy(css = "input[id='email']")
  private lateinit var newEmail: FluentWebElement

  @FindBy(css = "input[type='submit']")
  private lateinit var saveNewEmail: FluentWebElement

  fun inputAndConfirmNewEmail(newEmail: String): ChangeEmailPage {
    this.newEmail.fill().withText(newEmail)
    saveNewEmail.submit()
    return this
  }
}
