package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class ResetPasswordSpecification : AbstractAuthSpecification() {

  @Page
  private lateinit var resetPasswordRequestPage: ResetPasswordRequestPage

  @Page
  private lateinit var resetPasswordLinkSentPage: ResetPasswordLinkSentPage

  @Page
  private lateinit var resetPasswordUsernamePage: ResetPasswordUsernamePage

  @Page
  private lateinit var resetPasswordPage: ResetPasswordPage

  @Page
  private lateinit var setNewPasswordPage: SetNewPasswordPage

  @Page
  private lateinit var usernameResetPasswordPage: UsernameResetPasswordPage

  @Page
  private lateinit var resetPasswordPageInvalidToken: ResetPasswordPageInvalidToken

  @Page
  private lateinit var resetPasswordSuccessPage: ResetPasswordSuccessPage

  @Page
  private lateinit var homePage: HomePage

  @Test
  fun `A user can cancel reset password`() {
    goTo(loginPage)
        .forgottenPasswordLink()

    resetPasswordRequestPage.isAtPage()
        .cancel()

    loginPage.isAtPage()
  }

  @Test
  fun `A user must enter a valid email address`() {
    goTo(loginPage)
        .forgottenPasswordLink()

    resetPasswordRequestPage.isAtPage()
        .submitUsernameOrEmail("joe@bloggs.com")
        .checkError("Enter your work email address")
        .assertUsernameOrEmailText("joe@bloggs.com")
  }

  @Test
  fun `A user can enter their gsi email address to reset their password`() {
    goTo(loginPage)
        .forgottenPasswordLink()

    resetPasswordRequestPage.isAtPage()
        .submitUsernameOrEmail("reset_test@hmps.gsi.gov.uk")

    resetPasswordLinkSentPage.isAtPage()
  }

  @Test
  fun `A user can reset their password by email address`() {
    goTo(loginPage)
        .forgottenPasswordLink()

    resetPasswordRequestPage
        .submitUsernameOrEmail("reset_test@digital.justice.gov.uk")

    resetPasswordLinkSentPage.isAtPage()
    val resetLink = resetPasswordLinkSentPage.getResetLink()

    goTo(resetLink)

    resetPasswordUsernamePage
        .inputUsernameAndContinue("EXPIRED_TEST2_USER")
        .checkError("The username entered is not linked to your email address")
        .inputUsernameAndContinue("CA_USER_TEST")

    usernameResetPasswordPage
        .inputAndConfirmNewPassword("helloworld2")

    resetPasswordSuccessPage.isAtPage()

    goTo(loginPage)
        .loginAs("CA_USER_TEST", "helloworld2")
    homePage.isAt()
  }

  @Test
  fun `A user can reset their password by username`() {
    goTo(loginPage)
        .forgottenPasswordLink()

    resetPasswordRequestPage
        .submitUsernameOrEmail("RESET_TEST_USER")

    resetPasswordLinkSentPage.isAtPage()
    val resetLink = resetPasswordLinkSentPage.getResetLink()

    goTo(resetLink)

    resetPasswordPage
        .inputAndConfirmNewPassword("helloworld2")

    resetPasswordSuccessPage.isAtPage()

    goTo(loginPage)
        .loginAs("RESET_TEST_USER", "helloworld2")
    homePage.isAt()
  }

  @Test
  fun `An auth user can reset their password`() {
    goTo(loginPage)
        .forgottenPasswordLink()

    resetPasswordRequestPage
        .submitUsernameOrEmail("AUTH_LOCKED2")

    resetPasswordLinkSentPage.isAtPage()
    val resetLink = resetPasswordLinkSentPage.getResetLink()

    goTo(resetLink)

    resetPasswordPage
        .inputAndConfirmNewPassword("helloworld2")

    resetPasswordSuccessPage.isAtPage()

    goTo(loginPage)
        .loginAs("AUTH_LOCKED2", "helloworld2")
    homePage.isAt()
  }

  @Test
  fun `A DELIUS user can reset their password`() {
    goTo(loginPage)
        .forgottenPasswordLink()

    resetPasswordRequestPage
        .submitUsernameOrEmail("DELIUS_PASSWORD_RESET")

    resetPasswordLinkSentPage.isAtPage()
    val resetLink = resetPasswordLinkSentPage.getResetLink()

    goTo(resetLink)

    resetPasswordPage
        .inputAndConfirmNewPassword("helloworld2")

    resetPasswordSuccessPage.isAtPage()

    goTo(loginPage)
        .loginAs("DELIUS_PASSWORD_RESET", "helloworld2")
    homePage.isAt()
  }

  @Test
  fun `A user can reset their password back with lowercase username`() {
    goTo(loginPage)
        .forgottenPasswordLink()

    resetPasswordRequestPage
        .submitUsernameOrEmail("reset_test_user")

    resetPasswordLinkSentPage.isAtPage()
    val resetLink = resetPasswordLinkSentPage.getResetLink()

    goTo(resetLink)

    resetPasswordPage
        .inputAndConfirmNewPassword("helloworld2")

    resetPasswordSuccessPage.isAtPage()

    goTo(loginPage)
        .loginAs("reset_test_user", "helloworld2")
    homePage.isAt()
  }

  @Test
  fun `Attempt reset password without credentials`() {
    goTo(loginPage)
        .forgottenPasswordLink()

    resetPasswordRequestPage
        .submitUsernameOrEmail("RESET_TEST_USER")

    resetPasswordLinkSentPage.isAtPage()
    val resetLink = resetPasswordLinkSentPage.getResetLink()

    goTo(resetLink)

    resetPasswordPage
        .inputAndConfirmNewPassword("", "")
    setNewPasswordPage
        .checkError("Enter your new password\n" +
            "Enter your new password again")
        .inputAndConfirmNewPassword("somepass", "d")
        .checkError("Your password must have both letters and numbers\n" +
            "Your password must have at least 9 characters\n" +
            "Your passwords do not match. Enter matching passwords.")
  }

  @Test
  fun `A user is asked to reset password again if the reset link is invalid`() {
    goTo("/reset-password-confirm?token=someinvalidtoken")

    resetPasswordPageInvalidToken.checkError("This link is invalid. Please enter your username or email address and try again.")
  }

  @Test
  fun `A NOMIS user who has never logged into DPS can reset password`() {
    goTo(loginPage)
        .forgottenPasswordLink()

    resetPasswordRequestPage
        .submitUsernameOrEmail("NOMIS_NEVER_LOGGED_IN")

    resetPasswordLinkSentPage.isAtPage()
    val resetLink = resetPasswordLinkSentPage.getResetLink()

    goTo(resetLink)

    resetPasswordPage
        .inputAndConfirmNewPassword("helloworld2")

    resetPasswordSuccessPage.isAtPage()

    goTo(loginPage)
        .loginAs("NOMIS_NEVER_LOGGED_IN", "helloworld2")
    homePage.isAt()
  }

  @Test
  fun `A Delius user who has never logged into DPS can reset password`() {
    goTo(loginPage)
        .forgottenPasswordLink()

    resetPasswordRequestPage
        .submitUsernameOrEmail("DELIUS_PASSWORD_NEW")

    resetPasswordLinkSentPage.isAtPage()
    val resetLink = resetPasswordLinkSentPage.getResetLink()

    goTo(resetLink)

    resetPasswordPage
        .inputAndConfirmNewPassword("helloworld2")

    resetPasswordSuccessPage.isAtPage()

    goTo(loginPage)
        .loginAs("DELIUS_PASSWORD_NEW", "helloworld2")
    homePage.isAt()
  }


}

