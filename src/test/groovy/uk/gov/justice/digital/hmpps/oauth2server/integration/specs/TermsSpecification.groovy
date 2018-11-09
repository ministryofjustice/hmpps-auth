package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.spock.GebReportingSpec
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.LoginPage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.TermsPage

class TermsSpecification extends GebReportingSpec {

    def "The terms link is present"() {
        given: 'I go to the login page'
        to LoginPage

        when: 'I select the terms link'
        viewTerms()

        then: 'The Terms page is displayed'
        at TermsPage
    }

    def "Accept terms and conditions"() {
        given: 'I am on the terms page'
        to TermsPage

        when: "I accept the terms and conditions"
        accept()

        then: 'I am shown the login page'
        at LoginPage
    }
}
