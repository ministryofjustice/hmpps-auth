package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class UserHomePage extends Page {

    static url = "/auth/ui"

    static at = {
        headingText == 'OAuth server administration dashboard'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        logoutLink { $("a", id: 'logout') }
    }

    void logout() {
        assert logoutLink.text() == 'Sign out'

        logoutLink.click()
    }
}
