package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class MfaPreferenceConfirmPage extends Page {

  static url = '/auth/mfa-preference-confirm'

  static at = {
    title == 'HMPPS Digital Services - Security code Preference Confirmation'
    headingText == 'Security code preference updated'
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
