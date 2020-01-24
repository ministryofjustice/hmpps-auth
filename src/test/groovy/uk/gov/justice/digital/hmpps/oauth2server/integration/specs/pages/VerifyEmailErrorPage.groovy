package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

class VerifyEmailErrorPage extends VerifyEmailPage {

    static at = {
        title == 'Error: HMPPS Digital Services - Verify Email'
    }

    static content = {
        errorDetail { $('#error-detail').text() }
    }
}
