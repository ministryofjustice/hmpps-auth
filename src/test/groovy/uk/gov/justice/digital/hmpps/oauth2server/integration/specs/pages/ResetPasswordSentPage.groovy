package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class ResetPasswordSentPage extends Page {

    static url = '/auth/reset-password'

    static at = {
        title == 'HMPPS Digital Services - Reset Password Email Sent'
        headingText == 'Check your email'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        backLink { $("a", id: "back-link") }
    }

    void back() {
        backLink.click()
    }
}
