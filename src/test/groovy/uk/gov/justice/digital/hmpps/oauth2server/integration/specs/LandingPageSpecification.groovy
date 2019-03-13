package uk.gov.justice.digital.hmpps.oauth2server.integration.specs


import geb.spock.GebReportingSpec
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.HomePage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.LoginPage

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.CA_USER
import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.ITAG_USER

class LandingPageSpecification extends GebReportingSpec {

    def "Log in with licences user"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login using valid credentials"
        loginAs CA_USER, 'password123456'

        then: 'My credentials are accepted and I am shown NOMIS and HDC links on the home page'
        at HomePage
        $('a#HDC').text() == 'Home Detention Curfew'
        $('a#NOMIS').text() == 'New NOMIS'
    }

    def "Log in with nomis user"() {
        given: 'I am on the Login page'
        to LoginPage

        when: "I login using valid credentials"
        loginAs ITAG_USER, 'password'

        then: 'My credentials are accepted and I am shown NOMIS but not HDC links on the home page'
        at HomePage
        $('a#HDC').text() == null
        $('a#NOMIS').text() == 'New NOMIS'
    }
}
