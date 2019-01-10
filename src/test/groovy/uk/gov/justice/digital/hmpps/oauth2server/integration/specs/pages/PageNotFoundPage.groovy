package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class PageNotFoundPage extends Page {

    static url = '/auth/error/404'

    static at = {
        title == 'Error - HMPPS Digital Services - Page Not Found'
        headingText == 'Page not found'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        continueButton { $('#continue') }
    }

    void accept() {
        assert continueButton.text() == 'OK, continue'

        continueButton.click()
    }
}
