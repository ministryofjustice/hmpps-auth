package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class MfaEmailPage extends Page {

  static url = '/auth/mfa-challenge'

  static at = {
    title == 'HMPPS Digital Services - Email verification'
    headingText == 'Check your email'
  }

  static content = {
    headingText { $('#main-content h1').text() }
    continueButton { $("input", type: 'submit') }
    resendCodeLink { $("a", id: 'resend-mfa') }
    errorText { $('#error-detail').text() }
    mfaCode { $('[data-qa="mfa-code"]').text() }
  }

  void submitCode(String code) {

    $('form').code = code

    assert continueButton.value() == 'Continue'

    continueButton.click()
  }

  void resendMfa() {
    assert resendCodeLink.text() == 'Not received an email?'

    resendCodeLink.click()
  }
}
