package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class ResetPasswordSuccessPage extends Page {

    static url = '/auth/reset-password-success'

    static at = {
        title == 'HMPPS Digital Services - Reset Password Success'
        headingText == 'Reset password successful'
    }

    static content = {
        headingText { $('#main-content h1').text() }
    }
}
