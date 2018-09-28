package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class HomePage extends Page {

    static url = "/auth"

    static at = {
        headingText == 'Nomis Auth Server'
    }

    static content = {
        headingText { $('h1').text() }
    }
}
