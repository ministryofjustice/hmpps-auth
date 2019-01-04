package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class HomePage extends Page {

    static url = "/auth"

    static at = {
        headingText == 'Auth server'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        userInfo { $('#userinfo') }
        logoutLink { $("a", id: 'logout') }
    }

    void logout() {
        assert logoutLink.text() == 'Sign out'

        logoutLink.click()
    }
}
