package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

class VerifyEmailConfirmErrorPage extends VerifyEmailConfirmPage {

  static at = {
    title == 'Error: HMPPS Digital Services - Verify Email Confirmation'
  }

  static content = {
    errorDetail { $('#error-detail').text() }
  }
}
