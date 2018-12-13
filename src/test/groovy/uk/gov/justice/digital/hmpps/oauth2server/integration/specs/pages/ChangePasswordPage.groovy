package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class ChangePasswordPage extends Page {

    static url = '/auth/change-password'

    static at = {
        title == 'Nomis Authentication - Change Password'
        headingText == 'Change Password'
    }

    static content = {
        headingText { $('#content h1').text() }
        changePasswordButton { $("button", type: 'submit') }
        errorText { $('#errors').text() }
    }

    void changePasswordAs(String existingPassword, String newPassword, String confirmNewPassword) {

        $('form').password = existingPassword
        $('form').newPassword = newPassword
        $('form').confirmPassword = confirmNewPassword

        assert changePasswordButton.text() == 'Change Password'

        changePasswordButton.click()
    }
}
