package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount


class TermsPage extends Page {

    static url = '/auth/terms'

    static at = {
        title == 'Nomis Authentication - Terms and conditions'
        headingText == 'Terms and conditions'
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
