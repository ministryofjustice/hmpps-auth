package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class SetPasswordPage extends Page {

    static url = '/auth/initial-password'

    static at = {
        title == 'HMPPS Digital Services - Create a password'
        headingText == 'Create a password'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        setPasswordButton { $("input", type: 'submit') }
        usernameInput(required: false) { $("input", name: 'username') }
    }

    void setPasswordAs(String newPassword, String confirmNewPassword) {
        // ensure we don't have a non visible field containing username
        assert usernameInput.empty

        $('form').newPassword = newPassword
        $('form').confirmPassword = confirmNewPassword

        assert setPasswordButton.value() == 'Save password'

        setPasswordButton.click()
    }
}
