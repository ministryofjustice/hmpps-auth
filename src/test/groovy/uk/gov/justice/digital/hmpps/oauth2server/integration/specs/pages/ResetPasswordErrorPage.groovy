package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

class ResetPasswordErrorPage extends ResetPasswordPage {

    static at = {
        title == 'Error: HMPPS Digital Services - Reset Password'
    }

    static content = {
        errorDetail { $('#error-detail').text() }
        errorText { $('#error').text() }
    }
}
