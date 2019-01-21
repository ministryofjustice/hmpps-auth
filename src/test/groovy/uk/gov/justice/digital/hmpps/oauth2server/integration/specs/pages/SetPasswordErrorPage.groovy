package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

class SetPasswordErrorPage extends SetPasswordPage {

    static at = {
        title == 'Error: HMPPS Digital Services - Set Password'
    }

    static content = {
        errorText { $('#error-detail').text() }
        errorNewText { $('#errornew').text() }
        errorConfirmText { $('#errorconfirm').text() }
    }
}
