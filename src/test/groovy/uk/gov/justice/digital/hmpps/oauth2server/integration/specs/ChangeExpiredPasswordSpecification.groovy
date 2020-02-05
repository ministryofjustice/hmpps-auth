package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.spock.GebReportingSpec
import org.apache.commons.lang3.RandomStringUtils
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.*

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.*

class ChangeExpiredPasswordSpecification extends GebReportingSpec {
    public static final String clientBaseUrl = 'http://localhost:8081/login'

    def "Attempt change password without credentials"() {
        given: 'I try to login with an expired user'
        to LoginPage
        loginAs EXPIRED_TEST_USER, 'password123456'

        and: 'I am redirected to the change password page'
        at ChangeExpiredPasswordPage

        when: "I change password without credentials"
        changePasswordAs EXPIRED_TEST_USER, '', ''

        then: 'My credentials are rejected and I am still on the Change Password page'
        at ChangeExpiredPasswordErrorPage
        errorText == 'Enter your new password\nEnter your new password again'
        errorNewText == 'Enter your new password'
        errorConfirmText == 'Enter your new password again'
    }

    def "Attempt change password with invalid new password"() {
        given: 'I try to login with an expired user'
        to LoginPage
        loginAs EXPIRED_TEST_USER, 'password123456'

        and: 'I am redirected to the change password page'
        at ChangeExpiredPasswordPage

        when: "I change password without credentials"
        changePasswordAs EXPIRED_TEST_USER, 'somepass', 'd'

        then: 'My credentials are rejected and I am still on the Change Password page'
        at ChangeExpiredPasswordErrorPage
        errorText == 'Your password must have both letters and numbers\n' +
                'Your password must have at least 9 characters\n' +
                'Your passwords do not match. Enter matching passwords.'
        errorNewText == 'Your password must have both letters and numbers\n' +
                'Your password must have at least 9 characters'
        errorConfirmText == 'Your passwords do not match. Enter matching passwords.'
    }

    def "Attempt change password with password on blacklist"() {
        given: 'I try to login with an expired user'
        to LoginPage
        loginAs EXPIRED_TEST_USER, 'password123456'

        and: 'I am redirected to the change password page'
        at ChangeExpiredPasswordPage

        when: "I change password without credentials"
        changePasswordAs EXPIRED_TEST_USER, 'iLoveYou2', 'iLoveYou2'

        then: 'My credentials are rejected and I am still on the Change Password page'
        at ChangeExpiredPasswordErrorPage
        errorText == 'Your password is commonly used and may not be secure'
        errorNewText == 'Your password is commonly used and may not be secure'
    }

    // this test changes EXPIRED_TEST2_USER password
    def "Change password with valid credentials"() {
        given: 'I try to login with an expired user'
        to LoginPage
        loginAs EXPIRED_TEST2_USER, 'password123456'

        and: 'I am redirected to the change password page'
        at ChangeExpiredPasswordPage

        when: "I change password using valid credentials"
        changePasswordAs EXPIRED_TEST2_USER, 'helloworld2', 'helloworld2'

        and: 'My credentials are accepted and I am shown the Home page'
        at HomePage

        and: 'I can login with my new credentials'
        logout()
        at LoginPage
        loginAs EXPIRED_TEST2_USER, 'helloworld2'

      then: 'I am logged in'
      at HomePage
    }

  // this test changes AUTH_MFA_EXPIRED password
  def "Change password for auth user with MFA enabled"() {
    given: 'I try to login with an expired user'
    to LoginPage

    when: 'I login'
    loginAs AUTH_MFA_EXPIRED, 'password123456'

    then: 'I am redirected to the change password page'
    at ChangeExpiredPasswordPage

    when: "I change password using valid credentials"
    changePasswordAs AUTH_MFA_EXPIRED, 'helloworld2', 'helloworld2'

    then: 'I am redirected to the mfa page'
    at MfaPage

    when: "I enter my MFA credentials"
    submitCode mfaCode

    then: 'I am now at the home page'
    at HomePage

    when: 'I can login with my new credentials'
    logout()
    at LoginPage
    loginAs AUTH_MFA_EXPIRED, 'helloworld2'

    then: 'I am redirected to the mfa page'
    at MfaPage

    when: "I enter my MFA credentials"
    submitCode mfaCode

    then: 'I am logged in'
        at HomePage
    }

    // this test changes AUTH_EXPIRED password
    def "Change password for auth user with valid credentials"() {
        given: 'I try to login with an expired user'
        to LoginPage
        loginAs AUTH_EXPIRED, 'password123456'

        and: 'I am redirected to the change password page'
        at ChangeExpiredPasswordPage

        when: "I change password using valid credentials"
        changePasswordAs AUTH_EXPIRED, 'helloworld2', 'helloworld2'

        and: 'My credentials are accepted and I am shown the Home page'
        at HomePage

        and: 'I can login with my new credentials'
        logout()
        at LoginPage
        loginAs AUTH_EXPIRED, 'helloworld2'

        then: 'I am logged in'
        at HomePage
    }

    // this test changes EXPIRED_TEST3_USER password
    def "I can sign in from another client"() {
        given: 'I am using SSO auth token to change password'
        def state = RandomStringUtils.random(6, true, true)
        browser.go('/auth/oauth/authorize?client_id=elite2apiclient&redirect_uri=' + clientBaseUrl + '&response_type=code&state=' + state)
        at LoginPage
        loginAs EXPIRED_TEST3_USER, 'password123456'

        and: 'I am redirected to the change password page'
        at ChangeExpiredPasswordPage

        when: "I change password using valid credentials"
        changePasswordAs EXPIRED_TEST3_USER, 'dodgypass1', 'dodgypass1'

        then: 'I am redirected back'
        browser.getCurrentUrl() startsWith(clientBaseUrl + '?code')

        and: 'state is returned'
        browser.getCurrentUrl() contains('state=' + state)

        and: 'auth code is returned'
        def params = splitQuery(new URL(browser.getCurrentUrl()))
        def authCode = params.get('code')
        authCode != null
    }

    private static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
        return url.query.split('&')
                .collectEntries { it.split('=').collect { URLDecoder.decode(it, 'UTF-8') } }
    }
}
