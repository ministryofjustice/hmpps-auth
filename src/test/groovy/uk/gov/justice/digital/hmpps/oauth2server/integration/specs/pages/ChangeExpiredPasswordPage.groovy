package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount

class ChangeExpiredPasswordPage extends Page {

  static url = '/auth/change-password'

  static at = {
    title == 'HMPPS Digital Services - Change Password'
    headingText == 'Your password has expired'
  }

  static content = {
    headingText { $('#main-content h1').text() }
    changePasswordButton { $("input", type: 'submit') }
    usernameInput { $("input", name: 'username') }
  }

  void changePasswordAs(UserAccount user, String newPassword, String confirmNewPassword) {
    // ensure we have a non visible field containing username for password managers
    assert usernameInput.value() == user.username
    assert !usernameInput.isDisplayed()

    $('form').newPassword = newPassword
    $('form').confirmPassword = confirmNewPassword

    assert changePasswordButton.value() == 'Save password'

    changePasswordButton.click()
  }
}
