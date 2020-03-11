package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class LoginSpecification : AbstractAuthSpecification() {
  @Test
  fun `Log in with valid auth credentials`() {
    val homePage = goTo(loginPage).loginAs("AUTH_USER")
    homePage.assertNameDisplayedCorrectly("Auth Only")
  }

  @Test
  fun `Log in with valid delius credentials`() {
    val homePage = goTo(loginPage).loginAs("DELIUS_USER", "password")
    homePage.assertNameDisplayedCorrectly("Delius Smith")
  }
}

@PageUrl("/login")
class LoginPage : AuthPage<LoginPage>("HMPPS Digital Services - Sign in", "Sign in") {
  @FindBy(css = "input[type='submit']")
  private lateinit var signInButton: FluentWebElement

  @FindBy(css = "input[name='username']")
  private lateinit var username: FluentWebElement

  @FindBy(css = "input[name='password']")
  private lateinit var password: FluentWebElement

  fun loginAsWithUnverifiedEmail(username: String, password: String = "password123456"): VerifyEmailPage =
      loginWith(username, password, VerifyEmailPage::class.java)

  fun loginAs(username: String, password: String = "password123456"): HomePage =
      loginWith(username, password, HomePage::class.java)

  fun loginWithMfaEmail(username: String, password: String = "password123456"): MfaEmailPage =
      loginWith(username, password, MfaEmailPage::class.java)

  fun loginWithMfaText(username: String, password: String = "password123456"): MfaTextPage =
      loginWith(username, password, MfaTextPage::class.java)

  fun loginWithMfaError(username: String, password: String = "password123456"): LoginPage =
      errorLoginWith(username, password, LoginPage::class.java)

  private fun <T : AuthPage<T>> loginWith(username: String, password: String = "password123456", t: Class<T>): T {
    this.username.fill().withText(username)
    this.password.fill().withText(password)
    signInButton.submit()

    val authPage = newInstance(t)
    authPage.isAt()
    return authPage
  }

  private fun <T : AuthPage<T>> errorLoginWith(username: String, password: String = "password123456", t: Class<T>): T {
    this.username.fill().withText(username)
    this.password.fill().withText(password)
    signInButton.submit()

    val authPage = newInstance(t)
    return authPage
  }

  fun viewContact() {
    val contactLink = el("a[data-qa='contact']")
    assertThat(contactLink.text()).isEqualTo("Contact")
    contactLink.click()
  }

  fun viewTerms() {
    val termsLink = el("a[data-qa='terms']")
    assertThat(termsLink.text()).isEqualTo("Terms and conditions")
    termsLink.click()
  }

  fun checkLoginAuthenticationFailedError(): LoginPage {
    checkError("Your authentication request failed. You will be locked out if you enter the wrong details 3 times.")
    return this
  }

  fun checkLoginAuthenticationTimeoutError(): LoginPage {
    checkError("Your authentication request has timed out. Enter your username and password to start again.")
    return this
  }

  fun checkLoginUsernamePasswordError(): LoginPage {
    checkError("Enter a valid username and password. You will be locked out if you enter the wrong details 3 times.")
    return this
  }

  fun checkLoginAccountLockedError(): LoginPage {
    checkError("Your account is locked. If you have verified your email address then you can use 'I have forgotten my password' below.")
    return this
  }

}
