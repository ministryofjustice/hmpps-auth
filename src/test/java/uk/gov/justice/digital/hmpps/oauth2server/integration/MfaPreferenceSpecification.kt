package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class MfaPreferenceSpecification : AbstractAuthSpecification() {

  @Page
  private lateinit var mfaPreferencePage: MfaPreferencePage

  @Page
  private lateinit var accountDetailsPage: AccountDetailsPage

  @Page
  private lateinit var homePage: HomePage

  @Test
  fun `mfa Preference flow select text`() {
    goTo(loginPage)
        .loginWithMfaEmail("AUTH_MFA_PREF_EMAIL")
        .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage
        .isAtPage()
        .navigateToChangeMfaPreference()

    mfaPreferencePage.selectText()

    accountDetailsPage.checkMfaPreferenceIsText()

    accountDetailsPage
        .isAtPage()
        .navigateToChangeMfaPreference()

    mfaPreferencePage.selectEmail()

  }

  @Test
  fun `mfa Preference flow select email`() {
    goTo(loginPage)
        .loginWithMfaText("AUTH_MFA_PREF_TEXT")
        .submitCode()

    homePage.navigateToAccountDetails()

    accountDetailsPage
        .isAtPage()
        .navigateToChangeMfaPreference()

    mfaPreferencePage.selectEmail()

    accountDetailsPage
        .isAtPage()
        .checkMfaPreferenceIsEmail()

    accountDetailsPage.navigateToChangeMfaPreference()

    mfaPreferencePage.selectText()

  }

  @Test
  fun `mfa preference hint text shows user their primary email, mobile and secondary email`() {
    goTo(loginPage)
        .loginAs("AUTH_MFA_PREF_EMAIL_AND_TEXT")

    homePage.navigateToAccountDetails()

    accountDetailsPage
        .isAtPage()
        .navigateToChangeMfaPreference()

    mfaPreferencePage.assertEmailHintText("You will receive a 6 digit number to auth_email@digital.justice.gov.uk")
    mfaPreferencePage.assertMobileHintText("You will receive a 6 digit number to 07700900321 by text message")
    mfaPreferencePage.assertSecondaryEmailHintText("You will receive a 6 digit number to john@smith.com")
  }

  @Test
  fun `mfa preference hint text tells user they need to add and verify primary email, mobile and secondary email`() {
    goTo(loginPage)
        .loginAsWithUnverifiedEmail("AUTH_MFA_PREF_NO_EMAIL_TEXT")
        .cancel()

    homePage.navigateToAccountDetails()

    accountDetailsPage
        .isAtPage()
        .navigateToChangeMfaPreference()

    mfaPreferencePage.assertEmailHintText("You will need to add and verify your work email")
    mfaPreferencePage.assertMobileHintText("You will need to add and verify a mobile number")
    mfaPreferencePage.assertSecondaryEmailHintText("You will need to add and verify a backup email")
  }
}

@PageUrl("/mfa-preference")
open class MfaPreferencePage : AuthPage<MfaPreferencePage>("HMPPS Digital Services - Security Code Preference", "How would you prefer to receive your security code?") {
  @FindBy(css = "input[type='submit']")
  private lateinit var save: FluentWebElement

  @FindBy(css = "input[id='mfa-pref-email']")
  private lateinit var email: FluentWebElement

  @FindBy(css = "input[id='mfa-pref-text']")
  private lateinit var text: FluentWebElement

  @FindBy(css = "#mfa-pref-email-item-hint")
  private lateinit var emailHintText: FluentWebElement

  @FindBy(css = "#mfa-pref-text-item-hint")
  private lateinit var mobileHintText: FluentWebElement

  @FindBy(css = "#mfa-pref-backup-email-item-hint")
  private lateinit var secondaryEmailHintText: FluentWebElement

  fun selectText() {
    text.click()
    save.submit()
  }

  fun selectEmail() {
    email.click()
    save.submit()
  }

  fun assertEmailHintText(text: String) {
    assertThat(emailHintText.text()).isEqualTo(text)
  }

  fun assertMobileHintText(text: String) {
    assertThat(mobileHintText.text()).isEqualTo(text)
  }

  fun assertSecondaryEmailHintText(text: String) {
    assertThat(secondaryEmailHintText.text()).isEqualTo(text)
  }
}

