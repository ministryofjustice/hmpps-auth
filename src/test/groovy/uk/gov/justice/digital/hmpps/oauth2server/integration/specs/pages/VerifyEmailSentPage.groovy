package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class VerifyEmailSentPage extends Page {

    static url = '/auth/verify-email'

    static at = {
        title == 'Nomis Authentication - Verify Email Sent'
        headingText == 'Verification Email Sent'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        continueButton { $("a", role: 'button') }
        errorText { $('#errors').text() }
    }

    void continueProcess() {
        assert continueButton.text() == 'Continue'
        continueButton.click()
    }
}
