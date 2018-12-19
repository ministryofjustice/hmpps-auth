package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class VerifyEmailPage extends Page {

    static url = '/auth/verify-email'

    static at = {
        title == 'Nomis Authentication - Verify Email'
        headingText == 'Email Address Verification'
    }

    static content = {
        headingText { $('#content h1').text() }
        verifyEmailButton { $("input", type: 'submit') }
        emailInput { $('#email') }
        cancelButton { $("a", id: 'cancel') }
        errorText { $('#errors').text() }
    }

    void verifyEmailAs(String email) {
        emailInput = email
        assert verifyEmailButton.value() == 'Verify email'
        verifyEmailButton.click()
    }

    void verifyExistingEmailAs(String email) {
        assert emailInput.value() == email
        assert verifyEmailButton.value() == 'Verify email'
        verifyEmailButton.click()
    }

    void cancel() {
        $('form').email = email
        assert cancelButton.text() == 'Cancel'
        cancelButton.click()
    }
}
