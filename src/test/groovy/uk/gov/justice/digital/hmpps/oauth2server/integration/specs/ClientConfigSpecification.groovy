package uk.gov.justice.digital.hmpps.oauth2server.integration.specs


import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.ClientMaintenancePage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.ClientSummaryPage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.LoginPage

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.AUTH_ADM
import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.ITAG_USER_ADM

class ClientConfigSpecification extends BrowserReportingSpec {

    def "View Client Dashboard once logged in"() {
        given: 'I am trying to access the client dashboard page'
        browser.go('/auth/logout')
        browser.go('/auth/ui')
        at LoginPage

        when: "I login using admin valid credentials"
        loginAs ITAG_USER_ADM, 'password123456'

        then: 'My credentials are accepted and I am shown the Client Dashboard'
        at ClientSummaryPage
        rows.size() > 10
        table.find("tr", 1).find("td", 0).text() == 'apireporting'
        table.find("tr", 1).find("td", 4).text() == '3600'
        table.find("tr", 1).find("td", 5).text() == 'Edit'
    }

    def "I can edit a client credential"() {
        given: 'I am on the client dashboard page'
        browser.go('/auth/logout')
        browser.go('/auth/ui')
        at LoginPage
        loginAs ITAG_USER_ADM, 'password123456'
        at ClientSummaryPage

        when: "I edit a client"
        editButton.click()

        then: 'I am show the maintenance screen'
        at ClientMaintenancePage
    }

    def "I can edit a client credential as auth user"() {
        given: 'I am on the client dashboard page'
        browser.go('/auth/logout')
        browser.go('/auth/ui')
        at LoginPage
        loginAs AUTH_ADM, 'password123456'
        at ClientSummaryPage

        when: "I edit a client"
        editButton.click()

        then: 'I am show the maintenance screen'
        at ClientMaintenancePage
    }
}
