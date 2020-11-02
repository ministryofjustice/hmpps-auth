package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class VerifyEmailSpecification : AbstractAuthSpecification() {
  @Page
  private lateinit var verifyEmailPage: VerifyEmailPage

  @Page
  private lateinit var verifyEmailSentPage: VerifyEmailSentPage

  @Page
  private lateinit var verifyEmailConfirmPage: VerifyEmailConfirmPage

  @Page
  private lateinit var verifyEmailInvalidTokenPage: VerifyEmailInvalidTokenPage

  @Page
  private lateinit var verifyEmailExpiredTokenPage: VerifyEmailExpiredTokenPage

  @Test
  fun `A user can cancel email verification`() {
    goTo(loginPage).loginAsWithUnverifiedEmail("DM_USER")
      .cancel()

    newInstance(HomePage::class.java).isAt()
  }

  @Test
  fun `A user is not allowed to verify a gsi email address`() {
    goTo(loginPage).loginAsWithUnverifiedEmail("AUTH_NO_EMAIL")
      .verifyEmailAs("dm_user@hmps.gsi.gov.uk")

    verifyEmailPage.checkError("All gsi.gov.uk have now been migrated to a justice.gov.uk domain. Enter your justice.gov.uk address instead.")
  }

  @Test
  fun `A user can verify a previously chosen email`() {
    goTo(loginPage).loginAsWithUnverifiedEmail("DM_USER")
      .verifyExistingEmailAs("dm_user@digital.justice.gov.uk")

    verifyEmailSentPage.isAt()
    val verifyLink = verifyEmailSentPage.getVerifyLink()
    verifyEmailSentPage.continueProcess()

    newInstance(HomePage::class.java).isAt()

    goTo(verifyLink)
    verifyEmailConfirmPage.isAt()

    goTo(verifyLink)
    verifyEmailConfirmPage.isAt()
  }

  @Test
  fun `A user can verify an email that exists in pnomis`() {
    goTo(loginPage).loginAsWithUnverifiedEmail("RO_USER")
      .selectExistingEmailAs("phillips@bobjustice.gov.uk")

    verifyEmailSentPage.isAt()
    val verifyLink = verifyEmailSentPage.getVerifyLink()
    verifyEmailSentPage.continueProcess()

    newInstance(HomePage::class.java).isAt()

    goTo(verifyLink)
    verifyEmailConfirmPage.isAt()
  }

  @Test
  fun `A user can verify an email that exists in pnomis where the user has changed password in auth`() {
    goTo(loginPage).loginAsWithUnverifiedEmail("RO_DEMO")
      .selectExistingEmailAs("ro_user@some.justice.gov.uk")

    verifyEmailSentPage.isAt()
    val verifyLink = verifyEmailSentPage.getVerifyLink()
    verifyEmailSentPage.continueProcess()

    newInstance(HomePage::class.java).isAt()

    goTo(verifyLink)
    verifyEmailConfirmPage.isAt()
  }

  @Test
  fun `An auth only user can verify their email address`() {
    goTo(loginPage).loginAsWithUnverifiedEmail("AUTH_NO_EMAIL")
      .verifyEmailAs("auth_no_email@digital.justice.gov.uk")

    verifyEmailSentPage.isAt()
    val verifyLink = verifyEmailSentPage.getVerifyLink()
    verifyEmailSentPage.continueProcess()

    newInstance(HomePage::class.java).isAt()

    goTo(verifyLink)
    verifyEmailConfirmPage.isAt()
  }

  @Test
  fun `A user is asked to sign in again if the verification link is invalid`() {
    goTo("/verify-email-confirm?token=someinvalidtoken")

    verifyEmailInvalidTokenPage.isAt()
  }

  @Test
  fun `A user is sent a new link when they use expired verify email link`() {
    goTo("/verify-email-confirm?token=expired1")

    verifyEmailExpiredTokenPage.isAt()

    val verifyLink = verifyEmailExpiredTokenPage.getVerifyLink()

    goTo(verifyLink)
    verifyEmailConfirmPage.isAt()
  }
}

@PageUrl("/verify-email")
open class VerifyEmailPage :
  AuthPage<VerifyEmailPage>("HMPPS Digital Services - Verify Email", "Verify your email address") {
  @FindBy(css = "input[type='submit']")
  private lateinit var verifyEmailButton: FluentWebElement

  @FindBy(css = "#email")
  private lateinit var emailInput: FluentWebElement

  fun verifyEmailAs(email: String) {
    emailInput.fill().withText(email)
    assertThat(verifyEmailButton.value()).isEqualTo("Save")
    verifyEmailButton.click()
  }

  fun verifyExistingEmailAs(email: String) {
    assertThat(emailInput.value()).isEqualTo(email)
    assertThat(verifyEmailButton.value()).isEqualTo("Resend email")
    verifyEmailButton.click()
  }

  fun selectExistingEmailAs(email: String) {
    el("input[value='$email']").click()
    assertThat(verifyEmailButton.value()).isEqualTo("Save")
    verifyEmailButton.click()
  }

  fun cancel() {
    val cancelButton = el("a[id='cancel']")
    assertThat(cancelButton.text()).isEqualTo("Skip for now")
    cancelButton.click()
  }
}

@PageUrl("/verify-email")
open class VerifyEmailSentPage : AuthPage<VerifyEmailSentPage>(
  "HMPPS Digital Services - Verify Email Sent",
  "Verify your email address to complete the change"
) {
  @FindBy(css = "a[role='button']")
  private lateinit var continueButton: FluentWebElement

  fun continueProcess() {
    assertThat(continueButton.text()).isEqualTo("Continue")
    continueButton.click()
  }

  fun getVerifyLink(): String = el("#verifyLink").attribute("href")
}

@PageUrl("/verify-email-confirm")
open class VerifyEmailConfirmPage : AuthPage<VerifyEmailConfirmPage>(
  "HMPPS Digital Services - Verify Email Confirmation",
  "Your email address has been verified"
)

@PageUrl("/verify-email-already")
open class EmailAlreadyVerifiedPage : AuthPage<EmailAlreadyVerifiedPage>(
  "HMPPS Digital Services - Verify Email Confirmation",
  "Email already verified"
) {
  @FindBy(css = "#continue")
  private lateinit var continueButton: FluentWebElement

  fun continueToAccountDetailsPage() {
    assertThat(continueButton.text()).isEqualTo("OK, continue")
    continueButton.click()
  }
}

@PageUrl("/verify-email-already")
open class SecondaryEmailAlreadyVerifiedPage : AuthPage<SecondaryEmailAlreadyVerifiedPage>(
  "HMPPS Digital Services - Verify Backup Email Confirmation",
  "Backup email already verified"
) {
  @FindBy(css = "#continue")
  private lateinit var continueButton: FluentWebElement

  fun continueToAccountDetailsPage() {
    assertThat(continueButton.text()).isEqualTo("OK, continue")
    continueButton.click()
  }
}

@PageUrl("/verify-email-failure")
open class VerifyEmailInvalidTokenPage :
  AuthPage<VerifyEmailInvalidTokenPage>("HMPPS Digital Services - Verify Email Confirmation", "This link is invalid")

@PageUrl("/verify-email-expired")
open class VerifyEmailExpiredTokenPage :
  AuthPage<VerifyEmailExpiredTokenPage>("HMPPS Digital Services - Verify Email Confirmation", "The link has expired") {
  fun getVerifyLink(): String = el("#link").attribute("href")
}
