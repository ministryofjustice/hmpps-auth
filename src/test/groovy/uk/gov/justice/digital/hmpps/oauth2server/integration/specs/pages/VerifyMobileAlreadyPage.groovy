package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class VerifyMobileAlreadyPage extends Page {

  static url = '/auth/verify-email'

  static at = {
    title == 'HMPPS Digital Services - Verify Email Confirmation'
    headingText == 'Mobile already verified'
  }

  static content = {
    headingText { $('#main-content h1').text() }
    continueButton { $("a", id: 'continue') }
  }

  void continueProcess() {
    assert continueButton.text() == 'OK, continue'
    continueButton.click()
  }
}
