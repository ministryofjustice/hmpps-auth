package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.spock.GebReportingSpec
import org.apache.commons.lang3.RandomStringUtils
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.ChangePasswordPage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.LoginPage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.UserHomePage

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.CA_USER

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
        to username: CA_USER.username, ChangePasswordPage

        when: "I change password without credentials"
        changePasswordAs '', '', ''

        then: 'My credentials are rejected and I am still on the Change Password page'
        at ChangePasswordPage
        errorText == 'Enter your current password'
    }

    // this test changes CA_USER password
    def "Change password with valid credentials"() {
        given: 'I am on the Change Password page'
        to username: CA_USER.username, ChangePasswordPage

        when: "I change password using valid credentials"
        changePasswordAs 'password123456', 'password1', 'password1'

        then: 'My credentials are accepted and I am shown the Home page'
        at UserHomePage
    }

    // this test changes it back again so requires previous test to succeed
    def "I can sign in from another client"() {
        given: 'I am using SSO auth token to change password'
        def state = RandomStringUtils.random(6, true, true)
        browser.go('/auth/oauth/authorize?client_id=elite2apiclient&redirect_uri=' + clientBaseUrl + '&response_type=code&state=' + state)
        at LoginPage
        to username: CA_USER.username, ChangePasswordPage

        when: "I change password using valid credentials"
        changePasswordAs 'password1', 'password123456', 'password123456'

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
