package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class AddChangeSecondaryEmailSpecification : AbstractDeliusAuthSpecification() {

  @Page
  private lateinit var accountDetailsPage: AccountDetailsPage

  @Page
  private lateinit var homePage: HomePage

  @Page
  private lateinit var newSecondaryEmailPage: NewSecondaryEmailPage

  @Page
  private lateinit var changeSecondaryEmailPage: ChangeSecondaryEmailPage

  @Page
  private lateinit var verifyEmailSentPage: VerifyEmailSentPage

  @Page
  private lateinit var verifySecondaryEmailConfirmPage: VerifySecondaryEmailConfirmPage

  @Page
  private lateinit var secondaryEmailAlreadyVerifiedPage: SecondaryEmailAlreadyVerifiedPage

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
  fun `Add Secondary email mfa pref primary email flow but not verify`() {
    goTo(loginPage)
      .loginAs("AUTH_SECOND_EMAIL_ADD")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .submitCode(validMfaCode)

    newSecondaryEmailPage.addSecondaryEmailAs("bob@gmail.com")

    goTo(accountDetailsPage)
      .isAtPage()
      .checkSecondaryEmailAndIsNotVerified()
  }

  @Test
  fun `Add Secondary email mfa pref primary text flow and verify`() {
    goTo(loginPage)
      .loginAs("AUTH_SECOND_EMAIL_UPDATE")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .submitCode(validMfaCode)

    changeSecondaryEmailPage.updateSecondaryEmailAs("bob@gmail.com")

    val verifyLink = verifyEmailSentPage.getVerifyLink()

    verifyEmailSentPage.continueProcess()

    homePage.navigateToAccountDetails()

    accountDetailsPage
      .isAtPage()
      .checkSecondaryEmailAndIsNotVerified()

    goTo(verifyLink)

    verifySecondaryEmailConfirmPage.isAt()

    goTo(accountDetailsPage)
      .isAtPage()
      .checkSecondaryEmailAndIsVerified()
  }

  @Test
  fun `Add Secondary email flow and verify`() {
    goTo(loginPage)
      .loginAs("AUTH_SECOND_EMAIL_UPDATE")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .submitCode(validMfaCode)

    changeSecondaryEmailPage.updateSecondaryEmailAs("bob1@gmail.com")

    val verifyLink = verifyEmailSentPage.getVerifyLink()

    verifyEmailSentPage.continueProcess()

    homePage.navigateToAccountDetails()

    accountDetailsPage
      .isAtPage()
      .checkSecondaryEmailAndIsNotVerified("bob1@gmail.com")

    goTo(verifyLink)

    verifySecondaryEmailConfirmPage.isAt()

    goTo(accountDetailsPage)
      .isAtPage()
      .checkSecondaryEmailAndIsVerified("bob1@gmail.com")
  }

  @Test
  fun `As  delius user add Secondary email flow and verify`() {
    goTo(loginPage)
      .loginAs("DELIUS_SECOND_EMAIL_UPDATE")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .submitCode(validMfaCode)

    changeSecondaryEmailPage.updateSecondaryEmailAs("bob@gmail.com")

    val verifyLink = verifyEmailSentPage.getVerifyLink()

    verifyEmailSentPage.continueProcess()

    homePage.navigateToAccountDetails()

    accountDetailsPage
      .isAtPage()
      .checkSecondaryEmailAndIsNotVerified()

    goTo(verifyLink)

    verifySecondaryEmailConfirmPage.isAt()

    goTo(accountDetailsPage)
      .isAtPage()
      .checkSecondaryEmailAndIsVerified()
  }

  @Test
  fun `A user is not allowed to add a secondary email address which is a gsi email address`() {
    goTo(loginPage)
      .loginAs("AUTH_SECOND_EMAIL_UPDATE")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage.submitCode(validMfaCode)

    changeSecondaryEmailPage
      .updateSecondaryEmailAs("bob@justice.gsi.gov.uk")

    changeSecondaryEmailPage.checkError("All gsi.gov.uk have now been migrated to a justice.gov.uk domain. Enter your justice.gov.uk address instead.")
  }

  @Test
  fun `A user is not allowed to add an invalid secondary email address`() {
    goTo(loginPage)
      .loginAs("AUTH_SECOND_EMAIL_UPDATE")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .submitCode(validMfaCode)

    changeSecondaryEmailPage
      .updateSecondaryEmailAs("bob@justice")

    changeSecondaryEmailPage.checkError("Enter an email address in the correct format")
  }

  @Test
  fun `Change secondary email flow current verified email re-entered`() {
    goTo(loginPage)
      .loginAs("AUTH_SECOND_EMAIL_ALREADY")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage.submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode(validMfaCode)

    changeSecondaryEmailPage.updateSecondaryEmailAs("john@smith.com")

    secondaryEmailAlreadyVerifiedPage.isAt()
  }

  @Test
  fun `Add Secondary email mfa pref primary email MFA flow`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_USER")
      .assertEmailCodeDestination("mfa_******@******.gov.uk")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("mfa_******@******.gov.uk")
      .submitCode(validMfaCode)

    newSecondaryEmailPage.isAt()
  }

  @Test
  fun `Add Secondary email mfa pref text MFA flow`() {
    goTo(loginPage)
      .loginWithMfaText("AUTH_MFA_PREF_TEXT2")
      .assertMobileCodeDestination("*******0321")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    val validMfaCode = accountMfaTextPage.getCode()
    accountMfaTextPage
      .assertMobileCodeDestination("*******0321")
      .submitCode(validMfaCode)

    newSecondaryEmailPage.isAt()
  }

  @Test
  fun `Add Secondary email mfa pref secondary email MFA flow`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_2ND_EMAIL")
      .assertEmailCodeDestination("jo******@******ith.com")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("jo******@******ith.com")
      .submitCode(validMfaCode)

    changeSecondaryEmailPage.isAt()
  }

  @Test
  fun `Add Secondary email mfa pref unverified text but email verified MFA flow`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_TEXT_EMAIL")
      .assertEmailCodeDestination("auth******@******.gov.uk")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("auth******@******.gov.uk")
      .submitCode(validMfaCode)

    newSecondaryEmailPage.isAt()
  }

  @Test
  fun `Add Secondary email mfa pref unverified secondary email MFA enabled but email verified MFA flow`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_2ND_EMAIL_EMAIL")
      .assertEmailCodeDestination("auth_u******@******.gov.uk")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("auth_u******@******.gov.uk")
      .submitCode(validMfaCode)

    newSecondaryEmailPage.isAt()
  }

  @Test
  fun `Add Secondary email no verified email or mobile MFA flow`() {
    goTo(loginPage)
      .loginError("AUTH_MFA_NOEMAIL_USER")

    loginPage.checkError(
      "We need to send you a security code to sign in, but we can't find a verified email " +
        "address or phone number. Please verify your email address by clicking the link in your email."
    )
  }

  @Test
  fun `Add Secondary email - email preference gets locked after 3 invalid MFA attempts`() {
    goTo(loginPage)
      .loginAs("AUTH_MFA_LOCKED3_EMAIL")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    accountMfaEmailPage
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")

    loginPage.checkLoginAccountLockedError()
      .loginError("AUTH_MFA_LOCKED3_EMAIL")
      .checkLoginAccountLockedError()
  }

  @Test
  fun `Add Secondary email - text preference gets locked after 3 invalid MFA attempts`() {
    goTo(loginPage)
      .loginAs("AUTH_MFA_LOCKED3_TEXT")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    accountMfaTextPage
      .submitCode("123")
      .checkTextCodeIsIncorrectError()
      .submitCode("123")
      .checkTextCodeIsIncorrectError()
      .submitCode("123")

    loginPage.checkLoginAccountLockedError()
      .loginError("AUTH_MFA_LOCKED3_TEXT")
      .checkLoginAccountLockedError()
  }

  @Test
  fun `Add Secondary email - secondary email preference gets locked after 3 invalid MFA attempts`() {
    goTo(loginPage)
      .loginAs("AUTH_MFA_LOCKED3_2ND_EMAIL")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    accountMfaEmailPage
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")

    loginPage.checkLoginAccountLockedError()
      .loginError("AUTH_MFA_LOCKED3_2ND_EMAIL")
      .checkLoginAccountLockedError()
  }

  @Test
  fun `Add Secondary email - Locked count gets reset after successful MFA completion email preference`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_EMAIL3")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode(validMfaCode)

    changeSecondaryEmailPage.logOut()

    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_EMAIL3")
    val validMfaCode2 = mfaEmailPage.getCode()
    mfaEmailPage.submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode(validMfaCode2)

    homePage.isAt()
  }

  @Test
  fun `Add Secondary email - Locked count gets reset after successful MFA completion text preference`() {
    goTo(loginPage)
      .loginWithMfaText("AUTH_MFA_PREF_TEXT3")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    val validMfaCode = accountMfaTextPage.getCode()
    accountMfaTextPage
      .submitCode("123")
      .checkTextCodeIsIncorrectError()
      .submitCode("123")
      .checkTextCodeIsIncorrectError()
      .submitCode(validMfaCode)

    changeSecondaryEmailPage.logOut()

    goTo(loginPage)
      .loginWithMfaText("AUTH_MFA_PREF_TEXT3")
    val validMfaCode2 = mfaTextPage.getCode()
    mfaTextPage.submitCode("123")
      .checkTextCodeIsIncorrectError()
      .submitCode("123")
      .checkTextCodeIsIncorrectError()
      .submitCode(validMfaCode2)

    homePage.isAt()
  }

  @Test
  fun `Add Secondary email - Locked count gets reset after successful MFA completion 2nd email preference`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_2ND_EMAIL3")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode(validMfaCode)

    changeSecondaryEmailPage.logOut()

    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_2ND_EMAIL3")
    val validMfaCode2 = mfaEmailPage.getCode()
    mfaEmailPage.submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode(validMfaCode2)

    homePage.isAt()
  }

  @Test
  fun `Mfa preference email - I would like the MFA code to be resent by email`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_USER")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    accountMfaEmailPage.resendCodeLink()

    accountMfaEmailResendCodePage.resendCodeByEmail()

    accountMfaEmailPage.submitCode()

    newSecondaryEmailPage.isAt()
  }

  @Test
  fun `Mfa preference email - I would like the MFA code to be resent by text`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_EMAIL4")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()
    accountMfaEmailPage.resendCodeLink()

    accountMfaEmailResendCodePage.resendCodeByText()

    accountMfaEmailPage.submitCode()

    newSecondaryEmailPage.isAt()
  }

  @Test
  fun `Mfa preference email - I would like the MFA code to be resent by secondary email`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_EMAIL5")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()
    accountMfaEmailPage.resendCodeLink()

    accountMfaEmailResendCodePage.resendCodeBySecondaryEmail()

    accountMfaEmailPage.submitCode()

    changeSecondaryEmailPage.isAt()
  }

  @Test
  fun `MFA preference text - I would like the MFA code to be resent by email`() {
    goTo(loginPage)
      .loginWithMfaText("AUTH_MFA_PREF_TEXT2")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()
    accountMfaTextPage.resendCodeLink()

    accountMfaTextResendCodePage.resendCodeByEmail()

    accountMfaTextPage.submitCode()

    newSecondaryEmailPage.isAt()
  }

  @Test
  fun `MFA preference text - I would like the MFA code to be resent by text`() {
    goTo(loginPage)
      .loginWithMfaText("AUTH_MFA_PREF_TEXT2")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    accountMfaTextPage.resendCodeLink()

    accountMfaTextResendCodePage.resendCodeByText()

    accountMfaTextPage.submitCode()

    newSecondaryEmailPage.isAt()
  }

  @Test
  fun `Mfa preference secondary email - I would like the MFA code to be resent by email`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_2ND_EMAIL2")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()
    accountMfaEmailPage.resendCodeLink()

    accountMfaEmailResendCodePage.resendCodeByText()

    accountMfaTextPage.submitCode()

    changeSecondaryEmailPage.isAt()
  }

  @Test
  fun `Mfa preference secondary email - I would like the MFA code to be resent by text`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_2ND_EMAIL2")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()
    accountMfaEmailPage.resendCodeLink()

    accountMfaEmailResendCodePage.resendCodeByText()

    accountMfaTextPage.submitCode()

    changeSecondaryEmailPage.isAt()
  }
}

@PageUrl("/new-backup-email")
open class ChangeSecondaryEmailPage : AuthPage<ChangeSecondaryEmailPage>(
  "HMPPS Digital Services - Change Backup Email",
  "What is your new backup email address?"
) {
  @FindBy(css = "input[type='submit']")
  private lateinit var changeSecondaryEmailButton: FluentWebElement
  private lateinit var email: FluentWebElement

  fun updateSecondaryEmailAs(email: String) {
    this.email.fill().withText(email)
    assertThat(changeSecondaryEmailButton.value()).isEqualTo("Continue")
    changeSecondaryEmailButton.click()
  }
}

@PageUrl("/new-backup-email")
open class NewSecondaryEmailPage : AuthPage<NewSecondaryEmailPage>(
  "HMPPS Digital Services - Change Backup Email",
  "What is your backup email address?"
) {
  @FindBy(css = "input[type='submit']")
  private lateinit var changeSecondaryEmailButton: FluentWebElement
  private lateinit var email: FluentWebElement

  fun addSecondaryEmailAs(email: String) {
    this.email.fill().withText(email)
    assertThat(changeSecondaryEmailButton.value()).isEqualTo("Continue")
    changeSecondaryEmailButton.click()
  }
}
