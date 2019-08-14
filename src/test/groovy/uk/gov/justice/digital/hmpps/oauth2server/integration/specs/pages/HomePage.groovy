package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages


import geb.Page
import io.jsonwebtoken.impl.DefaultJwtParser

class HomePage extends Page {

    static url = "/auth"

    static at = {
        title == 'HMPPS Digital Services - Home'
        headingText == 'Select service'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        userInfo { $('#userinfo') }
        logoutLink { $("a", id: 'logout') }
        principalName { $('#principal-name').text() }
    }

    void logout() {
        assert logoutLink.text() == 'Sign out'

        logoutLink.click()
    }

    Object parseJwt() {
        def token = browser.driver.manage().getCookieNamed("jwtSession").value
        def (header, payload) = token.split('\\.')

        new DefaultJwtParser().parse("$header.$payload.").body
    }
}
