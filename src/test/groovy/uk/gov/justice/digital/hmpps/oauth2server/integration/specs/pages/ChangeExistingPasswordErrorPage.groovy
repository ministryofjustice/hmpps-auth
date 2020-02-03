package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

class ChangeExistingPasswordErrorPage extends ChangeExpiredPasswordPage {

  static at = {
    title == 'Error: HMPPS Digital Services - Change Password'
    headingText == 'Enter new password'
  }

  static content = {
    errorText { $('#error-detail').text() }
    errorNewText { $('#errornew').text() }
    errorConfirmText { $('#errorconfirm').text() }
  }
}
