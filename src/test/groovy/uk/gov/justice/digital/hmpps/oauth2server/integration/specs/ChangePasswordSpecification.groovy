package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.spock.GebReportingSpec
import org.apache.commons.lang3.RandomStringUtils
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.ChangePasswordErrorPage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.ChangePasswordPage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.HomePage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.LoginPage

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.*

class ChangePasswordSpecification extends GebReportingSpec {

    public static final String clientBaseUrl = 'http://localhost:8081/login'

    def "The change password page is present"() {
        when: 'I go to the change password page'
        to username: 'bob', ChangePasswordPage

        then: 'The Change Password page is displayed'
        at ChangePasswordPage
    }

    def "Attempt change password without credentials"() {
        given: 'I am on the Change Password page'
        to username: EXPIRED_TEST_USER.username, ChangePasswordPage

        when: "I change password without credentials"
        changePasswordAs '', '', ''

        then: 'My credentials are rejected and I am still on the Change Password page'
        at ChangePasswordErrorPage
        errorText == 'Enter your current password\nEnter your new password\nEnter your new password again'
        errorCurrentText == 'Enter your current password'
        errorNewText == 'Enter your new password'
        errorConfirmText == 'Enter your new password again'
    }

    def "Attempt change password with invalid new password"() {
        given: 'I am on the Change Password page'
        to username: EXPIRED_TEST_USER.username, ChangePasswordPage

        when: "I change password without credentials"
        changePasswordAs 'password123456', 'somepass', 'd'

        then: 'My credentials are rejected and I am still on the Change Password page'
        at ChangePasswordErrorPage
        errorText == 'Your password must have both letters and numbers\n' +
                'Your password must have at least 9 characters\n' +
                'Your passwords do not match. Enter matching passwords.';
        errorNewText == 'Your password must have both letters and numbers\n' +
                'Your password must have at least 9 characters'
        errorConfirmText == 'Your passwords do not match. Enter matching passwords.'
    }

    // this test changes EXPIRED_TEST2_USER password
    def "Change password with valid credentials"() {
        given: 'I am on the Change Password page'
        to username: EXPIRED_TEST2_USER.username, ChangePasswordPage

        when: "I change password using valid credentials"
        changePasswordAs 'password123456', 'password1', 'password1'

        then: 'My credentials are accepted and I am shown the Home page'
        at HomePage
    }

    // this test changes EXPIRED_TEST3_USER password
    def "I can sign in from another client"() {
        given: 'I am using SSO auth token to change password'
        def state = RandomStringUtils.random(6, true, true)
        browser.go('/auth/oauth/authorize?client_id=elite2apiclient&redirect_uri=' + clientBaseUrl + '&response_type=code&state=' + state)
        at LoginPage
        to username: EXPIRED_TEST3_USER.username, ChangePasswordPage

        when: "I change password using valid credentials"
        changePasswordAs 'password123456', 'password1', 'password1'

        then: 'I am redirected back'
        browser.getCurrentUrl() startsWith(clientBaseUrl + '?code')

        and: 'state is returned'
        browser.getCurrentUrl() contains('state=' + state)

        and: 'auth code is returned'
        def params = splitQuery(new URL(browser.getCurrentUrl()))
        def authCode = params.get('code');
        authCode != null
    }

    private static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
        return url.query.split('&')
                .collectEntries { it.split('=').collect { URLDecoder.decode(it, 'UTF-8') } };
    }
}
