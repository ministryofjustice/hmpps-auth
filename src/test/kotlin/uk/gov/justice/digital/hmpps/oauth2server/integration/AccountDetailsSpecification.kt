package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.oauth2server.resource.AzureOIDCExtension

class AccountDetailsSpecification : AbstractDeliusAuthSpecification() {
  @Page
  private lateinit var accountDetailsPage: AccountDetailsPage

  @Test
  fun `auth account details`() {
    goTo(loginPage).loginAs("AUTH_RO_USER")
      .navigateToAccountDetails()

    accountDetailsPage.isAt()
    accountDetailsPage.checkAuthDetails()
  }

  @Test
  fun `auth account details with username as email`() {
    goTo(loginPage).loginAs("AUTH_RO_USER1@DIGITAL.JUSTICE.GOV.UK")
      .navigateToAccountDetails()

    accountDetailsPage.isAt()
    accountDetailsPage.checkAuthEmailUsernameDetails()
  }

  @Test
  fun `nomis account details`() {
    goTo(loginPage).loginAs("ITAG_USER", "password")

    goTo(accountDetailsPage).checkNomisDetails()
  }

  @Test
  fun `azure account details`() {
    AzureOIDCExtension.azureOIDC.stubToken("multiple.user.test@digital.justice.gov.uk")
    goTo(loginPage).clickAzureOIDCLink()

    goTo(accountDetailsPage).checkAzureDetails()
  }

  @Test
  fun `unverified account details`() {
    goTo(loginPage).loginAsWithUnverifiedEmail("AUTH_UNVERIFIED")

    goTo(accountDetailsPage).checkUnverified()
  }

  @Test
  fun `cancel account details`() {
    val homePage = goTo(loginPage).loginAs("AUTH_RO_USER")
    homePage.navigateToAccountDetails()
    accountDetailsPage.isAt()

    accountDetailsPage.cancel()

    homePage.isAt()
  }

  @Test
  fun `navigation - change name`() {
    goTo(loginPage).loginAs("AUTH_RO_USER")
      .navigateToAccountDetails()

    accountDetailsPage.navigateToChangeName()

    newInstance(ChangeNamePage::class.java).isAt()
  }

  @Test
  fun `navigation - change mobile`() {
    goTo(loginPage).loginAs("AUTH_RO_USER")
      .navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMobile()

    newInstance(AddMobilePage::class.java).isAt()
  }

  @Test
  fun `navigation - change email`() {
    goTo(loginPage).loginAs("AUTH_RO_USER")
      .navigateToAccountDetails()

    accountDetailsPage.navigateToChangeEmail()

    newInstance(PasswordPromptForEmailPage::class.java).isAt()
  }

  @Test
  fun `navigation - change Secondary email`() {
    goTo(loginPage).loginAs("AUTH_RO_USER")
      .navigateToAccountDetails()

    accountDetailsPage.navigateToChangeSecondaryEmail()

    newInstance(NewSecondaryEmailPage::class.java).isAt()
  }

  @Test
  fun `navigation - change password`() {
    goTo(loginPage).loginAs("AUTH_RO_USER")
      .navigateToAccountDetails()

    accountDetailsPage.navigateToChangePassword()

    newInstance(PasswordPromptPage::class.java).isAt()
  }

  @Test
  fun `navigation - Change mfa preference`() {
    goTo(loginPage).loginAs("AUTH_RO_USER")
      .navigateToAccountDetails()

    accountDetailsPage.navigateToChangeMfaPreference()

    newInstance(MfaPreferencePage::class.java).isAt()
  }

  @Test
  fun `navigation - mfa preference email - not verified warning`() {
    goTo(loginPage).loginAsWithUnverifiedEmail("AUTH_UNVERIFIED")

    goTo(accountDetailsPage).checkUnverified()
  }

  @Test
  fun `navigation - mfa preference text - not verified warning`() {
    goTo(loginPage).loginAsWithUnverifiedEmail("AUTH_UNVERIFIED_TEXT")

    goTo(accountDetailsPage).checkMfaPreferenceTextWarning()
  }
}

