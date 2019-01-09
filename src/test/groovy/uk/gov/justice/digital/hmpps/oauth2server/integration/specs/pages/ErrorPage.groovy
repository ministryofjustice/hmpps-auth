package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class ErrorPage extends Page {

    static url = '/auth/error'

    static at = {
        title == 'HMPPS Digital Services - Error'
        headingText == 'Sorry, there is a problem with the service'
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
