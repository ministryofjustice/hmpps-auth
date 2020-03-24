package uk.gov.justice.digital.hmpps.oauth2server.integration

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
}

@PageUrl("/mfa-preference")
open class MfaPreferencePage : AuthPage<MfaPreferencePage>("HMPPS Digital Services - Security Code Preference", "How would you prefer to receive your security code?") {
  @FindBy(css = "input[type='submit']")
  private lateinit var save: FluentWebElement

  @FindBy(css = "input[id='mfa-pref-email']")
  private lateinit var email: FluentWebElement

  @FindBy(css = "input[id='mfa-pref-text']")
  private lateinit var text: FluentWebElement

  fun selectText() {
    text.click()
    save.submit()

  }

  fun selectEmail() {
    email.click()
    save.submit()
  }
}

