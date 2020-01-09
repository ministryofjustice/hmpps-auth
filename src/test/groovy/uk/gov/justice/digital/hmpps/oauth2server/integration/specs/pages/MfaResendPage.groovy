package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class MfaResendPage extends Page {

  static url = '/auth/mfa-resend'

  static at = {
    title == 'HMPPS Digital Services - Resend verification code'
    headingText == 'Resend security code'
  }

  static content = {
    headingText { $('#main-content h1').text() }
    continueButton { $("input", type: 'submit') }
  }

  void resendCode() {

    assert continueButton.value() == 'Resend security code'

    continueButton.click()
  }
}
