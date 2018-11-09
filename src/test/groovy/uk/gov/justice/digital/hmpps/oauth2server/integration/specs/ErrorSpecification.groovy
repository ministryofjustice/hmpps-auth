package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.spock.GebReportingSpec
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.ErrorPage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.LoginPage

class ErrorSpecification extends GebReportingSpec {

    def "Error page shown when error occurs"() {
        given: 'I go to the login page'
        to LoginPage

        when: 'I create an application error'
        browser.go('/auth/login;someerror')

        then: 'The Error page is displayed'
        at ErrorPage
    }

    def "Accept error"() {
        given: 'I am on the error page'
        to ErrorPage

        when: "I accept the error"
        accept()

        then: 'I am shown the login page'
        at LoginPage
    }
}
