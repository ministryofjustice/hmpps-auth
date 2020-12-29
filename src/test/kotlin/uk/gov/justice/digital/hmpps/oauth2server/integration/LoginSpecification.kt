package uk.gov.justice.digital.hmpps.oauth2server.integration

import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy
import uk.gov.justice.digital.hmpps.oauth2server.resource.TokenVerificationExtension.Companion.tokenVerificationApi

class LoginSpecification : AbstractDeliusAuthSpecification() {
  @Page
  internal lateinit var homePage: HomePage

  @Page
  lateinit var changeExpiredPasswordPage: ChangeExpiredPasswordPage

  @Test
  fun `Log in with valid auth credentials`() {
    val homePage = goTo(loginPage).loginAs("AUTH_USER")
    homePage.assertNameDisplayedCorrectly("Auth Only")
  }

  @Test
  fun `Log in with valid auth credentials sets jwt cookie`() {
    val homePage = goTo(loginPage).loginAs("AUTH_USER")
    val jwt = homePage.parseJwt()
    assertThat(jwt.getStringClaim("name")).isEqualTo("Auth Only")
    assertThat(jwt.getStringClaim("auth_source")).isEqualTo("auth")
    assertThat(jwt.getStringClaim("user_id")).isEqualTo("608955ae-52ed-44cc-884c-011597a77949")
  }

  @Test
  fun `Log in sets mfa passed in jwt cookie to false`() {
    val homePage = goTo(loginPage).loginAs("AUTH_USER")
    val jwt = homePage.parseJwt()
    assertThat(jwt.getBooleanClaim("passed_mfa")).isFalse
  }

  @Test
  fun `Log in with valid nomis credentials`() {
    val homePage = goTo(loginPage).loginAs("ITAG_USER", "password")
    homePage.assertNameDisplayedCorrectly("Itag User")
  }

  @Test
  fun `first time Log in with valid nomis credentials`() {
    val homePage = goTo(loginPage).loginAs("nomis_email", "password123456")
    homePage.assertNameDisplayedCorrectly("Nomis Email")
  }

  @Test
  fun `Log in with valid nomis credentials sets jwt cookie`() {
    val homePage = goTo(loginPage).loginAs("ITAG_USER", "password")
    val jwt = homePage.parseJwt()
    assertThat(jwt.getStringClaim("name")).isEqualTo("Itag User")
    assertThat(jwt.getStringClaim("auth_source")).isEqualTo("nomis")
    assertThat(jwt.getStringClaim("user_id")).isEqualTo("1")
  }

  @Test
  fun `Log in with valid delius credentials`() {
    val homePage = goTo(loginPage).loginAs("DELIUS_USER", "password")
    homePage.assertNameDisplayedCorrectly("Delius Smith")
  }

  @Test
  fun `Log in with valid delius credentials sets jwt cookie`() {
    val homePage = goTo(loginPage).loginAs("DELIUS_USER", "password")
    val jwt = homePage.parseJwt()
    assertThat(jwt.getStringClaim("name")).isEqualTo("Delius Smith")
    assertThat(jwt.getStringClaim("auth_source")).isEqualTo("delius")
    assertThat(jwt.getStringClaim("user_id")).isEqualTo("2500077027")
  }

  @Test
  fun `Log in with valid credentials in lower case`() {
    val homePage = goTo(loginPage).loginAs("itag_user", "password")
    homePage.assertNameDisplayedCorrectly("Itag User")
  }

  @Test
  fun `I can logout once logged in`() {
    val homePage = goTo(loginPage).loginAs("itag_user", "password")

    homePage.logOut()

    loginPage.isAtPage().checkLoggedOutMessage()
  }

  @Test
  fun `I can logout once logged in and send token to verification service (requires token-verification spring profile)`() {
    val homePage = goTo(loginPage).loginAs("itag_user", "password")
    val authJwtId = homePage.parseJwt().jwtid

    homePage.logOut()

    tokenVerificationApi.verify(
      deleteRequestedFor(urlPathEqualTo("/token"))
        .withQueryParam("authJwtId", equalTo(authJwtId))
    )
  }

