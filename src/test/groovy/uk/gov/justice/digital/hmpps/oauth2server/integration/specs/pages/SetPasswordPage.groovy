package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class SetPasswordPage extends Page {

    static url = '/auth/set-password'

    static at = {
        title == 'HMPPS Digital Services - Set Password'
        headingText == 'Enter a new password'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        setPasswordButton { $("input", type: 'submit') }
    }

    void setPasswordAs(String newPassword, String confirmNewPassword) {

        $('form').newPassword = newPassword
        $('form').confirmPassword = confirmNewPassword

        assert setPasswordButton.value() == 'Save new password'

        setPasswordButton.click()
    }
}
