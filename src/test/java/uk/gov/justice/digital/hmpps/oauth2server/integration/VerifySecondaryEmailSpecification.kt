package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class VerifySecondaryEmailSpecification : AbstractAuthSpecification() {

  @Page
  private lateinit var accountDetailsPage: AccountDetailsPage

  @Page
  private lateinit var homePage: HomePage

  @Page
  private lateinit var verifySecondaryEmailSentPage: VerifySecondaryEmailSentPage

  @Page
  private lateinit var secondaryEmailVerificationResendPage: SecondaryEmailVerificationResendPage

  @Page
  private lateinit var secondaryEmailAlreadyVerifiedPage: SecondaryEmailAlreadyVerifiedPage

  @Page
  private lateinit var verifySecondaryEmailConfirmPage: VerifySecondaryEmailConfirmPage

  @Test
  fun `A user can resend token to verify secondary email address`() {
    goTo(loginPage)
        .loginAs("AUTH_SECOND_EMAIL_VERIFY")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToResendVerifySecondaryEmail()

    secondaryEmailVerificationResendPage.resendCode()

    val verifyLink = verifySecondaryEmailSentPage.getVerifyLink()

    verifySecondaryEmailSentPage
        .isAtPage()
        .continueProcess()

    goTo(verifyLink)

    verifySecondaryEmailConfirmPage.isAt()

    goTo(accountDetailsPage)
        .isAtPage()
        .checkSecondaryEmailAndIsVerified()
  }

  @Test
  fun `Resend code page with verified secondary email is redirected to secondary email already verified`() {
    goTo(loginPage)
        .loginAs("AUTH_SECOND_EMAIL_ALREADY")

    goTo(secondaryEmailVerificationResendPage)

    secondaryEmailAlreadyVerifiedPage.isAtPage()
        .continueToAccountDetailsPage()

    homePage.isAt()
  }

  @Test
  fun `Secondary email can be verified form email link when user is not logged in`() {
    goTo(loginPage)
        .loginAs("AUTH_SECOND_EMAIL_VERIFY2")

    homePage.navigateToAccountDetails()

    accountDetailsPage
        .checkSecondaryEmailAndIsNotVerified()
        .navigateToResendVerifySecondaryEmail()

    secondaryEmailVerificationResendPage.resendCode()

    val verifyLink = verifySecondaryEmailSentPage.getVerifyLink()

    verifySecondaryEmailSentPage.logOut()

    goTo(verifyLink)

    verifySecondaryEmailConfirmPage.isAt()

    goTo(loginPage)
        .loginAs("AUTH_SECOND_EMAIL_VERIFY2")

    homePage.navigateToAccountDetails()

    accountDetailsPage
        .isAtPage()
        .checkSecondaryEmailAndIsVerified()
  }
}

  @PageUrl("/verify-email-sent")
  open class VerifySecondaryEmailSentPage : AuthPage<VerifySecondaryEmailSentPage>("HMPPS Digital Services - Verify Email Sent", "Verification email sent") {
    @FindBy(css = "a[role='button']")
    private lateinit var continueButton: FluentWebElement

    @FindBy(css = "#logout")
    private lateinit var logOut: FluentWebElement

    fun continueProcess() {
      assertThat(continueButton.text()).isEqualTo("Continue")
      continueButton.click()
    }

    fun logOut() {
      logOut.click()
    }

    fun getVerifyLink(): String = el("#verifyLink").attribute("href")
  }

  @PageUrl("/secondary-email-resend")
  class SecondaryEmailVerificationResendPage : AuthPage<SecondaryEmailVerificationResendPage>("HMPPS Digital Services - Resend Verification Code", "Resend security code") {
    @FindBy(css = "input[type='submit']")
    private lateinit var continueButton: FluentWebElement

    fun resendCode() {
      assertThat(continueButton.value()).isEqualTo("Resend security code")
      continueButton.click()
    }
  }

  @PageUrl("/verify-email-secondary-confirm")
  open class VerifySecondaryEmailConfirmPage : AuthPage<VerifySecondaryEmailConfirmPage>("HMPPS Digital Services - Verify Email Confirmation", "Email address verified")

