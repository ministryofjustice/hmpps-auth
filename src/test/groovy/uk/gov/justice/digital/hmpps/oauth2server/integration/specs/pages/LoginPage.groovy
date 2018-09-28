package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount


class LoginPage extends Page {

    static url = '/auth/login'

    static at = {
        title == 'Nomis Authentication - Login'
        headingText == 'Login'
    }

    static content = {
        headingText { $('h1').text() }
        signInButton{ $("button", type: 'submit') }
    }

    void loginAs(UserAccount userAccount, String password) {

        $('form').username = userAccount.username
        $('form').password = password

        assert signInButton.text() == 'Sign in'

        signInButton.click()
    }
}
