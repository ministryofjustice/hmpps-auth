package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class VerifyEmailConfirmPage extends Page {

    static url = '/auth/verify-email-confirm'

    static at = {
        title == 'HMPPS Digital Services - Verify Email Confirmation'
        headingText == 'Your email address has been verified'
    }

    static content = {
        headingText { $('#main-content h1').text() }
    }
}
