package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class ClientSummaryPage extends Page {

    static url = "/auth/ui"

    static at = {
        browser.currentUrl.contains(url)
        headingText == 'OAuth Server Administration Dashboard'
    }

    static content = {
        headingText { $('h1').text() }
        table { $('table') }
        rows { $('table tbody tr') }
        editButton { $('#edit-apireporting') }
    }
}
