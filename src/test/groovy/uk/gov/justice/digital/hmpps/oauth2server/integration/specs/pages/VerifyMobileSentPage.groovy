package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class VerifyMobileSentPage extends Page {

  static url = '/auth/verify-mobile'

  static at = {
    title == 'HMPPS Digital Services - Verify Mobile Code Sent'
    headingText == 'Check your phone'
  }

  static content = {
    headingText { $('#main-content h1').text() }
    continueButton { $("input", type: 'submit') }
    resendCodeLink { $("a", id: 'resend-mobile-code') }
    errorText { $('#error-detail').text() }
    verifyCode { $('[data-qa="verifyCode"]').text() }
  }

  void submitCode(String verifyCode) {

    $('form').code = verifyCode

    assert continueButton.value() == 'Continue'

    continueButton.click()
  }

  void resendMobileCode() {
    assert resendCodeLink.text() == 'Not received an text message?'

    resendCodeLink.click()
  }
}
