package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

class SetPasswordSelectErrorPage extends SetPasswordSelectPage {

    static at = {
        title == 'Error: HMPPS Digital Services - Set Password Select'
    }

    static content = {
        errorText { $('#error-detail').text() }
        errorNewText { $('#errornew').text() }
        errorConfirmText { $('#errorconfirm').text() }
    }
}
