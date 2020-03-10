package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class AccountDetailsPage extends Page {

    static url = "/auth"

    static at = {
        title == 'HMPPS Digital Services - Account Details'
        headingText == 'Account details'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        principalName { $('#principal-name').text() }
    }
}
