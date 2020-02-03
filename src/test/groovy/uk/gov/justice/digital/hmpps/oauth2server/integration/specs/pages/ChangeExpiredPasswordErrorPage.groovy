package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

class ChangeExpiredPasswordErrorPage extends ChangeExpiredPasswordPage {

    static at = {
        title == 'Error: HMPPS Digital Services - Change Password'
    }

    static content = {
        errorText { $('#error-detail').text() }
        errorNewText { $('#errornew').text() }
        errorConfirmText { $('#errorconfirm').text() }
    }
}
