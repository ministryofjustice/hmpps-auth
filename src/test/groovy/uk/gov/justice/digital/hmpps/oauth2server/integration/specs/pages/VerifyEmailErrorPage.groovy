package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class VerifyEmailErrorPage extends Page {

    static url = '/auth/verify-email-confirm'

    static at = {
        title == 'Error: HMPPS Digital Services - Verify Email Confirmation'
    }

    static content = {
        errorDetail { $('#error-detail').text() }
    }
}
