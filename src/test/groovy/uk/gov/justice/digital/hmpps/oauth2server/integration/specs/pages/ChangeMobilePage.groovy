package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class ChangeMobilePage extends Page {

  static url = '/auth/new-mobile'

  static at = {
    title == 'HMPPS Digital Services - Change Mobile Number'
    headingText == 'What is your new mobile phone number?'
  }

  static content = {
    headingText { $('#main-content h1').text() }
    changeMobileButton { $("input", type: 'submit') }
    mobileInput { $('#mobile') }
    errorText { $('#errors').text() }
  }

  void changeMobileAs(String mobile) {
    mobileInput = mobile
    assert changeMobileButton.value() == 'Update'
    changeMobileButton.click()
  }
}
