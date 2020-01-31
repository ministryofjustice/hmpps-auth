package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class ChangeExpiredPasswordPage extends Page {

    static url = '/auth/change-password'

    static at = {
        title == 'HMPPS Digital Services - Change Password'
        headingText == 'Your password has expired'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        changePasswordButton { $("input", type: 'submit') }
    }

    void changePasswordAs(String newPassword, String confirmNewPassword) {

        $('form').newPassword = newPassword
        $('form').confirmPassword = confirmNewPassword

        assert changePasswordButton.value() == 'Save new password'

        changePasswordButton.click()
    }
}
