package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class ChangeMobilePage extends Page {

  static url = '/auth/change-mobile'

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

  void changeExistingMobileAs(String mobile, String existingMobile) {
    $('form').mobile = existingMobile
    assert changeMobileButton.value() != 'Add'
    assert changeMobileButton.value() == 'Update'

    mobileInput = mobile
    changeMobileButton.click()
  }

  void changeMobileAs(String mobile) {
    assert changeMobileButton.value() == 'Add'
    assert changeMobileButton.value() != 'Update'

    mobileInput = mobile
    changeMobileButton.click()
  }
}
