package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount

class ExistingPasswordPage extends Page {

  static url = '/auth/existing-password'

  static at = {
    title == 'HMPPS Digital Services - Change Password Request'
    headingText == 'What is your current password?'
  }

  static content = {
    headingText { $('#main-content h1').text() }
    continueButtion { $("input", type: 'submit') }
    usernameInput { $("input", name: 'username') }
  }

  void existingPasswordAs(UserAccount user, String existingPassword) {
    // ensure we have a non visible field containing username for password managers
    assert usernameInput.value() == user.username
    assert !usernameInput.isDisplayed()

    $('form').password = existingPassword
    continueButtion.click()
  }
}
