package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class ChangePasswordPage extends Page {

    static url = '/auth/change-password'

    static at = {
        title == 'Nomis Authentication - Change Password'
        headingText == 'Your password has expired'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        changePasswordButton { $("input", type: 'submit') }
        errorText { $('#errorcurrent').text() }
    }

    void changePasswordAs(String existingPassword, String newPassword, String confirmNewPassword) {

        $('form').password = existingPassword
        $('form').newPassword = newPassword
        $('form').confirmPassword = confirmNewPassword

        assert changePasswordButton.value() == 'Save new password'

        changePasswordButton.click()
    }
}
