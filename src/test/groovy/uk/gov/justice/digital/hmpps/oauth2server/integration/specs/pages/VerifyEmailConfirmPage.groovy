package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class VerifyEmailConfirmPage extends Page {

    static url = '/auth/verify-email-confirm.'

    static at = {
        title == 'Nomis Authentication - Verify Email Confirmation'
        headingText == 'Verification email success'
    }

    static content = {
        headingText { $('#main-content h1').text() }
    }
}
