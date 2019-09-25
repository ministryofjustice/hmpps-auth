package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.spock.GebReportingSpec
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.ContactPage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.LoginPage

class ContactSpecification extends GebReportingSpec {

    def "The contact us link is present"() {
        given: 'I go to the login page'
        to LoginPage

        when: 'I select the contact us link'
        viewContact()

        then: 'The Contact us page is displayed'
        at ContactPage
    }

    def "View contact details"() {
        given: 'I am on the contact us page'
        to ContactPage

        when: "The hdc contact details are displayed"
        then:
        hdcSection.$('h3').text() == 'Home Detention Curfew'
        hdcSection.$('a').text() == 'hdcdigitalservice@digital.justice.gov.uk'
        hdcSection.$('a').@href == 'mailto:hdcdigitalservice@digital.justice.gov.uk'

        and: "The nomis contact details are displayed"
        nomisSection.$('h3').text() == 'Digital Prison Service'
        nomisSection.$('a').text() == 'feedback@digital.justice.gov.uk'
        nomisSection.$('a').@href == 'mailto:feedback@digital.justice.gov.uk'

        and: "The moic contact details are displayed"
        moicSection.$('h3').text() == 'Allocate a POM Service'
        moicSection.$('p').text() == 'Contact us using the online form'
        moicSection.$('a').text() == 'online form'
        moicSection.$('a').@href == 'https://moic.service.justice.gov.uk/help'
    }

    def "Return to login page"() {
        given: 'I am on the contact us page'
        to ContactPage

        when: "I view the contact us links"
        back()

        then: 'I am shown the login page'
        at LoginPage
    }
}
