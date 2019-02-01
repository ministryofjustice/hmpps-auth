package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount

class ResetPasswordPage extends Page {

    static url = '/auth/reset-password'

    static at = {
        title == 'HMPPS Digital Services - Reset Password'
        headingText == 'Reset your password'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        resetPasswordButton { $("input", type: 'submit') }
        usernameInput { $('#username') }
        errorDetail { $('#error-detail').text() }
        backLink { $("a", id: "back-link") }
    }

    void resetPasswordAs(UserAccount account) {
        usernameInput = account.username
        assert resetPasswordButton.value() == 'Continue'
        resetPasswordButton.click()
    }

    void resetPasswordAs(String username) {
        usernameInput = username
        assert resetPasswordButton.value() == 'Continue'
        resetPasswordButton.click()
    }

    void back() {
        backLink.click()
    }
}