  @Test
  fun `I can login again without logging out and send token to verification service (requires token-verification spring profile)`() {
    val oldJwtId = goTo(loginPage).loginAs("itag_user", "password").parseJwt().jwtid
    val homePage = goTo(loginPage).loginAs("itag_user_adm")

    // check now logged in as new user
    val jwt = homePage.parseJwt()
    assertThat(jwt.getStringClaim("sub")).isEqualTo("ITAG_USER_ADM")
    assertThat(jwt.jwtid).isNotEqualTo(oldJwtId)

    tokenVerificationApi.verify(
      deleteRequestedFor(urlPathEqualTo("/token"))
        .withQueryParam("authJwtId", equalTo(oldJwtId))
    )
  }

  @Test
  fun `Log in with valid credentials same user name in Auth and Delius but Auth account disabled`() {
    val homePage = goTo(loginPage).loginAs("DELIUS_ENABLED_AUTH_DISABLED", "password")
    homePage.assertNameDisplayedCorrectly("Delius Smith")
  }

  @Test
  fun `Log in with valid credentials same user name in Auth and Nomis but Auth account disabled`() {
    val homePage = goTo(loginPage).loginAs("NOMIS_ENABLED_AUTH_DISABLED", "password123456")
    homePage.assertNameDisplayedCorrectly("Nomis Enabled Auth Disabled")
  }

  @Test
  fun `Log in fails with valid credentials same user name in Auth and Nomis but both accounts disabled`() {
    goTo(loginPage).loginError("NOMIS_LOCKED_AUTH_DISABLED", "password123456")
      .checkError("Your account is locked. If you have verified your email address then you can use 'I have forgotten my password' below.")
  }

  @Test
  fun `Log in with Azure justice email credentials link results in successful login`() {
    goTo(loginPage).clickAzureOIDCLink()
    homePage.isAt()
  }

  @Test
  fun `Attempt login with unknown user`() {
    goTo(loginPage).loginError("NOT_KNOWN", "password123456")
      .checkError("Enter a valid username and password. You will be locked out if you enter the wrong details 3 times.")
  }

  @Test
  fun `Attempt login without credentials`() {
    goTo(loginPage).loginError("ITAG_USER", "")
      .checkError("Enter your password")
  }

  @Test
  fun `Attempt login with invalid credentials`() {
    goTo(loginPage).loginError("ITAG_USER", "wrong")
      .checkError("Enter a valid username and password. You will be locked out if you enter the wrong details 3 times.")
  }

  @Test
  fun `Attempt login with locked user`() {
    goTo(loginPage).loginError("LOCKED_USER", "password123456")
      .checkError("Your account is locked. If you have verified your email address then you can use 'I have forgotten my password' below.")
  }

  @Test
  fun `Attempt login with locked auth user`() {
    goTo(loginPage).loginError("AUTH_LOCKED", "password123456")
      .checkError("Your account is locked. If you have verified your email address then you can use 'I have forgotten my password' below.")
  }

  @Test
  fun `Delius user gets locked after 3 invalid login attempts`() {
    goTo(loginPage).loginError("DELIUS_LOCKED_IN_AUTH", "wrongpassword")
      .checkError("Enter a valid username and password. You will be locked out if you enter the wrong details 3 times.")

    goTo(loginPage).loginError("DELIUS_LOCKED_IN_AUTH", "wrongpassword")
      .checkError("Enter a valid username and password. You will be locked out if you enter the wrong details 3 times.")

    goTo(loginPage).loginError("DELIUS_LOCKED_IN_AUTH", "wrongpassword")
      .checkError("Your account is locked. If you have verified your email address then you can use 'I have forgotten my password' below.")
  }

  @Test
  fun `Attempt login with disabled delius user`() {
    goTo(loginPage).loginError("DELIUS_ERROR_LOCKED", "password123456")
      .checkError("Enter a valid username and password. You will be locked out if you enter the wrong details 3 times.")
  }

  @Test
  fun `Attempt login with disabled auth user`() {
    goTo(loginPage).loginError("AUTH_DISABLED", "password123456")
      .checkError("Enter a valid username and password. You will be locked out if you enter the wrong details 3 times.")
  }

