package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class ChangeEmailPage extends Page {

  static url = '/auth/change-email'

  static at = {
    title == 'HMPPS Digital Services - Change Email'
    headingText == 'What is your new email address?'
  }

  static content = {
    headingText { $('#main-content h1').text() }
    changeEmailButton { $("input", type: 'submit') }
    emailInput { $('#email') }
    errorText { $('#errors').text() }
  }

  void changeEmailAs(String newEmail, String existingEmail) {
    assert $('form').email == existingEmail
    emailInput = newEmail
    assert changeEmailButton.value() == 'Update'
    changeEmailButton.click()
  }
}
