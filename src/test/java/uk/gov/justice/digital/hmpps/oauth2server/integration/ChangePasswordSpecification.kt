package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class ChangePasswordSpecification : AbstractAuthSpecification() {

  @Page
  private lateinit var accountDetailsPage: AccountDetailsPage

  @Page
  private lateinit var homePage: HomePage

  @Page
  private lateinit var passwordPromptPage: PasswordPromptPage

  @Page
  private lateinit var newPasswordPage: NewPasswordPage

  @Page
  private lateinit var changePasswordSuccessPage: ChangePasswordSuccessPage

  @Test
  fun `Change password no current password entered`() {
    goTo(loginPage)
        .loginAs("CA_USER")

    homePage.navigateToAccountDetails()

    accountDetailsPage
        .isAtPage()
        .navigateToChangePassword()

    passwordPromptPage
        .isAtPage()
        .enterCurrentPassword("")
        .checkError("Enter your current password")
  }

  @Test
  fun `Change password wrong current locks account`() {
    goTo(loginPage)
        .loginAs("AUTH_CHANGE_TEST")

    homePage.navigateToAccountDetails()

    accountDetailsPage
        .isAtPage()
        .navigateToChangePassword()

    passwordPromptPage
        .isAtPage()
        .enterCurrentPassword("wrongpass")
        .checkError("Your password is incorrect. You will be locked out if you enter the wrong details 3 times.")
        .enterCurrentPassword("wrongpass")
        .checkError("Your password is incorrect. You will be locked out if you enter the wrong details 3 times.")
        .enterCurrentPassword("wrongpass")

    loginPage
        .checkLoginAccountLockedError()
        .loginError("AUTH_CHANGE_TEST")
        .checkLoginAccountLockedError()
  }

  @Test
  fun `Change password flow`() {
    goTo(loginPage)
        .loginAs("AUTH_CHANGE2_TEST")

    homePage.navigateToAccountDetails()

    accountDetailsPage
        .isAtPage()
        .navigateToChangePassword()

    passwordPromptPage
        .isAtPage()
        .enterCurrentPassword("password123456")

    newPasswordPage
        .isAtPage()
        .inputAndConfirmNewPassword("newpass123", "differentpass")
        .checkError("Your passwords do not match. Enter matching passwords.")
        .inputAndConfirmNewPassword("newpass123", "newpass123")

    changePasswordSuccessPage
        .isAtPage()
        .continueToAccountDetailsPage()
        .logOut()

    goTo(loginPage).loginAs("AUTH_CHANGE2_TEST", "newpass123")
    homePage.isAt()
  }

  @Test
  fun `Change password MFA user`() {
    goTo(loginPage)
        .loginWithMfaEmail("AUTH_MFA_CHANGE")
        .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage
        .isAtPage()
        .navigateToChangePassword()

    passwordPromptPage
        .isAtPage()
        .enterCurrentPassword("password123456")

    newPasswordPage
        .isAtPage()
        .inputAndConfirmNewPassword("helloworld2", "helloworld2")

    changePasswordSuccessPage
        .continueToAccountDetailsPage()
        .logOut()

    goTo(loginPage)
        .loginWithMfaEmail("AUTH_MFA_CHANGE", "helloworld2")
        .submitCode()
    homePage.isAt()
  }
}

@PageUrl("/existing-password")
open class PasswordPromptPage : AuthPage<PasswordPromptPage>("HMPPS Digital Services - Change Password Request", "What is your current password?") {
  @FindBy(css = "input[type='submit']")
  private lateinit var continueButton: FluentWebElement

  @FindBy(css = "input[name='password']")
  private lateinit var existingPassword: FluentWebElement

  fun enterCurrentPassword(existingPassword: String): PasswordPromptPage {
    this.existingPassword.fill().withText(existingPassword)
    assertThat(continueButton.value()).isEqualTo("Continue")
    continueButton.click()
    return this
  }
}

@PageUrl("/new-password")
open class NewPasswordPage : AuthPage<NewPasswordPage>("HMPPS Digital Services - Change Password", "Create a new password") {
  @FindBy(css = "input[id='new-password']")
  private lateinit var newPassword: FluentWebElement

  @FindBy(css = "input[id='confirm-password']")
  private lateinit var confirmPassword: FluentWebElement

  @FindBy(css = "input[type='submit']")
  private lateinit var savePassword: FluentWebElement

  fun inputAndConfirmNewPassword(password: String, confirmPassword: String): NewPasswordPage {
    this.newPassword.fill().withText(password)
    this.confirmPassword.fill().withText(confirmPassword)
    savePassword.submit()
    return this
  }
}

@PageUrl("/change-password-success")
open class ChangePasswordSuccessPage : AuthPage<ChangePasswordSuccessPage>("HMPPS Digital Services - Password Changed Confirmation", "Your password has been changed") {
  @FindBy(css = "#continue")
  private lateinit var continueButton: FluentWebElement

  fun continueToAccountDetailsPage(): ChangePasswordSuccessPage {
    assertThat(continueButton.text()).isEqualTo("Continue")
    continueButton.click()
    return this
  }
}


@PageUrl("/reset-password")
open class resetPasswordPage : AuthPage<resetPasswordPage>("HMPPS Digital Services - Reset Password", "Create a new password")
