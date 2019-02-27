package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.driver.CachingDriverFactory
import geb.spock.GebReportingSpec
import groovy.json.JsonSlurper
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.*

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.*

class LoginSpecification extends GebReportingSpec {

    public static final String clientBaseUrl = 'http://localhost:8081/login'

    def "The login page is present"() {
        when: 'I go to the login page'
        to LoginPage

        then: 'The Login page is displayed'
        at LoginPage
    }

    def "Attempt login with unknown user"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login without credentials"
        loginAs NOT_KNOWN, 'password1'

        then: 'My credentials are rejected and I am still on the Login page'
        at LoginErrorPage
        errorText == 'Enter a valid username and password. You will be locked out if you enter the wrong details 3 times.'
    }

    def "Attempt login without credentials"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login without credentials"
        loginAs ITAG_USER, ''

        then: 'My credentials are rejected and I am still on the Login page'
        at LoginErrorPage
        errorText == 'Enter your password'
    }

    def "Attempt login with invalid credentials"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login without credentials"
        loginAs ITAG_USER, 'wrong'

        then: 'My credentials are rejected and I am still on the Login page'
        at LoginErrorPage
        errorText == 'Enter a valid username and password. You will be locked out if you enter the wrong details 3 times.'
    }

    def "Attempt login with locked user"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login with locked user"
        loginAs LOCKED_USER, 'password123456'

        then: 'My credentials are rejected and I am still on the Login page'
        at LoginErrorPage
        errorText == "Your account is locked. If you have verified your email address then you can use 'I have forgotten my password' below."
    }

    def "Attempt login with locked auth user"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login with locked auth user"
        loginAs AUTH_LOCKED, 'password123456'

        then: 'My credentials are rejected and I am still on the Login page'
        at LoginErrorPage
        errorText == "Your account is locked. If you have verified your email address then you can use 'I have forgotten my password' below."
    }

    def "Attempt login with disabled auth user"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login with disabled auth user"
        loginAs AUTH_DISABLED, 'password123456'

        then: 'My credentials are rejected and I am still on the Login page'
        at LoginErrorPage
        errorText == 'Enter a valid username and password. You will be locked out if you enter the wrong details 3 times.'
    }

    def "Attempt login with expired user wrong password"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login with expired user"
        loginAs EXPIRED_USER, 'wrong'

        then: 'My credentials are rejected and I am still on the Login page'
        at LoginErrorPage
        errorText == 'Enter a valid username and password. You will be locked out if you enter the wrong details 3 times.'
    }

    def "Attempt login with expired user"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login with locked user"
        loginAs EXPIRED_USER, 'password123456'

        then: 'I am taken to the change password page'
        at ChangePasswordPage
    }

    def "Log in with valid credentials"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login using valid credentials"
        loginAs ITAG_USER, 'password'

        then: 'My credentials are accepted and I am shown the Home page'
        at HomePage
        principalName == 'Itag User'

        def body = parseJwt()
        body.name == 'Itag User'
        body.auth_source == 'nomis'
    }

    def "Log in with valid credentials sets jwt cookie"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login using valid credentials"
        loginAs ITAG_USER, 'password'

        then: 'My credentials are accepted and I have a cookie with my name and authentication source'
        at HomePage
        def body = parseJwt()
        body.name == 'Itag User'
        body.auth_source == 'nomis'
    }

    def "Log in with valid auth credentials"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login using valid credentials"
        loginAs AUTH_USER, 'password123456'

        then: 'My credentials are accepted and I am shown the Home page'
        at HomePage
        principalName == 'Auth Only'
    }

    def "Log in with valid auth credentials sets jwt cookie"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login using valid credentials"
        loginAs AUTH_USER, 'password123456'

        then: 'My credentials are accepted and I have a cookie with my name and authentication source'
        at HomePage
        def body = parseJwt()
        body.name == 'Auth Only'
        body.auth_source == 'auth'
    }

    def "Log in with valid credentials in lower case"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login using valid credentials"
        loginAs ITAG_USER_LOWERCASE, 'password'

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
        warning == 'Warning\nYou have been signed out'
    }

    def "I can sign in from another client"() {
        given: 'I am using SSO auth token to login'
        def state = RandomStringUtils.random(6, true, true)
        browser.go("/auth/oauth/authorize?client_id=elite2apiclient&redirect_uri=$clientBaseUrl&response_type=code&state=$state")
        at LoginPage

        when: "I login using valid credentials"
        loginAs ITAG_USER, 'password'

        then: 'I am redirected back'
        browser.getCurrentUrl() startsWith("$clientBaseUrl?code")

        and: 'state is returned'
        browser.getCurrentUrl() contains("state=$state")

        and: 'auth code is returned'
        def params = splitQuery(new URL(browser.getCurrentUrl()))
        def authCode = params.get('code')
        authCode != null

        and: 'auth code can be redeemed for access token'
        def response = getAccessToken(authCode)
        response.user_name == ITAG_USER.username
        response.auth_source == 'nomis'

        cleanup:
        CachingDriverFactory.clearCache()
    }

    def "I can sign in from another client as auth only user"() {
        given: 'I am using SSO auth token to login'
        def state = RandomStringUtils.random(6, true, true)
        browser.go("/auth/oauth/authorize?client_id=elite2apiclient&redirect_uri=$clientBaseUrl&response_type=code&state=$state")
        at LoginPage

        when: "I login using valid credentials"
        loginAs AUTH_USER, 'password123456'

        then: 'I am redirected back'
        browser.getCurrentUrl() startsWith("$clientBaseUrl?code")

        and: 'state is returned'
        browser.getCurrentUrl() contains("state=$state")

        and: 'auth code is returned'
        def params = splitQuery(new URL(browser.getCurrentUrl()))
        def authCode = params.get('code')
        authCode != null

        and: 'auth code can be redeemed for access token'
        def response = getAccessToken(authCode)
        response.user_name == AUTH_USER.username
        response.auth_source == 'auth'

        cleanup:
        CachingDriverFactory.clearCache()
    }

    private static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
        return url.query.split('&')
                .collectEntries { it.split('=').collect { URLDecoder.decode(it, 'UTF-8') } }
    }

    private Object getAccessToken(String authCode) {
        def headers = new HttpHeaders()
        headers.put('Authorization', List.of('Basic ZWxpdGUyYXBpY2xpZW50OmNsaWVudHNlY3JldA=='))
        def entity = new HttpEntity<>('', headers)
        String response = new RestTemplate().postForEntity("$baseUrl/auth/oauth/token?grant_type=authorization_code&code=$authCode&redirect_uri=$clientBaseUrl", entity, String.class).getBody()
        new JsonSlurper().parseText(response)
    }
}
