package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.spock.GebReportingSpec
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.*

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.*

class VerifyMobileSpecification extends GebReportingSpec {

  def "Change mobile flow resend verification code"() {
    given: 'I try to change my Mobile Number'
    to LoginPage
    loginAs AUTH_CHANGE_MOBILE_ADD, 'password123456'

    and: 'My credentials are accepted and I am on the Change Mobile page'
    to ChangeMobilePage

    when: "I enter new mobile number"
    changeMobileAs '07700900321'

    and: 'The Verify Mobile sent page is displayed'
    at VerifyMobileSentPage
    resendMobileCode()

    and: 'The Resend verification code page is displayed'
    at MobileVerificationResendPage
    resendCode()

    and: 'The Verify Mobile sent page is displayed'
    at VerifyMobileSentPage
    submitCode verifyCode

    and: 'I am shown the success page'
    at VerifyMobileConfirmPage
    continueToAccountDetailsPage()

    then: 'The Account details page is displayed'
    at AccountDetailsPage
  }

  def "Change mobile flow invalid verification code"() {
    given: 'I try to change my Mobile Number'
    to LoginPage
    loginAs AUTH_CHANGE_MOBILE_UPDATE, 'password123456'

    and: 'My credentials are accepted and I am on the Change Mobile page'
    to ChangeMobilePage

    when: "I enter new mobile number"
    changeExistingMobileAs '07700900322', '07700900321'

    and: 'The Verify Mobile sent page is displayed'
    at VerifyMobileSentPage
    submitCode '123456'

    then: "I am shown an error message"
    at VerifyMobileErrorPage
    errorText == 'Enter the code received in the text message'
  }

  def "Resend code page without phone number is redirected to enter new phone number page"() {
    given: 'I try to change my Mobile Number'
    to LoginPage
    loginAs AUTH_CHANGE_MOBILE2, 'password123456'

    when: 'My credentials are accepted and I go direct to the Resend verification code page'
    to MobileVerificationResendPage
    resendCode()

    then: 'I am redirected to the enter new mobile number page'
    at ChangeMobileErrorPage
    errorText == 'No phone number found'
  }

  def "Resend code page with verified phone number is redirected to phone number already verified"() {
    given: 'I try to change my Mobile Number'
    to LoginPage
    loginAs AUTH_CHANGE_MOBILE_VERIFIED, 'password123456'

    when: 'My credentials are accepted and I go direct to the Resend verification code page'
    browser.go "/auth/mobile-resend"

    and: 'I am redirected to the Mobile already Verified page'
    at VerifyMobileAlreadyPage
    continueProcess()

    then: 'The Account details page is displayed'
    at AccountDetailsPage
  }
}
