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

  @Page
  private lateinit var verifyEmailInvalidTokenPage: VerifyEmailInvalidTokenPage

  @Page
  private lateinit var verifySecondaryEmailExpiredTokenPage: VerifySecondaryEmailExpiredTokenPage

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

    accountDetailsPage.isAt()
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

  @Test
  fun `A user is asked to sign in again if the verification link is invalid`() {
    goTo("/verify-email-secondary-confirm?token=someinvalidtoken")

    verifyEmailInvalidTokenPage.isAt()
  }

  @Test
  fun `A user is sent a new link when they use expired verify secondary email link`() {
    goTo("/verify-email-secondary-confirm?token=expired2")

    verifySecondaryEmailExpiredTokenPage.isAt()

    val verifyLink = verifySecondaryEmailExpiredTokenPage.getVerifyLink()

    goTo(verifyLink)
    verifySecondaryEmailConfirmPage.isAt()
  }
}

@PageUrl("/verify-email-sent")
open class VerifySecondaryEmailSentPage : AuthPage<VerifySecondaryEmailSentPage>(
  "HMPPS Digital Services - Verify Email Sent",
  "Verify your backup email address to complete the change"
) {
  @FindBy(css = "a[role='button']")
  private lateinit var continueButton: FluentWebElement

  fun continueProcess() {
    assertThat(continueButton.text()).isEqualTo("Continue")
    continueButton.click()
  }

  fun getVerifyLink(): String = el("#verifyLink").attribute("href")
}

@PageUrl("/backup-email-resend")
class SecondaryEmailVerificationResendPage : AuthPage<SecondaryEmailVerificationResendPage>(
  "HMPPS Digital Services - Resend Verification Code",
  "Resend security code"
) {
  @FindBy(css = "input[type='submit']")
  private lateinit var continueButton: FluentWebElement

  fun resendCode() {
    assertThat(continueButton.value()).isEqualTo("Resend security code")
    continueButton.click()
  }
}

@PageUrl("/verify-email-secondary-confirm")
open class VerifySecondaryEmailConfirmPage : AuthPage<VerifySecondaryEmailConfirmPage>(
  "HMPPS Digital Services - Verify Email Confirmation",
  "Your backup email address has been verified"
)

@PageUrl("/verify-email-secondary-expired")
open class VerifySecondaryEmailExpiredTokenPage : AuthPage<VerifySecondaryEmailExpiredTokenPage>(
  "HMPPS Digital Services - Verify Email Confirmation",
  "The link has expired"
) {
  fun getVerifyLink(): String = el("#link").attribute("href")
}
