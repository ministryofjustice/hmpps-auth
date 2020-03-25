package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy
import uk.gov.justice.digital.hmpps.oauth2server.integration.VerifySecondaryEmailSpecification.VerifySecondaryEmailConfirmPage

class AddChangeSecondaryEmailSpecification : AbstractAuthSpecification() {

  @Page
  private lateinit var accountDetailsPage: AccountDetailsPage

  @Page
  private lateinit var homePage: HomePage

  @Page
  private lateinit var changeSecondaryEmailPage: ChangeSecondaryEmailPage

  @Page
  private lateinit var changeEmailPage: ChangeEmailPage

  @Page
  private lateinit var verifyEmailSentPage: VerifyEmailSentPage

  @Page
  private lateinit var verifySecondaryEmailConfirmPage: VerifySecondaryEmailConfirmPage

  @Page
  private lateinit var secondaryEmailAlreadyVerifiedPage: SecondaryEmailAlreadyVerifiedPage

  @Test
  fun `Add Secondary mobile flow but not verify`() {
    goTo(loginPage)
        .loginAs("AUTH_SECOND_EMAIL_ADD")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    changeSecondaryEmailPage.addSecondaryEmailAs("bob@gmail.com")

    goTo(accountDetailsPage)
        .isAtPage()
        .checkSecondaryEmailAndIsNotVerified()

  }

  @Test
  fun `Add Secondary mobile flow and verify`() {
    goTo(loginPage)
        .loginAs("AUTH_SECOND_EMAIL_UPDATE")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

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

    goTo(changeSecondaryEmailPage)
        .updateSecondaryEmailAs("bob@justice.gsi.gov.uk")

    changeEmailPage.checkError("All gsi.gov.uk have now been migrated to a justice.gov.uk domain. Enter your justice.gov.uk address instead.")
  }

  @Test
  fun `A user is not allowed to add an invalid secondary email address`() {
    goTo(loginPage)
        .loginAs("AUTH_SECOND_EMAIL_UPDATE")

    goTo(changeSecondaryEmailPage)
        .updateSecondaryEmailAs("bob@justice")

    changeEmailPage.checkError("Enter an email address in the correct format")
  }

  @Test
  fun `Change secondary email flow current verified email re-entered`() {
    goTo(loginPage)
        .loginAs("AUTH_SECOND_EMAIL_ALREADY")

    homePage.navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    changeSecondaryEmailPage.updateSecondaryEmailAs("john@smith.com")

    secondaryEmailAlreadyVerifiedPage.isAt()


  }
}

@PageUrl("/new-secondary-email")
open class ChangeSecondaryEmailPage : AuthPage<ChangeSecondaryEmailPage>("HMPPS Digital Services - Change Secondary Email", "What is your new secondary email address?") {
  @FindBy(css = "input[type='submit']")
  private lateinit var changeSecondaryEmailButton: FluentWebElement
  private lateinit var email: FluentWebElement

  fun addSecondaryEmailAs(email: String) {
    this.email.fill().withText(email)
    assertThat(changeSecondaryEmailButton.value()).isEqualTo("Add")
    changeSecondaryEmailButton.click()
  }

  fun updateSecondaryEmailAs(email: String) {
    this.email.fill().withText(email)
    assertThat(changeSecondaryEmailButton.value()).isEqualTo("Update")
    changeSecondaryEmailButton.click()
  }
}
