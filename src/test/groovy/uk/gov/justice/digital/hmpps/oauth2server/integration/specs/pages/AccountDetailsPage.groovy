package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class AccountDetailsPage extends Page {

    static url = "/auth"

    static at = {
        title == 'HMPPS Digital Services - Your Account Details'
        headingText == 'Your account details'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        principalName { $('#principal-name').text() }
    }
}
