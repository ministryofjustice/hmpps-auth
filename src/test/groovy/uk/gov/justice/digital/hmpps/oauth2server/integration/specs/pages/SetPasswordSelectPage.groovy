package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount

class SetPasswordSelectPage extends Page {

    static url = '/auth/reset-password-select'

    static at = {
        title == 'HMPPS Digital Services - Set Password Select'
        headingText == 'Enter your username'
    }

    static content = {
        headingText { $('#main-content h1').text() }
        continueButton { $("input", type: 'submit') }
    }

    void enterUsernameAs(UserAccount userAccount) {
        $('form').username = userAccount.username

        assert continueButton.value() == 'Continue'

        continueButton.click()
    }
}
