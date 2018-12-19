package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class VerifyEmailConfirmPage extends Page {

    static url = '/auth/verify-email-confirm.'

    static at = {
        title == 'Nomis Authentication - Verify Email Confirmation'
        headingText == 'Email Address Verification Success'
    }

    static content = {
        headingText { $('#content h1').text() }
    }
}