@PageUrl("/reset-password")
open class ResetPasswordRequestPage : AuthPage<ResetPasswordRequestPage>("HMPPS Digital Services - Reset Password", "Create a new password") {
  @FindBy(css = "input[type='submit']")
  private lateinit var continueButton: FluentWebElement
  private lateinit var usernameOrEmail: FluentWebElement

  fun submitUsernameOrEmail(usernameOrEmail: String): ResetPasswordRequestPage {
    this.usernameOrEmail.fill().withText(usernameOrEmail)
    assertThat(continueButton.value()).isEqualTo("Continue")
    continueButton.click()
    return this
  }

  fun assertUsernameOrEmailText(email: String) {
    assertThat(this.usernameOrEmail.value()).isEqualTo(email)
  }

  fun cancel() {
    el("[data-qa='back-link']").click()
  }
}

@PageUrl("/reset-password")
open class ResetPasswordLinkSentPage : AuthPage<ResetPasswordLinkSentPage>("HMPPS Digital Services - Reset Password Email Sent", "Check your email") {

  fun getResetLink(): String = el("#resetLink").attribute("href")
}

@PageUrl("/reset-password-select")
open class ResetPasswordUsernamePage : AuthPage<ResetPasswordUsernamePage>("HMPPS Digital Services - Set Password Select", "Enter your username") {
  @FindBy(css = "input[id='username']")
  private lateinit var username: FluentWebElement

  @FindBy(css = "input[type='submit']")
  private lateinit var continueButton: FluentWebElement

  fun inputUsernameAndContinue(username: String): ResetPasswordUsernamePage {
    this.username.fill().withText(username)
    continueButton.submit()
    return this
  }
}

@PageUrl("/reset-password-confirm")
open class ResetPasswordPage : AuthPage<ResetPasswordPage>("HMPPS Digital Services - Create a password", "Create a new password") {
  @FindBy(css = "input[id='new-password']")
  private lateinit var newPassword: FluentWebElement

  @FindBy(css = "input[id='confirm-password']")
  private lateinit var confirmPassword: FluentWebElement

  @FindBy(css = "input[type='submit']")
  private lateinit var savePassword: FluentWebElement

  fun inputAndConfirmNewPassword(password: String, confirmPassword: String = password): ResetPasswordPage {
    this.newPassword.fill().withText(password)
    this.confirmPassword.fill().withText(confirmPassword)
    savePassword.submit()
    return this
  }
}

//Duplicate of ResetPasswordPage as resetPasswordChosen method needs redirects implementing to update the url
@PageUrl("/reset-password-select")
open class UsernameResetPasswordPage : AuthPage<UsernameResetPasswordPage>("HMPPS Digital Services - Create a password", "Create a new password") {
  @FindBy(css = "input[id='new-password']")
  private lateinit var newPassword: FluentWebElement

  @FindBy(css = "input[id='confirm-password']")
  private lateinit var confirmPassword: FluentWebElement

  @FindBy(css = "input[type='submit']")
  private lateinit var savePassword: FluentWebElement

  fun inputAndConfirmNewPassword(password: String, confirmPassword: String = password): UsernameResetPasswordPage {
    this.newPassword.fill().withText(password)
    this.confirmPassword.fill().withText(confirmPassword)
    savePassword.submit()
    return this
  }
}

@PageUrl("/reset-password-confirm")
open class ResetPasswordPageInvalidToken : AuthPage<ResetPasswordPageInvalidToken>("HMPPS Digital Services - Reset Password", "Create a new password")

@PageUrl("/reset-password-success")
open class ResetPasswordSuccessPage : AuthPage<ResetPasswordSuccessPage>("HMPPS Digital Services - Reset Password Success", "Reset password successful")


