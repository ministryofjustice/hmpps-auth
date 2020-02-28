package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class VerifyMobileConfirmPage extends Page {

  static url = '/auth/verify-mobile-confirm'

  static at = {
    title == 'HMPPS Digital Services - Verify Mobile Confirmation'
    headingText == 'Mobile number verified'
  }

  static content = {
    headingText { $('#main-content h1').text() }
    continueButton { $('#continue') }
  }

  void continueToHomePage() {
    assert continueButton.text() == 'OK, continue'
    continueButton.click()
  }
}
