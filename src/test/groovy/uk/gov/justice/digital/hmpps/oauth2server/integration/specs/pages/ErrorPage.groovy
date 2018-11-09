package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class ErrorPage extends Page {

    static url = '/auth/error'

    static at = {
        title == 'Nomis Authentication - Error'
        headingText == 'Sorry, there is a problem with the service'
    }

    static content = {
        headingText { $('#content h1').text() }
        continueButton { $('#login') }
    }

    void accept() {
        assert continueButton.text() == 'OK, continue'

        continueButton.click()
    }
}
