package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class InitialPasswordSuccessPage extends Page {

    static url = '/auth/initial-password-success'

    static at = {
        title == 'HMPPS Digital Services - New Password Saved'
        headingText == 'New password saved'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        signInLink { $('#hdc-signin') }
    }
}
