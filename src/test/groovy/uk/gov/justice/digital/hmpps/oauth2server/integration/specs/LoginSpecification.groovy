package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.spock.GebReportingSpec
import org.apache.commons.lang3.RandomStringUtils
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.HomePage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.LoginPage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.UserHomePage

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.ITAG_USER

class LoginSpecification extends GebReportingSpec {

    public static final String clientBaseUrl = 'http://localhost:8081/login'

    def "The login page is present"() {
        when: 'I go to the login page'
        to LoginPage

        then: 'The Login page is displayed'
        at LoginPage
    }

    def "Log in with valid credentials"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login using valid credentials"
        loginAs ITAG_USER, 'password'

        then: 'My credentials are accepted and I am shown the Home page'
        at HomePage
    }

    def "View User Home Page once logged in"() {
        given: 'I am trying to access the user home page'
        browser.go('/auth/ui')
        at LoginPage

        when: "I login using valid credentials"
        loginAs ITAG_USER, 'password'

        then: 'My credentials are accepted and I am shown the User Home page'
        at UserHomePage
    }

    def "I can logout once logged in"() {
        given: 'I am logged in'
        to LoginPage
        loginAs ITAG_USER, 'password'
        at HomePage

        when: "I logout"
        logout()

        then: 'I am logged out'
        at LoginPage
        errorText == 'You have been logged out'
    }

    def "I can sign in from another client"() {
        given: 'I am using SSO auth token to login'
        def state = RandomStringUtils.random(6, true, true)
        browser.go('/auth/oauth/authorize?client_id=elite2apiclient&redirect_uri=' + clientBaseUrl + '&response_type=code&state=' + state)
        at LoginPage

        when: "I login using valid credentials"
        loginAs ITAG_USER, 'password'

        then: 'I am redirected back'
        browser.getCurrentUrl() startsWith(clientBaseUrl + '?code')

        and: 'state is returned'
        browser.getCurrentUrl() contains('state='+state)

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
