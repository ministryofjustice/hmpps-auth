package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.spock.GebReportingSpec
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.HomePage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.LoginPage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.PageNotFoundPage

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.ITAG_USER

class PageNotFoundSpecification extends GebReportingSpec {
    def "Page not found page shown when page does not exist"() {
        given: 'I am logged out'
        browser.go('/auth/logout')

        when: 'I go to a page that does not exist'
        browser.go("/auth/pagethatdoesntexist")

        and: 'I need to login'
        at LoginPage
        loginAs ITAG_USER, 'password'

        and: 'The Page not found page is displayed'
        at PageNotFoundPage

        and: "I accept the error"
        accept()

        then: 'I am shown the home page'
        at HomePage
    }
}
