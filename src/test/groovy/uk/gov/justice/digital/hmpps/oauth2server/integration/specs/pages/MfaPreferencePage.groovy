package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class MfaPreferencePage extends Page {

  static url = '/auth/mfa-preference'

  static at = {
    title == 'HMPPS Digital Services - Security code preference'
    headingText == 'How would you prefer to receive your security code?'
  }

  static content = {
    headingText { $('#main-content h1').text() }
    continueButton { $("input", type: 'submit') }
    errorText { $('#error-detail').text() }
    emailRadio { $('#mfa-pref-email') }
    textRadio { $('#mfa-pref-text') }
  }

  void selectTextMessage() {

    assert emailRadio.getAttribute("checked")

    textRadio.click()

    assert continueButton.value() == 'Save'

    continueButton.click()
  }

  void selectEmailMessage() {

    assert textRadio.getAttribute("checked")

    emailRadio.click()

    assert continueButton.value() == 'Save'

    continueButton.click()
  }


}
