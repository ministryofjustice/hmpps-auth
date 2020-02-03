package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

class ExistingPasswordErrorPage extends ExistingPasswordPage {

  static at = {
    title == 'Error: HMPPS Digital Services - Change Password Request'
    headingText == 'Change your password'
  }

  static content = {
    errorText { $('#error-detail').text() }
    errorFieldText { $('[data-qa="password-error"]').text() }
  }
}
