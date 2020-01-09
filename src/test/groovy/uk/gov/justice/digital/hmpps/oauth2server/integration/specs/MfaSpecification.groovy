package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.driver.CachingDriverFactory
import geb.spock.GebReportingSpec
import groovy.json.JsonSlurper
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.*

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.AUTH_MFA_USER

class MfaSpecification extends GebReportingSpec {
  public static final String clientBaseUrl = 'http://localhost:8081/login'

  def "Attempt MFA challenge with invalid token"() {
    when: 'I go to the MFA page'
    browser.go('/auth/mfa-challenge?token=invalidtoken')

    then: 'I am taken to the Login page'
    at LoginErrorPage
    errorText == 'Your authentication request failed. You will be locked out if you enter the wrong details 3 times.'
  }

  def "Attempt MFA challenge with expired token"() {
    when: 'I go to the MFA page'
    browser.go('/auth/mfa-challenge?token=mfa_expired')

    then: 'I am taken to the Login page'
    at LoginErrorPage
    errorText == 'Your authentication request has timed out. Enter your username and password to start again.'
  }

  def "Login as user with MFA enabled"() {
    given: 'I try to login with a user with MFA enabled'
    to LoginPage

    when: 'I login'
    loginAs AUTH_MFA_USER, 'password123456'

    then: 'I am redirected to the mfa page'
    at MfaPage

    when: "I don't enter a code"
    submitCode " "

    then: "I am shown an error message"
    at MfaErrorPage
    errorText == 'Enter the code received in the email'

    when: "I enter my MFA credentials"
    submitCode '123456'

    then: 'My credentials are accepted and I am shown the Home page'
    at HomePage
    principalName == 'Mfa User'

    def body = parseJwt()
    body.name == 'Mfa User'
  }

  def "MFA code is required"() {
    when: 'I try to login with a user with MFA enabled'
    browser.go("/auth/mfa-challenge?token=mfa_token")

    then: 'I am taken to the mfa page'
    at MfaPage

    when: "I don't enter a code"
    submitCode " "

    then: "I am shown an error message"
    at MfaErrorPage
    errorText == 'Enter the code received in the email'
  }

  def "I would like the MFA code to be resent"() {
    when: 'I try to login with a user with MFA enabled'
    browser.go("/auth/mfa-challenge?token=mfa_token")

    then: 'I am taken to the mfa page'
    at MfaPage

    when: "I select that I don't have a code"
    resendMfa()

    then: "I am taken to the resend MFA page"
    at MfaResendPage

    when: "I continue"
    resendCode()

    then: "I am now back at the MFA code page"
    at MfaPage
  }

  def "I can sign in from another client with MFA enabled"() {
    given: 'I am using SSO auth token to login'
    def state = RandomStringUtils.random(6, true, true)
    browser.go("/auth/oauth/authorize?client_id=elite2apiclient&redirect_uri=$clientBaseUrl&response_type=code&state=$state")
    at LoginPage

    when: "I login using valid credentials"
    loginAs AUTH_MFA_USER, 'password123456'

    then: 'I am redirected to the mfa page'
    at MfaPage

    when: "I enter my MFA credentials"
    submitCode '123456'

    then: 'I am redirected back'
    browser.getCurrentUrl() startsWith("$clientBaseUrl?code")

    and: 'state is returned'
    browser.getCurrentUrl() contains("state=$state")

    and: 'auth code is returned'
    def params = LoginSpecification.splitQuery(new URL(browser.getCurrentUrl()))
    def authCode = params.get('code')
    authCode != null

    and: 'auth code can be redeemed for access token'
    def response = getAccessToken(authCode)
    response.user_name == AUTH_MFA_USER.username
    response.auth_source == 'auth'

    cleanup:
    CachingDriverFactory.clearCache()
  }

  Object getAccessToken(String authCode) {
    def headers = new HttpHeaders()
    headers.put('Authorization', List.of('Basic ZWxpdGUyYXBpY2xpZW50OmNsaWVudHNlY3JldA=='))
    def entity = new HttpEntity<>('', headers)
    String response = new RestTemplate().postForEntity("$baseUrl/auth/oauth/token?grant_type=authorization_code&code=$authCode&redirect_uri=$clientBaseUrl", entity, String.class).getBody()
    new JsonSlurper().parseText(response)
  }
}
