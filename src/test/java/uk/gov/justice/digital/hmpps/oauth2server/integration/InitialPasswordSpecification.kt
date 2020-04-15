package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class InitialPasswordSpecification : AbstractAuthSpecification() {

  @Page
  private lateinit var initialPasswordPage: InitialPasswordPage

  @Page
  private lateinit var resentLinkinitialPasswordPage: ResentLinkInitialPasswordPage

  @Page
  private lateinit var newPasswordSavedPage: NewPasswordSavedPage

  @Page
  private lateinit var homePage: HomePage

  @Page
  private lateinit var newPasswordLinkExpiredPage: NewPasswordLinkExpiredPage

  @Page
  private lateinit var resetPasswordPage: resetPasswordPage

  @Test
  fun `A user can be created and new password saved`() {
    goTo("/initial-password?token=reset3")
    initialPasswordPage
        .isAtPage()
        .inputAndConfirmNewPassword("helloWorld2")

    newPasswordSavedPage
        .isAtPage()
        .selectSignInLink()

    goTo(loginPage).loginAs("AUTH_CREATE_USER_VALID", "helloWorld2")
    homePage.isAt()
  }

  @Test
  fun `A user can be created and password validated`() {
    goTo("/initial-password?token=reset4")
    initialPasswordPage
        .isAtPage()
        .inputAndConfirmNewPassword("password1")
        .checkError("Your password is commonly used and may not be secure")
        .inputAndConfirmNewPassword("helloWorld2")

    newPasswordSavedPage
        .isAtPage()
        .selectSignInLink()

    goTo(loginPage).loginAs("AUTH_CREATE_USER_VALID2", "helloWorld2")
    homePage.isAt()
  }

  @Test
  fun `A user is sent a new link when they use an expired link`() {
    goTo("/initial-password?token=reset5")

    newPasswordLinkExpiredPage
        .isAtPage()

    val newInitialPasswordLink = newPasswordLinkExpiredPage.getInitialPasswordLink()

    goTo(newInitialPasswordLink)

    resentLinkinitialPasswordPage
        .isAtPage()
        .inputAndConfirmNewPassword("helloWorld2")

    newPasswordSavedPage
        .isAtPage()
        .selectSignInLink()

    goTo(loginPage).loginAs("AUTH_CREATE_USER_EXPIRED", "helloWorld2")
    homePage.isAt()
  }

  @Test
  fun `A user tries to use an invalid token to set their password is redirected to reset password page`() {
    goTo("/initial-password?token=invalid")

    resetPasswordPage.isAtPage()
  }

  @Test
  fun `A user tries to use a resent invalid token to set their password is redirected to reset password page`() {
    goTo("/initial-password-expired-confirm?token=invalid")

    resetPasswordPage.isAtPage()
  }
}

@PageUrl("/initial-password")
class InitialPasswordPage : AuthPage<InitialPasswordPage>("HMPPS Digital Services - Create a password", "Create a password") {

  @FindBy(css = "input[id='new-password']")
  private lateinit var newPassword: FluentWebElement

  @FindBy(css = "input[id='confirm-password']")
  private lateinit var confirmPassword: FluentWebElement

  @FindBy(css = "input[type='submit']")
  private lateinit var savePassword: FluentWebElement

  fun inputAndConfirmNewPassword(password: String): InitialPasswordPage {
    this.newPassword.fill().withText(password)
    this.confirmPassword.fill().withText(password)
    savePassword.submit()
    return this
  }
}

@PageUrl("/initial-password-expired-confirm")
class ResentLinkInitialPasswordPage : AuthPage<ResentLinkInitialPasswordPage>("HMPPS Digital Services - Create a password", "Create a password") {

  @FindBy(css = "input[id='new-password']")
  private lateinit var newPassword: FluentWebElement

  @FindBy(css = "input[id='confirm-password']")
  private lateinit var confirmPassword: FluentWebElement

  @FindBy(css = "input[type='submit']")
  private lateinit var savePassword: FluentWebElement

  fun inputAndConfirmNewPassword(password: String): ResentLinkInitialPasswordPage {
    this.newPassword.fill().withText(password)
    this.confirmPassword.fill().withText(password)
    savePassword.submit()
    return this
  }
}

@PageUrl("/initial-password-success")
class NewPasswordSavedPage : AuthPage<NewPasswordSavedPage>("HMPPS Digital Services - New Password Saved", "New password saved") {

  fun selectSignInLink() {
    el("[data-qa='initial-signin']").click()
  }
}

@PageUrl("/initial-password-expired")
class NewPasswordLinkExpiredPage : AuthPage<NewPasswordLinkExpiredPage>("HMPPS Digital Services - Create Password token resent", "Your create a password link has expired") {

  fun getInitialPasswordLink(): String = el("#link").attribute("href")
}

