package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class ChangeMobileSpecification : AbstractAuthSpecification() {
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
  private lateinit var accountDetailsPage: AccountDetailsPage

  @Test
  fun `Change mobile flow`() {
    goTo(loginPage).loginAs("AUTH_CHANGE_MOBILE")

    goTo(changeMobilePage)
      .addMobileAs("07987654321")

    verifyMobileSentPage.isAtPage()
      .submitCode()

    verifyMobileConfirmPage.isAtPage()
      .continueToAccountDetailsPage()

    accountDetailsPage.isAtPage()
  }

  @Test
  fun `Change mobile invalid number entered`() {
    goTo(loginPage).loginAs("AUTH_CHANGE_MOBILE2")

    goTo(addMobilePage)
      .setMobileAs("07")
      .checkError("Enter a mobile number in the correct format")
      .updateMobileAs("", "07")
      .checkError("Enter a mobile number")
  }

  @Test
  fun `Change mobile flow invalid verification code`() {
    goTo(loginPage).loginAs("AUTH_CHANGE_MOBILE")

    goTo(changeMobilePage)
      .setMobileAs("07700900322")

    verifyMobileSentPage.isAtPage()
      .submitCode("123456")
      .checkError("Enter the code received in the text message")
  }

  @Test
  fun `current verified mobile number re-entered`() {

    goTo(loginPage).loginAs("AUTH_CHANGE_MOBILE_VERIFIED")

    goTo(changeMobilePage)
      .updateMobileAs("07700900321", "07700900321")

    verifyMobileAlreadyPage.isAtPage()
      .continueToAccountDetailsPage()

    accountDetailsPage.isAt()
  }
}

@PageUrl("/change-mobile")
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

@PageUrl("/change-mobile")
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

  fun addMobileAs(mobile: String) {
    this.mobile.fill().withText(mobile)
    assertThat(changeMobileButton.value()).isEqualTo("Continue")
    changeMobileButton.click()
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
