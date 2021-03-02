package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class ChangeMobileSpecification : AbstractAuthSpecification() {

  @Page
  private lateinit var homePage: HomePage

  @Page
  private lateinit var addMobilePage: AddMobilePage

  @Page
  private lateinit var changeMobilePage: ChangeMobilePage

  @Page
  private lateinit var verifyMobileSentPage: VerifyMobileSentPage

  @Page
  private lateinit var verifyMobileConfirmPage: VerifyMobileConfirmPage

  @Page
  private lateinit var verifyMobileAlreadyPage: VerifyMobileAlreadyPage

  @Page
  private lateinit var accountMfaEmailPage: AccountMfaEmailPage

  @Page
  private lateinit var accountMfaTextPage: AccountMfaTextPage

  @Page
  private lateinit var accountMfaEmailResendCodePage: AccountMfaEmailResendCodePage

  @Page
  private lateinit var accountMfaTextResendCodePage: AccountMfaTextResendCodePage

  @Page
  private lateinit var accountDetailsPage: AccountDetailsPage

  @Page
  private lateinit var mfaEmailPage: MfaEmailPage

  @Page
  private lateinit var mfaTextPage: MfaTextPage

  @Test
  fun `Change mobile flow`() {
    goTo(loginPage).loginAs("AUTH_CHANGE_MOBILE")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage.submitCode(validMfaCode)

    changeMobilePage
      .addMobileAs("07987654321")

    verifyMobileSentPage.isAtPage()
      .submitCode()

    verifyMobileConfirmPage.isAtPage()
      .continueToAccountDetailsPage()

    accountDetailsPage.isAtPage()
  }

  @Test
  fun `Change mobile mfa code not entered continue displays error`() {
    goTo(loginPage).loginAs("AUTH_CHANGE_MOBILE")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage.submitWithoutCode()
      .isAtError()
      .enterTheCodeError()

    accountMfaEmailPage.submitCode(validMfaCode)

    changeMobilePage
      .isAtPage()
  }

  @Test
  fun `Change mobile mfa code not entered, page displays correctly when continue and go back`() {
    goTo(loginPage).loginAs("AUTH_CHANGE_MOBILE")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage.submitWithoutCode()
      .enterTheCodeError()
      .submitWithoutCode()
      .enterTheCodeError()
      .submitWithoutCode()
      .enterTheCodeError()

    driver.navigate().back()
    driver.navigate().back()
    driver.navigate().back()

    // accountMfaEmailPage.enterTheCodeError()
    //   .submitCode(validMfaCode)
    //
    // changeMobilePage.isAtPage()
  }

  @Test
  fun `Change mobile invalid number entered`() {
    goTo(loginPage).loginAs("AUTH_CHANGE_MOBILE2")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage.submitCode(validMfaCode)

    addMobilePage
      .setMobileAs("07")
      .checkError("Enter a mobile number in the correct format")
      .updateMobileAs("", "07")
      .checkError("Enter a mobile number")
  }

  @Test
  fun `Change mobile flow invalid verification code`() {
    goTo(loginPage).loginAs("AUTH_CHANGE_MOBILE")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage.submitCode(validMfaCode)

    changeMobilePage.setMobileAs("07700900322")

    verifyMobileSentPage.isAtPage()
      .submitCode("123456")
      .checkError("Enter the code received in the text message")
  }

  @Test
  fun `current verified mobile number re-entered`() {

    goTo(loginPage).loginAs("AUTH_CHANGE_MOBILE_VERIFIED")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage.submitCode(validMfaCode)

    changeMobilePage.updateMobileAs("07700900321", "07700900321")

    verifyMobileAlreadyPage.isAtPage()
      .continueToAccountDetailsPage()

    accountDetailsPage.isAt()
  }

  @Test
  fun `Change mobile flow mfa primary email`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_USER")
      .assertEmailCodeDestination("mfa_******@******.gov.uk")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("mfa_******@******.gov.uk")
      .submitCode(validMfaCode)

    addMobilePage.isAtPage()
  }

  @Test
  fun `Change mobile flow mfa text`() {
    goTo(loginPage)
      .loginWithMfaText("AUTH_MFA_PREF_TEXT2")
      .assertMobileCodeDestination("*******0321")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    val validMfaCode = accountMfaTextPage.getCode()
    accountMfaTextPage
      .assertMobileCodeDestination("*******0321")
      .submitCode(validMfaCode)

    changeMobilePage.isAtPage()
  }

  @Test
  fun `Change mobile flow mfa secondary email`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_2ND_EMAIL")
      .assertEmailCodeDestination("jo******@******ith.com")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("jo******@******ith.com")
      .submitCode(validMfaCode)

    addMobilePage.isAtPage()
  }

  @Test
  fun `Change mobile mfa pref unverified text but email verified MFA flow`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_TEXT_EMAIL")
      .assertEmailCodeDestination("auth******@******.gov.uk")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("auth******@******.gov.uk")
      .submitCode(validMfaCode)

    addMobilePage.isAtPage()
  }

  @Test
  fun `Change mobile mfa pref unverified secondary email MFA enabled but email verified MFA flow`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_2ND_EMAIL_EMAIL")
      .assertEmailCodeDestination("auth_u******@******.gov.uk")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .assertEmailCodeDestination("auth_u******@******.gov.uk")
      .submitCode(validMfaCode)

    addMobilePage.isAtPage()
  }

  @Test
  fun `Add mobile no verified email or mobile MFA flow`() {
    goTo(loginPage)
      .loginError("AUTH_MFA_NOEMAIL_USER")

    loginPage.checkError(
      "We need to send you a security code to sign in, but we can't find a verified email " +
        "address or phone number. Please verify your email address by clicking the link in your email."
    )
  }

  @Test
  fun `Add mobile - email preference gets locked after 3 invalid MFA attempts`() {
    goTo(loginPage)
      .loginAs("AUTH_MFA_LOCKED4_EMAIL")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    accountMfaEmailPage.submitCode("123")
      .isAtError()
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")

    loginPage.checkLoginAccountLockedError()
      .loginError("AUTH_MFA_LOCKED4_EMAIL")
      .checkLoginAccountLockedError()
  }

  @Test
  fun `Add mobile - text preference gets locked after 3 invalid MFA attempts`() {
    goTo(loginPage)
      .loginAs("AUTH_MFA_LOCKED4_TEXT")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    accountMfaTextPage.submitCode("123")
      .isAtError()
      .checkTextCodeIsIncorrectError()
      .submitCode("123")
      .checkTextCodeIsIncorrectError()
      .submitCode("123")

    loginPage.checkLoginAccountLockedError()
      .loginError("AUTH_MFA_LOCKED4_TEXT")
      .checkLoginAccountLockedError()
  }

  @Test
  fun `Add mobile - secondary email preference gets locked after 3 invalid MFA attempts`() {
    goTo(loginPage)
      .loginAs("AUTH_MFA_LOCKED4_2ND_EMAIL")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    accountMfaEmailPage.submitCode("123")
      .isAtError()
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")

    loginPage.checkLoginAccountLockedError()
      .loginError("AUTH_MFA_LOCKED4_2ND_EMAIL")
      .checkLoginAccountLockedError()
  }

  @Test
  fun `Add mobile - Locked count gets reset after successful MFA completion email preference`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_EMAIL3")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage.submitCode("123")
      .isAtError()
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode(validMfaCode)

    changeMobilePage.logOut()

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
  fun `Add mobile - Locked count gets reset after successful MFA completion text preference`() {
    goTo(loginPage)
      .loginWithMfaText("AUTH_MFA_PREF_TEXT3")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    val validMfaCode = accountMfaTextPage.getCode()
    accountMfaTextPage.submitCode("123")
      .isAtError()
      .checkTextCodeIsIncorrectError()
      .submitCode("123")
      .checkTextCodeIsIncorrectError()
      .submitCode(validMfaCode)

    changeMobilePage.logOut()

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
  fun `Add mobile - Locked count gets reset after successful MFA completion 2nd email preference`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_2ND_EMAIL3")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage.submitCode("123")
      .isAtError()
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode(validMfaCode)

    changeMobilePage.logOut()

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

    accountDetailsPage.navigateToChangeMobile()

    accountMfaEmailPage.resendCodeLink()

    accountMfaEmailResendCodePage.resendCodeByEmail()

    accountMfaEmailPage.submitCode()

    addMobilePage.isAt()
  }

  @Test
  fun `Mfa preference email - I would like the MFA code to be resent by text`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_EMAIL4")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()
    accountMfaEmailPage.isAtPage().resendCodeLink()
    accountMfaEmailResendCodePage.isAtPage().resendCodeByText()
    accountMfaTextPage.isAtPage()
      .assertMobileCodeDestination("*******0321")
      .submitCode()

    changeMobilePage.isAt()
  }

  @Test
  fun `Mfa preference email - I would like the MFA code to be resent by secondary email`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_EMAIL5")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()
    accountMfaEmailPage.resendCodeLink()

    accountMfaEmailResendCodePage.resendCodeBySecondaryEmail()

    accountMfaEmailPage.submitCode()

    changeMobilePage.isAt()
  }

  @Test
  fun `MFA preference text - I would like the MFA code to be resent by email`() {
    goTo(loginPage)
      .loginWithMfaText("AUTH_MFA_PREF_TEXT2")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()
    accountMfaTextPage.resendCodeLink()

    accountMfaTextResendCodePage.resendCodeByEmail()

    accountMfaTextPage.submitCode()

    changeMobilePage.isAt()
  }

  @Test
  fun `MFA preference text - I would like the MFA code to be resent by text`() {
    goTo(loginPage)
      .loginWithMfaText("AUTH_MFA_PREF_TEXT2")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    accountMfaTextPage.resendCodeLink()

    accountMfaTextResendCodePage.resendCodeByText()

    accountMfaTextPage.submitCode()

    changeMobilePage.isAt()
  }

  @Test
  fun `Mfa preference secondary email - I would like the MFA code to be resent by email`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_2ND_EMAIL2")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()
    accountMfaEmailPage.resendCodeLink()

    accountMfaEmailResendCodePage.resendCodeByText()

    accountMfaTextPage.submitCode()

    changeMobilePage.isAt()
  }

  @Test
  fun `Mfa preference secondary email - I would like the MFA code to be resent by text`() {
    goTo(loginPage)
      .loginWithMfaEmail("AUTH_MFA_PREF_2ND_EMAIL2")
      .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()
    accountMfaEmailPage.resendCodeLink()

    accountMfaEmailResendCodePage.resendCodeByText()

    accountMfaTextPage.submitCode()

    changeMobilePage.isAt()
  }
}

