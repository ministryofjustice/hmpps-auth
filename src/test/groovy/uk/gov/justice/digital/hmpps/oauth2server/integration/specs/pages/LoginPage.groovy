package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount


class LoginPage extends Page {

    static url = '/auth/login'

    static at = {
        title == 'HMPPS Digital Services - Sign in'
        headingText == 'Sign in'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        signInButton { $("input", type: 'submit') }
        termsLink { $("a", id: 'terms') }
        resetPasswordLink { $("a", id: 'forgotten-password') }
        errorText { $('#error-detail').text() }
        warning { $('#warning').text() }
    }

    void loginAs(UserAccount userAccount, String password) {
        loginAs userAccount.username, password
    }

    void loginAs(String username, String password) {

        $('form').username = username
        $('form').password = password

        assert signInButton.value() == 'Sign in'

        signInButton.click()
    }

    void viewTerms() {
        assert termsLink.text() == 'Terms and conditions'

        termsLink.click()
    }

    void resetPassword() {
        assert resetPasswordLink.text() == 'I have forgotten my password'

        resetPasswordLink.click()
    }
}
