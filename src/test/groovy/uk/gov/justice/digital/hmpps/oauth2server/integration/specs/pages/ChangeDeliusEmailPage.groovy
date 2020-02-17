package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class ChangeDeliusEmailPage extends Page {

  static url = '/auth/change-email'

  static at = {
    title == 'HMPPS Digital Services - Change Email'
    headingText == 'Delius user - update email address'
    deliusUserText == 'Please update your email address within Delius as you are unable to do it here'
  }

  static content = {
    headingText { $('#main-content h1').text() }
    deliusUserText { $('#delius-user-text').text() }
    continueButton { $('#continue') }
  }

  void continueToHomePage() {
    assert continueButton.text() == 'OK, continue'
    continueButton.click()
  }
}
