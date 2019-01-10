package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

class ChangePasswordErrorPage extends ChangePasswordPage {

    static at = {
        title == 'Error - HMPPS Digital Services - Change Password'
    }

    static content = {
        errorText { $('#error-detail').text() }
        errorCurrentText { $('#errorcurrent').text() }
        errorNewText { $('#errornew').text() }
        errorConfirmText { $('#errorconfirm').text() }
    }
}
