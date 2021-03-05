package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
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

  @Page
  private lateinit var accountMfaEmailPage: AccountMfaEmailPage

  @Page
  private lateinit var accountMfaTextPage: AccountMfaTextPage

  @Page
  private lateinit var accountMfaEmailResendCodePage: AccountMfaEmailResendCodePage

  @Page
  private lateinit var accountMfaTextResendCodePage: AccountMfaTextResendCodePage

  @Page
  private lateinit var mfaEmailPage: MfaEmailPage

  @Page
  private lateinit var mfaTextPage: MfaTextPage

  @Test
  fun `Change email flow`() {
    goTo(loginPage)
      .loginExistingPasswordChangeEmail("AUTH_CHANGE_EMAIL", "password123456")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage
      .isAtPage()
      .inputAndConfirmCurrentPassword("password123456")
    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("auth******@******.gov.uk")
      .submitCode(validMfaCode)
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
    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("auth_******@******.gov.uk")
      .submitCode(validMfaCode)
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
    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("auth_******@******.gov.uk")
      .submitCode(validMfaCode)
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
    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("auth_******@******.gov.uk")
      .submitCode(validMfaCode)
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
    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("auth_c******@******.gov.uk")
      .submitCode(validMfaCode)
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
    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("auth_u******@******.gov.uk")
      .submitCode(validMfaCode)
    changeEmailPage
      .isAtPage()
      .inputAndConfirmNewEmail("auth_user_email@justice.gov.uk")
    verifyEmailErrorPage
      .checkError("There is already an account with this email address. Please sign in with that email address instead.")
  }

  @Test
  fun `Change email flow with failing email`() {
    goTo(loginPage)
      .loginExistingPasswordChangeEmail("AUTH_CHANGE_EMAIL_INCOMPLETE", "password123456")
    goTo(passwordPromptForEmailPage)
      .isAtPage()
      .inputAndConfirmCurrentPassword("password123456")
    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("auth_******@******.gov.uk")
      .submitCode(validMfaCode)
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
    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("te******@******.gov.uk")
      .submitCode(validMfaCode)
    changeEmailDeliusPage
      .isAtPage()
      .confirm()
    accountDetailsPage
      .isAtPage()
    assertThat(accountDetailsPage.getCurrentName()).isEqualTo("D. Smith")
  }

  @Test
  fun `Change email mfa user`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_CHANGE_EMAIL")
      .submitCode()
    goTo(passwordPromptForEmailPage)
      .isAtPage()
      .inputAndConfirmCurrentPassword("password123456")
    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("auth_******@******.gov.uk")
      .submitCode(validMfaCode)
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
    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("auth_******@******.gov.uk")
      .submitCode(validMfaCode)
    changeEmailPage
      .isAtPage()
      .inputAndConfirmNewEmail("auth_email@digital.justice.gov.uk")
    emailAlreadyVerifiedPage
      .isAtPage()
      .continueToAccountDetailsPage()
    accountDetailsPage
      .isAt()
  }

  @Test
  fun `change email mfa pref primary email MFA flow`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_USER")
      .assertEmailCodeDestination("mfa_******@******.gov.uk")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("mfa_******@******.gov.uk")
      .submitCode(validMfaCode)

    changeEmailPage.isAt()
  }

  @Test
  fun `change email mfa pref text MFA flow`() {
    goTo(loginPage)
      .loginWithMfaText("AUTH_MFA_PREF_TEXT2")
      .assertMobileCodeDestination("*******0321")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")

    val validMfaCode = accountMfaTextPage.getCode()
    accountMfaTextPage
      .assertMobileCodeDestination("*******0321")
      .submitCode(validMfaCode)

    changeEmailPage.isAt()
  }

  @Test
  fun `change email mfa pref secondary email MFA flow`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_2ND_EMAIL")
      .assertEmailCodeDestination("jo******@******ith.com")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("jo******@******ith.com")
      .submitCode(validMfaCode)

    changeEmailPage.isAt()
  }

  @Test
  fun `change email mfa pref unverified text but email verified MFA flow`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_TEXT_EMAIL")
      .assertEmailCodeDestination("auth******@******.gov.uk")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("auth******@******.gov.uk")
      .submitCode(validMfaCode)

    changeEmailPage.isAt()
  }

  @Test
  fun `change email mfa pref unverified secondary email MFA enabled but email verified MFA flow`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_2ND_EMAIL_EMAIL")
      .assertEmailCodeDestination("auth_u******@******.gov.uk")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("auth_u******@******.gov.uk")
      .submitCode(validMfaCode)

    changeEmailPage.isAt()
  }

  @Test
  fun `change email no verified email or mobile MFA flow`() {
    goTo(loginPage)
      .loginError("AUTH_MFA_NOEMAIL_USER")

    loginPage.checkError(
      "We need to send you a security code to sign in, but we can't find a verified email " +
        "address or phone number. Please verify your email address by clicking the link in your email."
    )
  }

  @Test
  fun `change email - email preference gets locked after 3 invalid MFA attempts`() {
    goTo(loginPage)
      .loginAs("AUTH_MFA_LOCKED5_EMAIL")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")

    accountMfaEmailPage
      .assertEmailCodeDestination("mfa_******@******.gov.uk")
      .submitCode("123")
      .isAtError()
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")

    loginPage.checkLoginAccountLockedError()
      .loginError("AUTH_MFA_LOCKED5_EMAIL")
      .checkLoginAccountLockedError()
  }

  @Test
  fun `change email - text preference gets locked after 3 invalid MFA attempts`() {
    goTo(loginPage)
      .loginAs("AUTH_MFA_LOCKED5_TEXT")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")

    accountMfaTextPage
      .assertMobileCodeDestination("*******0321")
      .submitCode("123")
      .isAtError()
      .checkTextCodeIsIncorrectError()
      .submitCode("123")
      .checkTextCodeIsIncorrectError()
      .submitCode("123")

    loginPage.checkLoginAccountLockedError()
      .loginError("AUTH_MFA_LOCKED5_TEXT")
      .checkLoginAccountLockedError()
  }

  @Test
  fun `change email - secondary email preference gets locked after 3 invalid MFA attempts`() {
    goTo(loginPage)
      .loginAs("AUTH_MFA_LOCKED5_2ND_EMAIL")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")

    accountMfaEmailPage
      .assertEmailCodeDestination("jo******@******ith.com")
      .submitCode("123")
      .isAtError()
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")

    loginPage.checkLoginAccountLockedError()
      .loginError("AUTH_MFA_LOCKED5_2ND_EMAIL")
      .checkLoginAccountLockedError()
  }

  @Test
  fun `change email - Locked count gets reset after successful MFA completion email preference`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_EMAIL3")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("auth.******@******.gov.uk")
      .submitCode("123")
      .isAtError()
      .checkEmailCodeIsIncorrectError()
      .assertEmailCodeDestination("auth.******@******.gov.uk")
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .assertEmailCodeDestination("auth.******@******.gov.uk")
      .submitCode(validMfaCode)

    changeEmailPage.logOut()

    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_EMAIL3")
    val validMfaCode2 = mfaEmailPage.getCode()
    mfaEmailPage
      .assertEmailCodeDestination("auth.******@******.gov.uk")
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .assertEmailCodeDestination("auth.******@******.gov.uk")
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .assertEmailCodeDestination("auth.******@******.gov.uk")
      .submitCode(validMfaCode2)

    homePage.isAt()
  }

  @Test
  fun `change email - Locked count gets reset after successful MFA completion text preference`() {
    goTo(loginPage)
      .loginWithMfaText("AUTH_MFA_PREF_TEXT3")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")

    val validMfaCode = accountMfaTextPage.getCode()
    accountMfaTextPage
      .assertMobileCodeDestination("*******0321")
      .submitCode("123")
      .isAtError()
      .checkTextCodeIsIncorrectError()
      .assertMobileCodeDestination("*******0321")
      .submitCode("123")
      .checkTextCodeIsIncorrectError()
      .assertMobileCodeDestination("*******0321")
      .submitCode(validMfaCode)

    changeEmailPage.logOut()

    goTo(loginPage)
      .loginWithMfaText("AUTH_MFA_PREF_TEXT3")
    val validMfaCode2 = mfaTextPage.getCode()
    mfaTextPage
      .assertMobileCodeDestination("*******0321")
      .submitCode("123")
      .checkTextCodeIsIncorrectError()
      .submitCode("123")
      .checkTextCodeIsIncorrectError()
      .submitCode(validMfaCode2)

    homePage.isAt()
  }

  @Test
  fun `change email - Locked count gets reset after successful MFA completion 2nd email preference`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_2ND_EMAIL3")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("jo******@******ith.com")
      .submitCode("123")
      .isAtError()
      .checkEmailCodeIsIncorrectError()
      .assertEmailCodeDestination("jo******@******ith.com")
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .assertEmailCodeDestination("jo******@******ith.com")
      .submitCode(validMfaCode)

    changeEmailPage.logOut()

    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_2ND_EMAIL3")
    val validMfaCode2 = mfaEmailPage.getCode()
    mfaEmailPage
      .assertEmailCodeDestination("jo******@******ith.com")
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .assertEmailCodeDestination("jo******@******ith.com")
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .assertEmailCodeDestination("jo******@******ith.com")
      .submitCode(validMfaCode2)

    homePage.isAt()
  }

  @Test
  fun `Mfa preference email - I would like the MFA code to be resent by email`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_USER")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")

    accountMfaEmailPage
      .assertEmailCodeDestination("mfa_******@******.gov.uk")
      .resendCodeLink()

    accountMfaEmailResendCodePage.resendCodeByEmail()

    accountMfaEmailPage
      .assertEmailCodeDestination("mfa_******@******.gov.uk")
      .submitCode()

    changeEmailPage.isAt()
  }

  @Test
  fun `Mfa preference email - I would like the MFA code to be resent by text`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_EMAIL4")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")
    accountMfaEmailPage
      .assertEmailCodeDestination("auth.******@******.gov.uk")
      .resendCodeLink()

    accountMfaEmailResendCodePage.resendCodeByText()

    accountMfaTextPage
      .assertMobileCodeDestination("*******0321")
      .submitCode()

    changeEmailPage.isAt()
  }

  @Test
  fun `Mfa preference email - I would like the MFA code to be resent by secondary email`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_EMAIL5")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")
    accountMfaEmailPage
      .assertEmailCodeDestination("auth.******@******.gov.uk")
      .resendCodeLink()

    accountMfaEmailResendCodePage.resendCodeBySecondaryEmail()

    accountMfaEmailPage
      .assertEmailCodeDestination("jo******@******ith.com")
      .submitCode()

    changeEmailPage.isAt()
  }

  @Test
  fun `MFA preference text - I would like the MFA code to be resent by email`() {
    goTo(loginPage)
      .loginWithMfaText("AUTH_MFA_PREF_TEXT2")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")
    accountMfaTextPage
      .assertMobileCodeDestination("*******0321")
      .resendCodeLink()

    accountMfaTextResendCodePage.resendCodeByEmail()

    accountMfaEmailPage
      .assertEmailCodeDestination("auth******@******.gov.uk")
      .submitCode()

    changeEmailPage.isAt()
  }

  @Test
  fun `MFA preference text - I would like the MFA code to be resent by text`() {
    goTo(loginPage)
      .loginWithMfaText("AUTH_MFA_PREF_TEXT2")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")

    accountMfaTextPage
      .assertMobileCodeDestination("*******0321")
      .resendCodeLink()

    accountMfaTextResendCodePage.resendCodeByText()

    accountMfaTextPage
      .assertMobileCodeDestination("*******0321")
      .submitCode()

    changeEmailPage.isAt()
  }

  @Test
  fun `Mfa preference secondary email - I would like the MFA code to be resent by email`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_2ND_EMAIL2")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")
    accountMfaEmailPage
      .assertEmailCodeDestination("jo******@******ith.com")
      .resendCodeLink()

    accountMfaEmailResendCodePage.resendCodeByEmail()

    accountMfaEmailPage
      .assertEmailCodeDestination("auth_u******@******.gov.uk")
      .submitCode()

    changeEmailPage.isAt()
  }

  @Test
  fun `Mfa preference secondary email - I would like the MFA code to be resent by text`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_2ND_EMAIL2")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    passwordPromptForEmailPage.inputAndConfirmCurrentPassword("password123456")
    accountMfaEmailPage
      .assertEmailCodeDestination("jo******@******ith.com")
      .resendCodeLink()

    accountMfaEmailResendCodePage.resendCodeByText()

    accountMfaTextPage
      .assertMobileCodeDestination("*******0321")
      .submitCode()

    changeEmailPage.isAt()
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
    assertThat(continueButton.text()).isEqualTo("OK, continue")
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
