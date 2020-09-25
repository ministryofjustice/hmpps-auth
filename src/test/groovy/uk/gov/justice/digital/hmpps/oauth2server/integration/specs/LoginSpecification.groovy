package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import groovy.json.JsonSlurper
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.ChangeExpiredPasswordPage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.LoginErrorPage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.LoginPage

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.*

class LoginSpecification extends DeliusIntegrationSpec {

    public static final String clientBaseUrl = 'http://localhost:8081/login'

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

    def "Delius user gets locked after 3 invalid login attempts"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login with wrong password as a delius user"
        loginAs 'DELIUS_LOCKED_IN_AUTH', 'wrongpassword'

        then: 'My credentials are rejected and I am still on the Login page'
        at LoginErrorPage
        errorText == "Enter a valid username and password. You will be locked out if you enter the wrong details 3 times."

        when: "I login with wrong password as a delius user"
        loginAs 'DELIUS_LOCKED_IN_AUTH', 'wrongpassword'

        then: 'My credentials are rejected and I am still on the Login page'
        at LoginErrorPage
        errorText == "Enter a valid username and password. You will be locked out if you enter the wrong details 3 times."

        when: "I login with wrong password as a delius user"
        loginAs 'DELIUS_LOCKED_IN_AUTH', 'wrongpassword'

        then: 'My credentials are rejected and I am still on the Login page'
        at LoginErrorPage
        errorText == "Your account is locked. If you have verified your email address then you can use 'I have forgotten my password' below."
    }

    def "Attempt login with disabled delius user"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login with disabled delius user"
        loginAs DELIUS_LOCKED, 'password123456'

        then: 'My credentials are rejected and I am still on the Login page'
        at LoginErrorPage
        errorText == "Enter a valid username and password. You will be locked out if you enter the wrong details 3 times."
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
        at ChangeExpiredPasswordPage
    }

    def "Attempt login when delius connections time out"() {
      // dev-config defines timeout to delius as 2 seconds.  The DELIUS_ERROR_TIMEOUT user has a success mapping,
      // but with fixed delay of 2 seconds which should therefore cause the timeout.
      // If timeout not working then login will succeed instead and test will fail.
        given: 'I am on the Login page'
        to LoginPage

        when: "I login with delius user that times out"
        loginAs DELIUS_TIMEOUT, 'password123456'

        then: 'My credentials are rejected and I am still on the Login page'
        at LoginErrorPage
        errorText == "Enter a valid username and password. You will be locked out if you enter the wrong details 3 times." +
                "\nDelius is experiencing issues. Please try later if you are attempting to login using your Delius credentials."
    }

    def "Attempt login with Delius unavailable (gateway returns 503)"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I attempt to login and receive a server error"
        loginAs DELIUS_SERVER_ERROR, 'password'

        then: 'My credentials are rejected and I am still on the Login page'
        at LoginErrorPage
        errorText == "Enter a valid username and password. You will be locked out if you enter the wrong details 3 times." +
                "\nDelius is experiencing issues. Please try later if you are attempting to login using your Delius credentials."
    }

    static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
        return url.query.split('&')
                .collectEntries { it.split('=').collect { URLDecoder.decode(it, 'UTF-8') } }
    }

    Object getAccessToken(String authCode) {
        def headers = new HttpHeaders()
        headers.put('Authorization', List.of('Basic ZWxpdGUyYXBpY2xpZW50OmNsaWVudHNlY3JldA=='))
        def entity = new HttpEntity<>('', headers)
        String response = new RestTemplate().postForEntity("$baseUrl/auth/oauth/token?grant_type=authorization_code&code=$authCode&redirect_uri=$clientBaseUrl", entity, String.class).getBody()
        new JsonSlurper().parseText(response)
    }
}
