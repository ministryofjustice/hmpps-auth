package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class ForgottenUsernameSpecification : AbstractDeliusAuthSpecification() {

  @Page
  private lateinit var forgottenUsernameRequestPage: ForgottenUsernameRequestPage

  @Page
  private lateinit var forgottenUsernameEmailSentPage: ForgottenUsernameEmailSentPage

  @Test
  fun `forgotten username`() {
    goTo(loginPage)
      .forgottenUsernameLink()

    forgottenUsernameRequestPage
      .isAtPage()
      .enterEmailAndSubmit("mfa_user@digital.justice.gov.uk")

    forgottenUsernameEmailSentPage
      .isAtPage()
      .checkUsernames()
      .returnToSignInPage()

    loginPage.isAtPage()
  }

  @Test
  fun `forgotten username - no email entered`() {
    goTo(loginPage)
      .forgottenUsernameLink()

    forgottenUsernameRequestPage
      .isAtPage()
      .enterEmailAndSubmit("")
      .isAtError()
      .checkMissingEmailError()
  }

  @Test
  fun `forgotten username - none work email entered`() {
    goTo(loginPage)
      .forgottenUsernameLink()

    forgottenUsernameRequestPage
      .isAtPage()
      .enterEmailAndSubmit("bob@notvalid.com")
      .isAtError()
      .checkWorkEmailError()
  }

  @PageUrl("/forgotten-username")
  open class ForgottenUsernameRequestPage : AuthPage<ForgottenUsernameRequestPage>(
    "HMPPS Digital Services - Forgotten Username",
    "Recover your username"
  ) {
    @FindBy(css = "input[type='submit']")
    private lateinit var submitEmail: FluentWebElement

    @FindBy(css = "input[id='email']")
    private lateinit var email: FluentWebElement

    fun enterEmailAndSubmit(email: String): ForgottenUsernameRequestPage {
      this.email.fill().withText(email)
      submitEmail.submit()
      return this
    }

    fun checkMissingEmailError(): ForgottenUsernameRequestPage {
      checkError("Enter your email address")
      return this
    }

    fun checkWorkEmailError(): ForgottenUsernameRequestPage {
      checkError("Enter your work email address in the correct format")
      return this
    }
  }

  @PageUrl("/forgotten-username")
  open class ForgottenUsernameEmailSentPage : AuthPage<ForgottenUsernameEmailSentPage>(
    "HMPPS Digital Services - Forgotten Username Email Sent",
    "Check your email"
  ) {
    @FindBy(linkText = "sign in")
    private lateinit var signin: FluentWebElement

    fun checkUsernames(): ForgottenUsernameEmailSentPage {
      assertThat(el("[id='username1']").text()).isEqualToNormalizingWhitespace("AUTH_MFA_CHANGE")
      assertThat(el("[id='username2']").text()).isEqualToNormalizingWhitespace("AUTH_MFA_EXPIRED_USER")
      assertThat(el("[id='username3']").text()).isEqualToNormalizingWhitespace("AUTH_MFA_LOCKED2_EMAIL")
      assertThat(el("[id='username4']").text()).isEqualToNormalizingWhitespace("AUTH_MFA_LOCKED3_EMAIL")
      assertThat(el("[id='username5']").text()).isEqualToNormalizingWhitespace("AUTH_MFA_LOCKED4_EMAIL")
      assertThat(el("[id='username6']").text()).isEqualToNormalizingWhitespace("AUTH_MFA_LOCKED5_EMAIL")
      assertThat(el("[id='username7']").text()).isEqualToNormalizingWhitespace("AUTH_MFA_LOCKED6_EMAIL")
      assertThat(el("[id='username8']").text()).isEqualToNormalizingWhitespace("AUTH_MFA_LOCKED_EMAIL")
      assertThat(el("[id='username9']").text()).isEqualToNormalizingWhitespace("AUTH_MFA_TOKEN_USER")
      assertThat(el("[id='username10']").text()).isEqualToNormalizingWhitespace("AUTH_MFA_USER")
      return this
    }

    fun returnToSignInPage() {
      signin.click()
    }
  }
}
