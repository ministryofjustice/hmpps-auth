package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class ChangeEmailSpecification : AbstractDeliusAuthSpecification() {

  @Page
  private lateinit var passwordPromptForEmailPage: PasswordPromptForEmailPage

  @Page
  private lateinit var changeEmailPage: ChangeEmailPage

  @Page
  private lateinit var changeEmailDeliusPage: ChangeEmailDeliusPage

  @Page
  private lateinit var verifyEmailSentPage: VerifyEmailSentPage

  @Page
  private lateinit var homePage: HomePage

  @Page
  private lateinit var verifyEmailConfirmPage: VerifyEmailConfirmPage

  @Page
  private lateinit var verifyEmailErrorPage: VerifyEmailErrorPage

  @Page
  private lateinit var existingPasswordPage: ExistingPasswordPage

  @Page
  private lateinit var accountDetailsPage: AccountDetailsPage

  @Page
  private lateinit var emailAlreadyVerifiedPage: EmailAlreadyVerifiedPage

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

  @Test
  fun `A user is not allowed to change email address to a gsi email address`() {
    goTo(loginPage)
      .loginExistingPasswordChangeEmail("AUTH_CHANGE_EMAIL_GSI", "password123456")
    goTo(passwordPromptForEmailPage)
      .isAtPage()
      .inputAndConfirmCurrentPassword("password123456")
    changeEmailPage
      .isAtPage()
      .inputAndConfirmNewEmail("dm_user@hmps.gsi.gov.uk")
    verifyEmailErrorPage
      .checkError(
        "All gsi.gov.uk have now been migrated to a justice.gov.uk domain. " +
          "Enter your justice.gov.uk address instead."
      )
  }

  @Test
  fun `A user is not allowed to change email address to an invalid email address`() {
    goTo(loginPage)
      .loginExistingPasswordChangeEmail("AUTH_CHANGE_EMAIL_INVALID", "password123456")
    goTo(passwordPromptForEmailPage)
      .isAtPage()
      .inputAndConfirmCurrentPassword("password123456")
    changeEmailPage
      .isAtPage()
      .inputAndConfirmNewEmail("dm_user@digital.justice")
    verifyEmailErrorPage
      .checkError("Enter your work email address")
  }

  @Test
  fun `Change email also changes username`() {
    goTo(loginPage)
      .loginExistingPasswordChangeEmail("auth_change_email@justice.gov.uk")
    goTo(passwordPromptForEmailPage)
      .isAtPage()
      .inputAndConfirmCurrentPassword("password123456")
    changeEmailPage
      .isAtPage()
      .inputAndConfirmNewEmail("auth_change_email_new@justice.gov.uk")
    verifyEmailSentPage
      .isAt()
    // check account details page displays correctly
    goTo(accountDetailsPage).isAtPage()
    // now check that we can login with new email address
    goTo(loginPage).loginAsWithUnverifiedEmail("auth_change_email_new@justice.gov.uk")
  }

  @Test
  fun `Change email rejected if email as username already exists`() {
    goTo(loginPage)
      .loginExistingPasswordChangeEmail("auth_user_email_test@justice.gov.uk")
    goTo(passwordPromptForEmailPage)
      .isAtPage()
      .inputAndConfirmCurrentPassword("password123456")
    changeEmailPage
      .isAtPage()
      .inputAndConfirmNewEmail("auth_user_email@justice.gov.uk")
    verifyEmailErrorPage
      .checkError("There is already an account with this email address. Please login with that email address instead.")
  }

  @Test
  fun `Change email flow with failing email`() {
    goTo(loginPage)
      .loginExistingPasswordChangeEmail("AUTH_CHANGE_EMAIL_INCOMPLETE", "password123456")
    goTo(passwordPromptForEmailPage)
      .isAtPage()
      .inputAndConfirmCurrentPassword("password123456")
    changeEmailPage
      .isAtPage()
      .inputAndConfirmNewEmail("dm_user2@digital.justice")
    verifyEmailErrorPage
      .checkError("Enter your work email address")
      .inputAndConfirmNewEmail("dm_user2@digital.justice.gov.uk")
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
  fun `Delius user change email address flow`() {
    goTo(loginPage)
      .loginExistingPasswordChangeEmail("DELIUS_EMAIL", "password")
    goTo(passwordPromptForEmailPage)
      .isAtPage()
      .inputAndConfirmCurrentPassword("password")
    changeEmailDeliusPage
      .isAtPage()
      .confirm()
    accountDetailsPage
      .isAtPage()
    Assertions.assertThat(accountDetailsPage.getCurrentName()).isEqualTo("Delius Smith")
  }

  @Test
  fun `Change email mfa user`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_CHANGE_EMAIL")
      .submitCode()
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
  fun `Change email flow current verified email re-entered`() {
    goTo(loginPage)
      .loginExistingPasswordChangeEmail("AUTH_CHANGE_EMAIL_VERIFIED", "password123456")
    goTo(passwordPromptForEmailPage)
      .isAtPage()
      .inputAndConfirmCurrentPassword("password123456")
    changeEmailPage
      .isAtPage()
      .inputAndConfirmNewEmail("auth_email@digital.justice.gov.uk")
    emailAlreadyVerifiedPage
      .isAtPage()
      .continueToAccountDetailsPage()
    accountDetailsPage
      .isAt()
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

@PageUrl("/new-email")
open class ChangeEmailDeliusPage : AuthPage<ChangeEmailDeliusPage>(
  "HMPPS Digital Services - Change Email",
  "Delius user - update email address"
) {

  @FindBy(css = "#continue")
  private lateinit var continueButton: FluentWebElement

  fun confirm(): ChangeEmailDeliusPage {
    Assertions.assertThat(continueButton.text()).isEqualTo("OK, continue")
    continueButton.click()
    return this
  }
}

@PageUrl("/verify-email")
open class VerifyEmailErrorPage : AuthPage<VerifyEmailErrorPage>(
  "HMPPS Digital Services - Change Email",
  "What is your new email address?"
) {

  @FindBy(css = "input[id='email']")
  private lateinit var newEmail: FluentWebElement

  @FindBy(css = "input[type='submit']")
  private lateinit var saveNewEmail: FluentWebElement

  fun inputAndConfirmNewEmail(newEmail: String): VerifyEmailErrorPage {
    this.newEmail.fill().withText(newEmail)
    saveNewEmail.submit()
    return this
  }
}
