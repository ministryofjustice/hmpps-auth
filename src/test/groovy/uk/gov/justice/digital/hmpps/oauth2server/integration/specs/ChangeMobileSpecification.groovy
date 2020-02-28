package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.*

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.AUTH_CHANGE_MOBILE

class ChangeMobileSpecification extends DeliusIntegrationSpec {

  def "Change mobile flow"() {
    given: 'I try to change my mobile mumber'
    to LoginPage
    loginAs AUTH_CHANGE_MOBILE, 'password123456'

    and: 'My credentials are accepted and I go on to the change mobile page'
    to ChangeMobilePage

    when: "I enter new mobile number"
    changeMobileAs '07987654321'

    and: 'The verify mobile sent page is displayed'
    at VerifyMobileSentPage
    submitCode verifyCode

    and: 'I am shown the success page'
    at VerifyMobileConfirmPage
    continueToHomePage()

    then: 'The home page is displayed'
    at HomePage
  }

  def 'Change mobile invalid number entered'() {
    given: 'I try to change my mobile number'
    to LoginPage
    loginAs AUTH_CHANGE_MOBILE, 'password123456'

    and: 'My credentials are accepted and I go on to the change mobile page'
    to ChangeMobilePage

    when: "I enter new mobile number"
    changeMobileAs '07'

    then: "I am shown an error message"
    at ChangeMobileErrorPage
    errorText == 'Enter a mobile phone number in the correct format'
  }

  def 'Change mobile flow invalid verification code'() {
    given: 'I try to change my mobile number'
    to LoginPage
    loginAs AUTH_CHANGE_MOBILE, 'password123456'

    and: 'My credentials are accepted and I am on the Change Mobile page'
    to ChangeMobilePage

    when: "I enter new mobile number"
    changeMobileAs '07987654321'

    and: 'The Verify Mobile sent page is displayed'
    at VerifyMobileSentPage
    submitCode '123456'

    then: "I am shown an error message"
    at VerifyMobileErrorPage
    errorText == 'Enter the code received in the text message'
  }

}
