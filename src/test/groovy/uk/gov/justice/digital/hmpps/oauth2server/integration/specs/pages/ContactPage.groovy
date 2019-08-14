package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class ContactPage extends Page {

    static url = '/auth/contact'

    static at = {
        title == 'HMPPS Digital Services - Contact us'
        headingText == 'Contact us'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        backLink { $("a", 'data-qa': "back-link") }
        hdcSection { $('#HDC') }
        nomisSection { $('#NOMIS') }
    }

    void back() {
        backLink.click()
    }
}
