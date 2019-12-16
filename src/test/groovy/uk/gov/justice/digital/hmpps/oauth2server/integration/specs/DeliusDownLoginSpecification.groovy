package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.HomePage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.LoginErrorPage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.LoginPage

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.AUTH_USER
import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.DELIUS_USER
import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.ITAG_USER

class DeliusDownLoginSpecification extends BrowserReportingSpec {

    def "Delius unavailable shows in error"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login using valid Delius credentials"
        loginAs DELIUS_USER, 'password'

        then: 'My credentials are rejected and I am still on the Login page'
        at LoginErrorPage
        errorText == "Enter a valid username and password. You will be locked out if you enter the wrong details 3 times." +
                "\nDelius is experiencing issues. Please try later if you are attempting to login using your Delius credentials."
    }

    def "Delius unavailable doesn't prevent logging in as auth user"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login using valid Auth credentials"
        loginAs AUTH_USER, 'password123456'

        then: 'My credentials are accepted and I am shown the Home page'
        at HomePage
        principalName == 'Auth Only'
    }

    def "Delius unavailable doesn't prevent logging in as nomis user"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login using valid Nomis credentials"
        loginAs ITAG_USER, 'password'

        then: 'My credentials are accepted and I am shown the Home page'
        at HomePage
        principalName == 'Itag User'
    }
}
