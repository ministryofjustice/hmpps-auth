package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class VerifyMobileSpecification : AbstractAuthSpecification() {
  @Page
  private lateinit var changeMobilePage: ChangeMobilePage

  @Page
  private lateinit var addMobilePage: AddMobilePage

  @Page
  private lateinit var verifyMobileSentPage: VerifyMobileSentPage

  @Page
  private lateinit var mobileVerificationResendPage: MobileVerificationResendPage

  @Page
  private lateinit var verifyMobileConfirmPage: VerifyMobileConfirmPage

  @Page
  private lateinit var verifyMobileAlreadyPage: VerifyMobileAlreadyPage

  @Page
  private lateinit var accountDetailsPage: AccountDetailsPage

  @Page
  private lateinit var accountMfaEmailPage: AccountMfaEmailPage

  @Test
  fun `Change mobile flow resend security code`() {
    goTo(loginPage).loginAs("AUTH_CHANGE_MOBILE_ADD")

    goTo(accountDetailsPage).navigateToChangeMobile()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .submitCode(validMfaCode)

    changeMobilePage
      .addMobileAs("07700900321")

    verifyMobileSentPage.isAtPage()
      .resendMobileCode()

    mobileVerificationResendPage.isAtPage()
      .resendCode()

    verifyMobileSentPage.isAtPage()
      .submitCode()

    verifyMobileConfirmPage.isAtPage()
      .continueToAccountDetailsPage()

    accountDetailsPage.isAt()
  }

  @Test
  fun `Change mobile flow invalid verification code`() {
    goTo(loginPage).loginAs("AUTH_CHANGE_MOBILE_UPDATE")

    goTo(accountDetailsPage).navigateToChangeMobile()

    val validMfaCode = accountMfaEmailPage.getCode()
    accountMfaEmailPage
      .submitCode(validMfaCode)

    changeMobilePage
      .updateMobileAs("07700900322", "07700900321")

    verifyMobileSentPage.isAtPage()
      .submitCode("123456")

    verifyMobileSentPage.checkError("Enter the code received in the text message")
  }

  @Test
  fun `Resend code page without phone number is redirected to enter new phone number page`() {
    goTo(loginPage).loginAs("AUTH_CHANGE_MOBILE3")

    goTo(mobileVerificationResendPage)
      .resendCode()

    accountDetailsPage.checkError("No phone number found")
  }

  @Test
  fun `Resend code page with verified phone number is redirected to phone number already verified`() {
    goTo(loginPage).loginAs("AUTH_CHANGE_MOBILE_VERIFIED")

    goTo(mobileVerificationResendPage)

    verifyMobileAlreadyPage.isAtPage()
      .continueToAccountDetailsPage()

    accountDetailsPage.isAt()
  }
}

@PageUrl("/mobile-resend")
class MobileVerificationResendPage : AuthPage<MobileVerificationResendPage>(
  "HMPPS Digital Services - Resend Security Code",
  "Send another text message"
) {
  @FindBy(css = "input[type='submit']")
  private lateinit var continueButton: FluentWebElement

  fun resendCode() {
    assertThat(continueButton.value()).isEqualTo("Send text")
    continueButton.click()
  }
}