  @Test
  fun `Attempt login with expired user wrong password`() {
    goTo(loginPage).loginError("EXPIRED_USER", "wrong")
      .checkError("Enter a valid username and password. You will be locked out if you enter the wrong details 3 times.")
  }

  @Test
  fun `Attempt login with expired user`() {
    goTo(loginPage).loginExpiredUser("EXPIRED_USER", "password123456")
    changeExpiredPasswordPage.isAtPage()
  }

  @Test
  fun `Attempt login when delius connections time out`() {
    // dev-config defines timeout to delius as 2 seconds.  The DELIUS_ERROR_TIMEOUT user has a success mapping,
    // but with fixed delay of 2 seconds which should therefore cause the timeout.
    // If timeout not working then login will succeed instead and test will fail.
    goTo(loginPage).loginError("DELIUS_ERROR_TIMEOUT", "password123456")
      .checkError(
        "Enter a valid username and password. You will be locked out if you enter the wrong details 3 times." +
          "\nDelius is experiencing issues. Please try later if you are attempting to sign in using your Delius credentials."
      )
  }

  @Test
  fun `Attempt login with Delius unavailable (gateway returns 503)`() {
    goTo(loginPage).loginError("DELIUS_ERROR_SERVER", "password")
      .checkError(
        "Enter a valid username and password. You will be locked out if you enter the wrong details 3 times." +
          "\nDelius is experiencing issues. Please try later if you are attempting to sign in using your Delius credentials."
      )
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

  @FindBy(linkText = "I have forgotten my password")
  private lateinit var forgottenPassword: FluentWebElement

  @FindBy(id = "oauth2-client-microsoft")
  private lateinit var azureOIDCLink: FluentWebElement

  fun loginAsWithUnverifiedEmail(username: String, password: String = "password123456"): VerifyEmailPage =
    loginWith(username, password, VerifyEmailPage::class.java)

  fun loginAs(username: String, password: String = "password123456"): HomePage =
    loginWith(username, password, HomePage::class.java)

  fun loginWithMfaEmail(username: String, password: String = "password123456"): MfaEmailPage =
    loginWith(username, password, MfaEmailPage::class.java)

  fun loginWithMfaText(username: String, password: String = "password123456"): MfaTextPage =
    loginWith(username, password, MfaTextPage::class.java)

  fun loginError(username: String, password: String = "password123456"): LoginPage =
    errorLoginWith(username, password, LoginPage::class.java)

  fun loginExpiredUser(username: String, password: String = "password123456"): ChangeExpiredPasswordPage =
    errorLoginWith(username, password, ChangeExpiredPasswordPage::class.java)

  fun loginExistingPasswordChangeEmail(username: String, password: String = "password123456"): PasswordPromptForEmailPage =
    errorLoginWith(username, password, PasswordPromptForEmailPage::class.java)

  fun loginPageNotFound(username: String, password: String = "password123456"): PageNotFoundPage =
    errorLoginWith(username, password, PageNotFoundPage::class.java)

  private fun <T : AuthPage<T>> loginWith(username: String, password: String = "password123456", t: Class<T>): T {
    submitLogin(username, password)
    return newInstance(t).isAtPage()
  }

  private fun <T : AuthPage<T>> errorLoginWith(username: String, password: String = "password123456", t: Class<T>): T {
    submitLogin(username, password)
    return newInstance(t)
  }

  fun submitLogin(username: String, password: String = "password123456") {
    this.username.fill().withText(username)
    this.password.fill().withText(password)
    signInButton.submit()
  }

  fun viewTerms() {
    val termsLink = el("a[data-qa='terms']")
    assertThat(termsLink.text()).isEqualTo("Terms and conditions")
    termsLink.click()
  }

  fun checkLoggedOutMessage() {
    val warning = el("#warning")
    assertThat(warning.text()).isEqualTo("Warning\nYou have been signed out")
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

  fun forgottenPasswordLink() {
    forgottenPassword.click()
  }

  fun clickAzureOIDCLink() {
    azureOIDCLink.click()
  }
}
