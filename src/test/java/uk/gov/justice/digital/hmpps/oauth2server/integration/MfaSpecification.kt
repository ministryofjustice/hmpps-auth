package uk.gov.justice.digital.hmpps.oauth2server.integration

import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.RandomStringUtils
import groovy.json.JsonSlurper
import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

class MfaSpecification : AbstractAuthSpecification() {

  @Page
  private lateinit var mfaEmailPage: MfaEmailPage

  @Page
  private lateinit var mfaTextPage: MfaTextPage

  @Page
  private lateinit var mfaEmailResendCodePage: MfaEmailResendCodePage

  @Page
  private lateinit var mfaTextResendCodePage: MfaTextResendCodePage

  @Page
  private lateinit var homePage: HomePage

  private val clientBaseUrl = "http://localhost:8081/login"

  @Test
  fun `Attempt MFA challenge with invalid token`() {
    goTo("/mfa-challenge?token=invalidtoken&mfaPreference=TEXT")
    loginPage.checkLoginAuthenticationFailedError()
  }

  @Test
  fun `Attempt MFA challenge with expired token`() {
    goTo("/mfa-challenge?token=mfa_expired&mfaPreference=EMAIL")
    loginPage.checkLoginAuthenticationTimeoutError()

  }

  @Test
  fun `Login as user with email MFA enabled`() {
    goTo(loginPage)
        .loginWithMfaEmail("AUTH_MFA_USER")
        .submitCode()
    homePage.isAt()
  }

  @Test
  fun `Login as user with text MFA enabled`() {
    goTo(loginPage)
        .loginWithMfaEmail("AUTH_MFA_PREF_TEXT2")
        .submitCode()
    homePage.isAt()
  }

  @Test
  fun `Login as user with MFA enabled but no email address`() {
    goTo(loginPage)
        .loginWithMfaError("AUTH_MFA_NOEMAIL_USER")

    loginPage.checkError("We need to send you a security code to login, but we can't find a verified email address. Please login on an approved network and verify your email address.")
  }

  @Test
  fun `MFA code is required - email`() {
    goTo(loginPage)
        .loginWithMfaEmail("AUTH_MFA_PREF_EMAIL2")
        .submitCode(" ")

    mfaEmailPage.checkError("Enter the code received in the email")
  }

  @Test
  fun `MFA code is required - text message`() {
    goTo(loginPage)
        .loginWithMfaText("AUTH_MFA_PREF_TEXT2")
        .submitCode(" ")

    mfaTextPage.checkError("Enter the code received in the text message")
  }

  @Test
  fun `MFA code is incorrect email`() {
    goTo(loginPage)
        .loginWithMfaEmail("AUTH_MFA_USER")
        .submitCode("123")
    mfaEmailPage.checkEmailCodeIsIncorrectError()
  }

  @Test
  fun `MFA code is incorrect text`() {
    goTo(loginPage)
        .loginWithMfaEmail("AUTH_MFA_PREF_TEXT2")
        .submitCode("123")
    mfaTextPage.checkTextCodeIsIncorrectError()
  }

  @Test
  fun `MFA user email preference gets locked after 3 invalid MFA attempts`() {
    goTo(loginPage)
        .loginWithMfaEmail("AUTH_MFA_LOCKED_EMAIL")
        .submitCode("123")
        .checkEmailCodeIsIncorrectError()
        .submitCode("123")
        .checkEmailCodeIsIncorrectError()
        .submitCode("123")

    loginPage.checkLoginAccountLockedError()
        .loginWithMfaError("AUTH_MFA_LOCKED_EMAIL")
        .checkLoginAccountLockedError()
  }

  @Test
  fun `MFA user text preference gets locked after 3 invalid MFA attempts`() {
    goTo(loginPage)
        .loginWithMfaText("AUTH_MFA_LOCKED_TEXT")
        .submitCode("123")
        .checkTextCodeIsIncorrectError()
        .submitCode("123")
        .checkTextCodeIsIncorrectError()
        .submitCode("123")

    loginPage.checkLoginAccountLockedError()
        .loginWithMfaError("AUTH_MFA_LOCKED_TEXT")
        .checkLoginAccountLockedError()
  }

  @Test
  fun `MFA user email preference gets locked after mix of MFA and login attempts`() {

    goTo(loginPage)
        .loginWithMfaError("AUTH_MFA_LOCKED2_EMAIL", "wrongpass")
        .checkLoginUsernamePasswordError()
        .loginWithMfaEmail("AUTH_MFA_LOCKED2_EMAIL")
        .submitCode("123")
        .checkEmailCodeIsIncorrectError()
        .submitCode("123456")
        .checkEmailCodeIsIncorrectError()

    goTo(loginPage)
        .loginWithMfaError("AUTH_MFA_LOCKED2_EMAIL", "wrongpass")
        .checkLoginAccountLockedError()
  }

