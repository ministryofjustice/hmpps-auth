package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount


class LoginPage extends Page {

    static url = '/auth/login'

    static at = {
        title == 'Nomis Authentication - Sign in'
        headingText == 'Sign in'
    }

    static content = {
        headingText { $('#content h1').text() }
        signInButton { $("button", type: 'submit') }
        termsLink { $("a", id: 'terms') }
        errorText { $('#content h2').text() }
    }

    void loginAs(UserAccount userAccount, String password) {

        $('form').username = userAccount.username
        $('form').password = password

        assert signInButton.text() == 'Sign in'

        signInButton.click()
    }

    void viewTerms() {
        assert termsLink.text() == 'Terms and conditions'

        termsLink.click()
    }
}
