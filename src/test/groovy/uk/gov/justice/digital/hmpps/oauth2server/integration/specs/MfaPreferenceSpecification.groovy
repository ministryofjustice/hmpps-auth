package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.spock.GebReportingSpec
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.*

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.AUTH_MFA_PREF_EMAIL
import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.AUTH_MFA_PREF_TEXT

class MfaPreferenceSpecification extends GebReportingSpec {

  def "mfa Preference flow select text"() {
    given: 'I try to change my MFA preference'
    to LoginPage

    when: 'I login'
    loginAs AUTH_MFA_PREF_EMAIL, 'password123456'

    then: 'I am redirected to the mfa page'
    at MfaPage

    when: "I enter my MFA credentials"
    submitCode mfaCode

    and: 'My credentials are accepted and I go on to the mfa preference page'
    to MfaPreferencePage

    and: "I select text message"
    selectTextMessage()

    and: 'I am shown the success page'
    at MfaPreferenceConfirmPage
    continueToHomePage()

    then: 'The home page is displayed'
    at HomePage
  }

  def "mfa Preference flow select email"() {
    given: 'I try to change my MFA preference'
    to LoginPage

    when: 'I login'
    loginAs AUTH_MFA_PREF_TEXT, 'password123456'

    then: 'I am redirected to the mfa page'
    at MfaPage

    when: "I enter my MFA credentials"
    submitCode mfaCode

    and: 'My credentials are accepted and I go on to the mfa preference page'
    to MfaPreferencePage

    and: "I select email"
    selectEmailMessage()

    and: 'I am shown the success page'
    at MfaPreferenceConfirmPage
    continueToHomePage()

    then: 'The home page is displayed'
    at HomePage
  }
}