  @Test
  fun `MFA user text preference gets locked after mix of MFA and login attempts`() {

    goTo(loginPage)
        .loginWithMfaError("AUTH_MFA_LOCKED2_TEXT", "wrongpass")
        .checkLoginUsernamePasswordError()
        .loginWithMfaText("AUTH_MFA_LOCKED2_TEXT")
        .submitCode("123")
        .checkTextCodeIsIncorrectError()
        .submitCode("123456")
        .checkTextCodeIsIncorrectError()

    goTo(loginPage)
        .loginWithMfaError("AUTH_MFA_LOCKED2_TEXT", "wrongpass")
        .checkLoginAccountLockedError()
  }

  @Test
  fun `Locked count gets reset after successful MFA login email preference`() {
    goTo(loginPage)
        .loginWithMfaEmail("AUTH_MFA_PREF_EMAIL3")

    val validMfaCode = mfaEmailPage.getCode()
    mfaEmailPage.submitCode("123")
        .checkEmailCodeIsIncorrectError()
        .submitCode("123")
        .checkEmailCodeIsIncorrectError()
        .submitCode(validMfaCode)

    homePage.isAt()

    goTo(loginPage)
        .loginWithMfaError("AUTH_MFA_PREF_EMAIL3", "wrongpass")
        .checkLoginUsernamePasswordError()
        .loginWithMfaError("AUTH_MFA_PREF_EMAIL3", "wrongpass")
        .checkLoginUsernamePasswordError()
  }

  @Test
  fun `Locked count gets reset after successful MFA login text preference`() {
    goTo(loginPage)
        .loginWithMfaText("AUTH_MFA_PREF_TEXT3")

    val validMfaCode = mfaTextPage.getCode()
    mfaTextPage.submitCode("123")
        .checkTextCodeIsIncorrectError()
        .submitCode("123")
        .checkTextCodeIsIncorrectError()
        .submitCode(validMfaCode)

    homePage.isAt()

    goTo(loginPage)
        .loginWithMfaError("AUTH_MFA_PREF_TEXT3", "wrongpass")
        .checkLoginUsernamePasswordError()
        .loginWithMfaError("AUTH_MFA_PREF_TEXT3", "wrongpass")
        .checkLoginUsernamePasswordError()
  }

  @Test
  fun `Mfa preference email - I would like the MFA code to be resent`() {
    goTo(loginPage)
        .loginWithMfaEmail("AUTH_MFA_USER")

    mfaEmailPage.resendCodeLink()

    mfaEmailResendCodePage.resendCode()

    mfaEmailPage.submitCode()

    homePage.isAt()
  }


  @Test
  fun `MFA preference text - I would like the MFA code to be resent by email`() {
    goTo(loginPage)
        .loginWithMfaEmail("AUTH_MFA_PREF_TEXT2")

    mfaTextPage.resendCodeLink()

    mfaTextResendCodePage.resendCodeByEmail()

    mfaTextPage.submitCode()

    homePage.isAt()
  }

  @Test
  fun `MFA preference text - I would like the MFA code to be resent by text`() {
    goTo(loginPage)
        .loginWithMfaEmail("AUTH_MFA_PREF_TEXT2")

    mfaTextPage.resendCodeLink()

    mfaTextResendCodePage.resendCodeByText()

    mfaTextPage.submitCode()

    homePage.isAt()
  }

  @Test
  fun `I can sign in from another client with MFA enabled email preference`() {
    val state = RandomStringUtils.random(6, true, true)
    goTo("/oauth/authorize?client_id=elite2apiclient&redirect_uri=$clientBaseUrl&response_type=code&state=$state")

    loginPage
        .loginWithMfaEmail("AUTH_MFA_PREF_EMAIL4")
        .submitCode()

    val url = driver.currentUrl
    assertThat(url).startsWith("${clientBaseUrl}?code")
    assertThat(url).contains("state=$state")

    val authCode = splitQuery(url)["code"]?.first()
    assertThat(authCode).isNotNull()

    val response = getAccessToken(authCode!!)
    assertThat(response["user_name"]).isEqualTo("AUTH_MFA_PREF_EMAIL4")
    assertThat(response["auth_source"]).isEqualTo("auth")
  }

