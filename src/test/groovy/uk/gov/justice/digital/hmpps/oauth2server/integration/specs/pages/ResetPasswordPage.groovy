package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount

class ResetPasswordPage extends Page {

    static url = '/auth/reset-password'

    static at = {
        title == 'HMPPS Digital Services - Reset Password'
        headingText == 'Create a new password'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        resetPasswordButton { $("input", type: 'submit') }
        usernameInput { $('#usernameOrEmail') }
        errorDetail { $('#error-detail').text() }
        backLink { $("a", 'data-qa': "back-link") }
    }

    void resetPasswordAs(UserAccount account) {
        resetPasswordAs account.username
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
