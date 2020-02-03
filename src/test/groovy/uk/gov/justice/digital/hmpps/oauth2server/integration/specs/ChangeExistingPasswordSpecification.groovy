package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.*

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.*

class ChangeExistingPasswordSpecification extends DeliusIntegrationSpec {
  def "Change password no current password entered"() {
    given: 'I try to change my password'
    browser.go("/auth/existing-password")
    at LoginPage
    loginAs CA_USER, 'password123456'

    and: 'I am redirected to the existing password page'
    at ExistingPasswordPage

    when: "I change password without credentials"
    existingPasswordAs CA_USER, '   '

    then: 'My credentials are rejected and I am still on the Change Existing Password page'
    at ExistingPasswordErrorPage
    errorText == 'Enter your current password'
    errorFieldText == 'Enter your current password'
  }

  def "Change password wrong current locks account"() {
    given: 'I try to change my password'
    browser.go("/auth/existing-password")
    at LoginPage
    loginAs AUTH_CHANGE_TEST, 'password123456'

    and: 'I am redirected to the existing password page'
    at ExistingPasswordPage

    when: "I change password with incorrect credentials"
    existingPasswordAs AUTH_CHANGE_TEST, 'wrongpass'

    then: 'My credentials are rejected and I am still on the Change Existing Password page'
    at ExistingPasswordErrorPage
    errorText == 'Your password is incorrect. You will be locked out if you enter the wrong details 3 times.'
    errorFieldText == 'Your password is incorrect. You will be locked out if you enter the wrong details 3 times.'

    when: "I change password with incorrect credentials"
    existingPasswordAs AUTH_CHANGE_TEST, 'wrongpass'

    then: 'My credentials are rejected and I am still on the Change Existing Password page'
    at ExistingPasswordErrorPage
    errorText == 'Your password is incorrect. You will be locked out if you enter the wrong details 3 times.'
    errorFieldText == 'Your password is incorrect. You will be locked out if you enter the wrong details 3 times.'

    when: "I change password with incorrect credentials"
    existingPasswordAs AUTH_CHANGE_TEST, 'wrongpass'

    then: 'My credentials are rejected and I am taken to the Login page'
    at LoginErrorPage
    errorText == "Your account is locked. If you have verified your email address then you can use 'I have forgotten my password' below."

    when: "I then try to login"
    loginAs AUTH_CHANGE_TEST, 'password123456'

    then: 'My credentials are rejected and I am still on the Login page'
    at LoginErrorPage
    errorText == "Your account is locked. If you have verified your email address then you can use 'I have forgotten my password' below."
  }


  def "Change password flow"() {
    given: 'I try to change my password'
    to LoginPage
    loginAs AUTH_CHANGE2_TEST, 'password123456'

    and: 'I am redirected to the existing password page'
    to ExistingPasswordPage

    when: "I change password with correct credentials"
    existingPasswordAs AUTH_CHANGE2_TEST, 'password123456'

    then: 'My credentials are accepted and I am on the Change Password page'
    at ChangeExistingPasswordPage

    when: "I enter not matching passwords"
    changePasswordAs AUTH_CHANGE2_TEST, 'newpass123', 'differentpass'

    then:
    at ChangeExistingPasswordErrorPage
    errorText == 'Your passwords do not match. Enter matching passwords.'
    errorConfirmText == 'Your passwords do not match. Enter matching passwords.'

    when: "I enter correct passwords"
    changePasswordAs AUTH_CHANGE2_TEST, 'newpass123', 'newpass123'

    then: 'My password is changed'
    at HomePage

    when: "I logout and I login using valid credentials"
    logout()
    at LoginPage
    loginAs AUTH_CHANGE2_TEST, 'newpass123'

    then: 'My credentials are accepted and I am shown the Home page'
    at HomePage
    principalName == 'Auth Test'
  }

  def "Change password mfa user"() {
    given: 'I try to change my password'
    browser.go("/auth/existing-password")

    when: 'I login as an MFA user'
    at LoginPage
    loginAs AUTH_MFA_USER, 'password123456'

    then: 'I am redirected to the mfa page'
    at MfaPage

    when: "I enter my MFA credentials"
    submitCode mfaCode

    then: 'I am taken to the existing password page'
    at ExistingPasswordPage

    when: "I enter existing password"
    existingPasswordAs AUTH_MFA_USER, 'password123456'

    then: 'My credentials are accepted and I am taken to the enter new password page'
    at ChangeExistingPasswordPage
  }
}