  @Test
  fun `I can sign in from another client with MFA enabled text preference`() {
    val state = RandomStringUtils.random(6, true, true)
    goTo("/oauth/authorize?client_id=elite2apiclient&redirect_uri=$clientBaseUrl&response_type=code&state=$state")

    loginPage
        .loginWithMfaText("AUTH_MFA_PREF_TEXT4")
        .submitCode()

    val url = driver.currentUrl
    assertThat(url).startsWith("${clientBaseUrl}?code")
    assertThat(url).contains("state=$state")

    val authCode = splitQuery(url)["code"]?.first()
    assertThat(authCode).isNotNull()

    val response = getAccessToken(authCode!!)
    assertThat(response["user_name"]).isEqualTo("AUTH_MFA_PREF_TEXT4")
    assertThat(response["auth_source"]).isEqualTo("auth")
  }

  private fun splitQuery(url: String): MultiValueMap<String, String> {
    return UriComponentsBuilder.fromUriString(url).build().queryParams
  }


  private fun getAccessToken(authCode: String): Map<String, String> {

    val headers = HttpHeaders()
    headers.set("Authorization", listOf("Basic ZWxpdGUyYXBpY2xpZW50OmNsaWVudHNlY3JldA=="))

    val entity = HttpEntity("", headers)

    val response = RestTemplate().postForEntity("$baseUrl/oauth/token?grant_type=authorization_code&code=$authCode&redirect_uri=$clientBaseUrl", entity, String::class.java).body

    @Suppress("UNCHECKED_CAST")
    return JsonSlurper().parseText(response) as Map<String, String>
  }

}

@PageUrl("/mfa-challenge")
class MfaEmailPage : AuthPage<MfaEmailPage>("HMPPS Digital Services - Email Verification", "Check your email") {
  @FindBy(css = "input[type='submit']")
  private lateinit var continueButton: FluentWebElement

  @FindBy(css = "input[name='code']")
  private lateinit var code: FluentWebElement

  @FindBy(linkText = "Not received an email?")
  private lateinit var resend: FluentWebElement

  fun getCode(): String {
    return el("[data-qa='mfa-code']").text()
  }

  fun submitCode() {
    val mfaCode = el("[data-qa='mfa-code']").text()
    this.code.fill().withText(mfaCode)
    continueButton.submit()
  }

  fun submitCode(code: String): MfaEmailPage {
    this.code.fill().withText(code)
    continueButton.submit()
    return this
  }

  fun resendCodeLink() {
    resend.click()
  }

  fun checkEmailCodeIsIncorrectError(): MfaEmailPage {
    checkError("Security code is incorrect. Please check your email and try again. You will be locked out if you enter the wrong code 3 times.")
    return this
  }
}

@PageUrl("/mfa-challenge")
class MfaTextPage : AuthPage<MfaTextPage>("HMPPS Digital Services - Text Message Verification", "Check your phone") {
  @FindBy(css = "input[type='submit']")
  private lateinit var continueButton: FluentWebElement

  @FindBy(css = "input[name='code']")
  private lateinit var code: FluentWebElement

  @FindBy(linkText = "Not received a text message?")
  private lateinit var resend: FluentWebElement

  fun getCode(): String {
    return el("[data-qa='mfa-code']").text()
  }

  fun submitCode() {
    val mfaCode = el("[data-qa='mfa-code']").text()
    this.code.fill().withText(mfaCode)
    continueButton.submit()
  }

  fun submitCode(code: String): MfaTextPage {
    this.code.fill().withText(code)
    continueButton.submit()
    return this
  }

  fun checkTextCodeIsIncorrectError(): MfaTextPage {
    checkError("Security code is incorrect. Please check your phone and try again. You will be locked out if you enter the wrong code 3 times.")
    return this
  }

  fun resendCodeLink() {
    resend.click()
  }
}

@PageUrl("/mfa-challenge")
class MfaEmailResendCodePage : AuthPage<MfaEmailResendCodePage>("HMPPS Digital Services - Resend Security Code", "Resend security code") {
  @FindBy(css = "input[type='submit']")
  private lateinit var resendSecurityCode: FluentWebElement

  fun resendCode() {
    resendSecurityCode.submit()
  }
}

@PageUrl("/mfa-resend-text")
class MfaTextResendCodePage : AuthPage<MfaTextResendCodePage>("HMPPS Digital Services - Resend Security Code", "Resend security code") {
  @FindBy(css = "input[type='submit']")
  private lateinit var resendSecurityCode: FluentWebElement

  @FindBy(css = "input[id='mfa-pref-email']")
  private lateinit var selectMfaPreferenceEmail: FluentWebElement

  @FindBy(css = "input[id='mfa-pref-text']")
  private lateinit var selectMfaPreferenceText: FluentWebElement

  fun resendCodeByEmail() {
    selectMfaPreferenceEmail.click()
    resendSecurityCode.submit()
  }

  fun resendCodeByText() {
    selectMfaPreferenceText.click()
    resendSecurityCode.submit()
  }
}


