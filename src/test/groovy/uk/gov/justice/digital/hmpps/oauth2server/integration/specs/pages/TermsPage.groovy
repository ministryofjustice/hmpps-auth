package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class TermsPage extends Page {

    static url = '/auth/terms'

    static at = {
        title == 'HMPPS Digital Services - Terms and conditions'
        headingText == 'Terms and conditions'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        backLink { $("a", 'data-qa': "back-link") }
    }

    void accept() {
        backLink.click()
    }
}
