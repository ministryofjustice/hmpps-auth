package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class UserHomePage extends Page {

    static url = "/auth/ui"

    static at = {
        headingText == 'OAuth Server Administration Dashboard'
    }

    static content = {
        headingText { $('#content h1').text() }
    }
}
