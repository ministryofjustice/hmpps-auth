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
  private lateinit var secondaryEmailVerificationResendPage: SecondaryEmailVerificationResendPage

  @Page
  private lateinit var verifySecondaryEmailConfirmPage: VerifySecondaryEmailConfirmPage

  @Test
  fun `A user can resend token to verify secondary email address`() {
    goTo(loginPage)
        .loginAs("AUTH_SECOND_EMAIL_VERIFY")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToResendVerifySecondaryEmail()

    secondaryEmailVerificationResendPage.resendCode()

//    verifySecondaryEmailConfirmPage.isAt()
//
//    goTo(accountDetailsPage)
//        .isAtPage()
//        .checkSecondaryEmailAndIsVerified()
  }


}

@PageUrl("/secondary-Email-resend")
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