@PageUrl("/new-mobile")
open class ChangeMobilePage :
  AuthPage<ChangeMobilePage>("HMPPS Digital Services - Change Mobile Number", "What is your new mobile number?") {
  @FindBy(css = "input[type='submit']")
  private lateinit var changeMobileButton: FluentWebElement
  private lateinit var mobile: FluentWebElement

  fun updateMobileAs(mobile: String, existingMobile: String): ChangeMobilePage {
    assertThat(this.mobile.value()).isEqualTo(existingMobile)
    this.mobile.fill().withText(mobile)
    assertThat(changeMobileButton.value()).isEqualTo("Continue")
    changeMobileButton.click()
    return this
  }

  fun addMobileAs(mobile: String) {
    this.mobile.fill().withText(mobile)
    assertThat(changeMobileButton.value()).isEqualTo("Continue")
    changeMobileButton.click()
  }

  fun setMobileAs(mobile: String): ChangeMobilePage {
    this.mobile.fill().withText(mobile)
    changeMobileButton.click()
    return this
  }
}

@PageUrl("/new-mobile")
open class AddMobilePage :
  AuthPage<AddMobilePage>("HMPPS Digital Services - Change Mobile Number", "What is your mobile number?") {
  @FindBy(css = "input[type='submit']")
  private lateinit var changeMobileButton: FluentWebElement
  private lateinit var mobile: FluentWebElement

  fun updateMobileAs(mobile: String, existingMobile: String): AddMobilePage {
    assertThat(this.mobile.value()).isEqualTo(existingMobile)
    this.mobile.fill().withText(mobile)
    assertThat(changeMobileButton.value()).isEqualTo("Continue")
    changeMobileButton.click()
    return this
  }

  fun setMobileAs(mobile: String): AddMobilePage {
    this.mobile.fill().withText(mobile)
    changeMobileButton.click()
    return this
  }
}

