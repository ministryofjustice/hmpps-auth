package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

class ChangeEmailErrorPage extends ChangeEmailPage {

    static at = {
        title == 'Error: HMPPS Digital Services - Change Email'
    }

    static content = {
        errorText { $('#error-detail').text() }
    }
}
