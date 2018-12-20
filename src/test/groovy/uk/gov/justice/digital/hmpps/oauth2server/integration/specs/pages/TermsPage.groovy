package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class TermsPage extends Page {

    static url = '/auth/terms'

    static at = {
        title == 'Nomis Authentication - Terms and conditions'
        headingText == 'Terms and conditions'
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