@PageUrl("/verify-mobile")
open class VerifyMobileSentPage :
  AuthPage<VerifyMobileSentPage>("HMPPS Digital Services - Verify Mobile Code Sent", "Check your phone") {
  @FindBy(css = "input[type='submit']")
  private lateinit var continueButton: FluentWebElement
  private lateinit var code: FluentWebElement

  fun submitCode(verifyCode: String? = null): VerifyMobileSentPage {
    this.code.fill().withText(verifyCode ?: el("[data-qa='verifyCode']").text())
    assertThat(continueButton.value()).isEqualTo("Continue")
    continueButton.click()
    return this
  }

  fun resendMobileCode() {
    val resendLink = el("a[id='resend-mobile-code']")
    assertThat(resendLink.text()).isEqualTo("you can request a new code to be sent")
    resendLink.click()
  }
}

@PageUrl("/verify-mobile")
open class VerifyMobileConfirmPage : AuthPage<VerifyMobileConfirmPage>(
  "HMPPS Digital Services - Verify Mobile Confirmation",
  "Your mobile number has been verified"
) {
  @FindBy(css = "#continue")
  private lateinit var continueButton: FluentWebElement

  fun continueToAccountDetailsPage() {
    assertThat(continueButton.text()).isEqualTo("Continue")
    continueButton.click()
  }
}

@PageUrl("/verify-mobile-already")
open class VerifyMobileAlreadyPage :
  AuthPage<VerifyMobileAlreadyPage>("HMPPS Digital Services - Verify Mobile Confirmation", "Mobile already verified") {
  @FindBy(css = "#continue")
  private lateinit var continueButton: FluentWebElement

  fun continueToAccountDetailsPage() {
    assertThat(continueButton.text()).isEqualTo("OK, continue")
    continueButton.click()
  }
}
