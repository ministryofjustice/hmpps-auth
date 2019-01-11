package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class VerifyEmailPage extends Page {

    static url = '/auth/verify-email'

    static at = {
        title == 'HMPPS Digital Services - Verify Email'
        headingText == 'Verify your email address'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        verifyEmailButton { $("input", type: 'submit') }
        emailInput { $('#email') }
        cancelButton { $("a", id: 'cancel') }
        errorText { $('#errors').text() }
    }

    void verifyEmailAs(String email) {
        emailInput = email
        assert verifyEmailButton.value() == 'Save'
        verifyEmailButton.click()
    }

    void verifyExistingEmailAs(String email) {
        assert emailInput.value() == email
        assert verifyEmailButton.value() == 'Save'
        verifyEmailButton.click()
    }

    void selectExistingEmailAs(String email) {
        $('#verifyEmailForm').candidate = email
        assert $('#verifyEmailForm').candidate == email
        assert verifyEmailButton.value() == 'Save'
        verifyEmailButton.click()
    }

    void cancel() {
        $('form').email = email
        assert cancelButton.text() == 'Skip for now'
        cancelButton.click()
    }
}