@PageUrl("/account-details")
class AccountDetailsPage :
  AuthPage<AccountDetailsPage>("HMPPS Digital Services - Your Account Details", "Your account details") {
  @Suppress("UsePropertyAccessSyntax")
  fun checkAuthDetails(): AccountDetailsPage {
    assertThat(el("[data-qa='changePassword']").text()).isEqualToNormalizingWhitespace("Change your password")
    assertThat(el("[data-qa='username']").text()).isEqualTo("AUTH_RO_USER")
    assertThat(el("[data-qa='name']").text()).isEqualTo("Ryan-Auth Orton")
    assertThat(el("[data-qa='changeName']").text()).isEqualToNormalizingWhitespace("Change name")
    assertThat(el("[data-qa='usernameEmail']").text()).isEqualToNormalizingWhitespace("Email address")
    assertThat(el("[data-qa='email']").text()).isEqualTo("auth_ro_user@digital.justice.gov.uk")
    assertThat(el("[data-qa='changeEmail']").text()).isEqualToNormalizingWhitespace("Change email")
    assertThat(el("[data-qa='verified']").text()).isEqualTo("Yes")
    assertThat(find("[data-qa='verifyEmail']")).isEmpty()
    assertThat(el("[data-qa='secondaryEmail']").text()).isBlank()
    assertThat(el("[data-qa='changeSecondaryEmail']").text()).isEqualToNormalizingWhitespace("Add backup email")
    assertThat(el("[data-qa='verifiedSecondaryEmail']").text()).isEqualTo("No")
    assertThat(find("[data-qa='verifySecondaryEmail']")).isEmpty()
    assertThat(el("[data-qa='mobile']").text()).isBlank()
    assertThat(el("[data-qa='changeMobile']").text()).isEqualToNormalizingWhitespace("Add mobile number")
    assertThat(el("[data-qa='mobileVerified']").text()).isEqualTo("No")
    assertThat(find("[data-qa='verifyMobile']")).isEmpty()
    assertThat(el("[data-qa='mfaPreference']").text()).isEqualTo("Email")
    assertThat(el("[data-qa='changeMfaPreference']").text()).isEqualToNormalizingWhitespace("Change 2-step verification preference")
    return this
  }

  fun checkAuthEmailUsernameDetails(): AccountDetailsPage {
    assertThat(el("[data-qa='changePassword']").text()).isEqualToNormalizingWhitespace("Change your password")
    assertThat(el("[data-qa='name']").text()).isEqualTo("Ryan-Auth Orton")
    assertThat(el("[data-qa='changeName']").text()).isEqualToNormalizingWhitespace("Change name")
    assertThat(el("[data-qa='usernameEmail']").text()).isEqualToNormalizingWhitespace("Username / email")
    assertThat(el("[data-qa='email']").text()).isEqualTo("auth_ro_user1@digital.justice.gov.uk")
    assertThat(el("[data-qa='changeEmail']").text()).isEqualToNormalizingWhitespace("Change email")
    assertThat(el("[data-qa='verified']").text()).isEqualTo("Yes")
    assertThat(find("[data-qa='verifyEmail']")).isEmpty()
    assertThat(el("[data-qa='secondaryEmail']").text()).isBlank()
    assertThat(el("[data-qa='changeSecondaryEmail']").text()).isEqualToNormalizingWhitespace("Add backup email")
    assertThat(el("[data-qa='verifiedSecondaryEmail']").text()).isEqualTo("No")
    assertThat(find("[data-qa='verifySecondaryEmail']")).isEmpty()
    assertThat(el("[data-qa='mobile']").text()).isBlank()
    assertThat(el("[data-qa='changeMobile']").text()).isEqualToNormalizingWhitespace("Add mobile number")
    assertThat(el("[data-qa='mobileVerified']").text()).isEqualTo("No")
    assertThat(find("[data-qa='verifyMobile']")).isEmpty()
    assertThat(el("[data-qa='mfaPreference']").text()).isEqualTo("Email")
    assertThat(el("[data-qa='changeMfaPreference']").text()).isEqualToNormalizingWhitespace("Change 2-step verification preference")
    return this
  }

  fun checkNomisDetails(): AccountDetailsPage {
    assertThat(el("[data-qa='username']").text()).isEqualTo("ITAG_USER")
    assertThat(el("[data-qa='name']").text()).isEqualTo("Itag User")
    // change name only available for auth users
    assertThat(find("[data-qa='changeName']")).isEmpty()
    assertThat(el("[data-qa='email']").text()).isEqualTo("itag_user@digital.justice.gov.uk")
    assertThat(el("[data-qa='changeEmail']").text()).isEqualToNormalizingWhitespace("Change email")
    assertThat(el("[data-qa='secondaryEmail']").text()).isBlank()
    assertThat(el("[data-qa='changeSecondaryEmail']").text()).isEqualToNormalizingWhitespace("Add backup email")
    assertThat(el("[data-qa='verifiedSecondaryEmail']").text()).isEqualTo("No")
    assertThat(find("[data-qa='verifySecondaryEmail']")).isEmpty()
    assertThat(el("[data-qa='mfaPreference']").text()).isEqualTo("Email")
    assertThat(el("[data-qa='changeMfaPreference']").text()).isEqualToNormalizingWhitespace("Change 2-step verification preference")
    return this
  }

  fun checkAzureDetails(): AccountDetailsPage {
    assertThat(el("[data-qa='name']").text()).isEqualTo("Test User")
    assertThat(find("[data-qa='changeName']")).isEmpty()
    assertThat(find("[data-qa='changePassword']")).isEmpty()
    assertThat(find("[data-qa='changeEmail']")).isEmpty()

    assertThat(el("[data-qa='email']").text()).isEqualTo("multiple.user.test@digital.justice.gov.uk")
    assertThat(el("[data-qa='secondaryEmail']").text()).isBlank
    assertThat(el("[data-qa='changeSecondaryEmail']").text()).isEqualToNormalizingWhitespace("Add backup email")
    assertThat(el("[data-qa='verifiedSecondaryEmail']").text()).isEqualTo("No")
    assertThat(find("[data-qa='verifySecondaryEmail']")).isEmpty()
    assertThat(el("[data-qa='mfaPreference']").text()).isEqualTo("Email")
    assertThat(el("[data-qa='changeMfaPreference']").text()).isEqualToNormalizingWhitespace("Change 2-step verification preference")

    assertThat(el("[data-qa='linkedAccountsHeading']").text()).isEqualTo("Your linked accounts")
    assertThat(find("[data-qa='linkedAccount']").count()).isEqualTo(4)

    assertThat(el("[data-qa='system-0']").text()).isEqualTo("NOMIS")
    assertThat(el("[data-qa='username-0']").text()).isEqualTo("ITAG_USER")

    assertThat(el("[data-qa='system-1']").text()).isEqualTo("NOMIS")
    assertThat(el("[data-qa='username-1']").text()).isEqualTo("ITAG_USER_ADM")

    assertThat(el("[data-qa='system-2']").text()).isEqualTo("DELIUS")
    assertThat(el("[data-qa='username-2']").text()).isEqualTo("DELIUSSMITH")

    assertThat(el("[data-qa='system-3']").text()).isEqualTo("DELIUS")
    assertThat(el("[data-qa='username-3']").text()).isEqualTo("DELIUSSMITH2")
    return this
  }

  fun checkUnverified(): AccountDetailsPage {
    assertThat(el("[data-qa='username']").text()).isEqualTo("AUTH_UNVERIFIED")
    assertThat(el("[data-qa='email']").text()).isEqualTo("auth_unverified@digital.justice.gov.uk")
    assertThat(el("[data-qa='changeEmail']").text()).isEqualToNormalizingWhitespace("Change email")
    assertThat(el("[data-qa='verified']").text()).isEqualTo("No")
    assertThat(el("[data-qa='verifyEmail']").text()).isEqualToNormalizingWhitespace("Resend email code")
    assertThat(el("[data-qa='mobile']").text()).isEqualTo("07700900321")
    assertThat(el("[data-qa='secondaryEmail']").text()).isBlank()
    assertThat(el("[data-qa='changeSecondaryEmail']").text()).isEqualToNormalizingWhitespace("Add backup email")
    assertThat(el("[data-qa='verifiedSecondaryEmail']").text()).isEqualTo("No")
    assertThat(find("[data-qa='verifySecondaryEmail']")).isEmpty()
    assertThat(el("[data-qa='changeMobile']").text()).isEqualToNormalizingWhitespace("Change mobile number")
    assertThat(el("[data-qa='mobileVerified']").text()).isEqualTo("No")
    assertThat(el("[data-qa='verifyMobile']").text()).isEqualToNormalizingWhitespace("Resend mobile code")
    assertThat(el("[id='mfa-preference-not-verified-error']").text()).isEqualTo("Selection has not been verified")
    assertThat(el("[data-qa='mfaPreference']").text()).isEqualToNormalizingWhitespace("Email Selection has not been verified")
    assertThat(el("[data-qa='changeMfaPreference']").text()).isEqualToNormalizingWhitespace("Change 2-step verification preference")
    return this
  }

  fun checkMfaPreferenceTextWarning(): AccountDetailsPage {
    assertThat(el("[data-qa='username']").text()).isEqualTo("AUTH_UNVERIFIED_TEXT")
    assertThat(el("[data-qa='email']").text()).isEqualTo("auth_unverified@digital.justice.gov.uk")
    assertThat(el("[data-qa='changeEmail']").text()).isEqualToNormalizingWhitespace("Change email")
    assertThat(el("[data-qa='verified']").text()).isEqualTo("No")
    assertThat(el("[data-qa='verifyEmail']").text()).isEqualToNormalizingWhitespace("Resend email code")
    assertThat(el("[data-qa='secondaryEmail']").text()).isBlank()
    assertThat(el("[data-qa='changeSecondaryEmail']").text()).isEqualToNormalizingWhitespace("Add backup email")
    assertThat(el("[data-qa='verifiedSecondaryEmail']").text()).isEqualTo("No")
    assertThat(find("[data-qa='verifySecondaryEmail']")).isEmpty()
    assertThat(el("[data-qa='mobile']").text()).isEqualTo("07700900321")
    assertThat(el("[data-qa='changeMobile']").text()).isEqualToNormalizingWhitespace("Change mobile number")
    assertThat(el("[data-qa='mobileVerified']").text()).isEqualTo("No")
    assertThat(el("[data-qa='verifyMobile']").text()).isEqualToNormalizingWhitespace("Resend mobile code")
    assertThat(el("[id='mfa-preference-not-verified-error']").text()).isEqualTo("Selection has not been verified")
    assertThat(el("[data-qa='mfaPreference']").text()).isEqualToNormalizingWhitespace("Text message Selection has not been verified")
    assertThat(el("[data-qa='changeMfaPreference']").text()).isEqualToNormalizingWhitespace("Change 2-step verification preference")
    return this
  }

  fun checkMfaPreferenceIsText(): AccountDetailsPage {
    assertThat(el("[data-qa='mfaPreference']").text()).isEqualToNormalizingWhitespace("Text message")
    assertThat(el("[data-qa='changeMfaPreference']").text()).isEqualToNormalizingWhitespace("Change 2-step verification preference")
    return this
  }

  fun checkMfaPreferenceIsEmail(): AccountDetailsPage {
    assertThat(el("[data-qa='mfaPreference']").text()).isEqualToNormalizingWhitespace("Email")
    assertThat(el("[data-qa='changeMfaPreference']").text()).isEqualToNormalizingWhitespace("Change 2-step verification preference")
    return this
  }

  fun checkSecondaryEmailAndIsNotVerified(): AccountDetailsPage {
    assertThat(el("[data-qa='secondaryEmail']").text()).isEqualToNormalizingWhitespace("bob@gmail.com")
    assertThat(el("[data-qa='verifiedSecondaryEmail']").text()).isEqualToNormalizingWhitespace("No")
    return this
  }

  fun checkSecondaryEmailAndIsVerified(): AccountDetailsPage {
    assertThat(el("[data-qa='secondaryEmail']").text()).isEqualToNormalizingWhitespace("bob@gmail.com")
    assertThat(el("[data-qa='verifiedSecondaryEmail']").text()).isEqualToNormalizingWhitespace("Yes")
    return this
  }

  fun navigateToChangeName() {
    el("[data-qa='changeName']").click()
  }

  fun navigateToUseEmail() {
    el("[data-qa='useEmail']").click()
  }

  fun navigateToChangeMobile() {
    el("[data-qa='changeMobile']").click()
  }

  fun navigateToChangeEmail() {
    el("[data-qa='changeEmail']").click()
  }

  fun navigateToChangePassword() {
    el("[data-qa='changePassword']").click()
  }

  fun navigateToChangeMfaPreference() {
    el("[data-qa='changeMfaPreference']").click()
  }

  fun navigateToChangeSecondaryEmail() {
    el("[data-qa='changeSecondaryEmail']").click()
  }

  fun navigateToResendVerifySecondaryEmail() {
    el("[data-qa='verifySecondaryEmail']").click()
  }

  fun cancel() {
    el("[data-qa='back-link']").click()
  }

  fun checkUsername(username: String) {
    assertThat(el("[data-qa='username']").text()).isEqualTo(username)
  }

  fun checkEmailUsername(username: String) {
    assertThat(el("[data-qa='email']").text()).isEqualTo(username)
  }
}
