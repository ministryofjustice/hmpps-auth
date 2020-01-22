package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class UserDetailsPage extends Page {

  static url = '/auth/user-details'

  static at = {
    title == 'HMPPS Digital Services - Update Personal Details'
    headingText == 'Update personal details'
  }

  static content = {
    headingText { $('#main-content h1').text() }
    saveButton { $("input", type: 'submit') }
    firstNameInput { $('form').firstName }
    lastNameInput { $('form').lastName }
    cancelButton { $("a", id: 'cancel') }
  }

  void userDetails(String firstName, String lastName) {
    $('form').firstName = firstName
    $('form').lastName = lastName

    assert saveButton.value() == 'Save'

    saveButton.click()
  }

  void cancel() {
    assert cancelButton.text() == 'Cancel'
    cancelButton.click()
  }
}
