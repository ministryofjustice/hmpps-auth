package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.*

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.*

class ChangeEmailSpecification extends DeliusIntegrationSpec {

  def "Change email flow"() {
    given: 'I try to change my Email address'
    to LoginPage
    loginAs AUTH_CHANGE_EMAIL, 'password123456'

    and: 'I am redirected to the existing password page'
    to ExistingPasswordChangeEmailPage

    when: "I enter password with correct credentials"
    existingPasswordAs AUTH_CHANGE_EMAIL, 'password123456'

    then: 'My credentials are accepted and I am on the Change Email page'
    at ChangeEmailPage

    when: "I enter new email"
    changeEmailAs 'dm_user@digital.justice.gov.uk', 'auth_test@digital.justice.gov.uk'

    and: 'The Verify Email sent page is displayed'
    at VerifyEmailSentPage
    String verifyLink = $('#verifyLink').@href
    continueProcess()

    and: 'The Home page is displayed'
    at HomePage

    and: 'I can then verify my email address'
    browser.go verifyLink

    then: 'I am shown the success page'
    at VerifyEmailConfirmPage

    and: 'I can then verify my email address again'
    browser.go verifyLink

    then: 'I am shown the success page'
    at VerifyEmailConfirmPage
  }

  def "Change email flow incorrect password"() {
    given: 'I try to change my Email address'
    to LoginPage
    loginAs AUTH_CHANGE_EMAIL2, 'password123456'

    and: 'I am redirected to the existing password page'
    to ExistingPasswordChangeEmailPage

    when: "I enter password with incorrect credentials"
    existingPasswordAs AUTH_CHANGE_EMAIL2, 'password1234567'

    then: 'My credentials are rejected and I am still on the Change Existing Password page'
    at ExistingPasswordChangeEmailErrorPage
    errorText == 'Your password is incorrect. You will be locked out if you enter the wrong details 3 times.'
    errorFieldText == 'Your password is incorrect. You will be locked out if you enter the wrong details 3 times.'

    when: "I enter password with incorrect credentials"
    existingPasswordAs AUTH_CHANGE_EMAIL2, 'password123456'

    then: 'My credentials are accepted and I am on the Change Email page'
    at ChangeEmailPage

    when: "I enter new email"
    changeEmailAs 'dm_user1@digital.justice.gov.uk', 'auth_email@digital.justice.gov.uk'

    and: 'The Verify Email sent page is displayed'
    at VerifyEmailSentPage
    String verifyLink = $('#verifyLink').@href
    continueProcess()

    and: 'The Home page is displayed'
    at HomePage

    and: 'I can then verify my email address'
    browser.go verifyLink

    then: 'I am shown the success page'
    at VerifyEmailConfirmPage

    and: 'I can then verify my email address again'
    browser.go verifyLink

    then: 'I am shown the success page'
    at VerifyEmailConfirmPage
  }

  def "A user is not allowed to change email address to a gsi email address"() {
    given: 'I try to change my Email address'
    to LoginPage
    loginAs AUTH_CHANGE_EMAIL_GSI, 'password123456'

    and: 'I am redirected to the existing password page'
    to ExistingPasswordChangeEmailPage

    when: "I enter password with correct credentials"
    existingPasswordAs AUTH_CHANGE_EMAIL_GSI, 'password123456'

    then: 'My credentials are accepted and I am on the Change Email page'
    at ChangeEmailPage

    when: "I enter new email"
    changeEmailAs 'dm_user@hmps.gsi.gov.uk', 'auth_email@digital.justice.gov.uk'

    then:
    at ChangeEmailErrorPage
    errorText.startsWith('All gsi.gov.uk have now been migrated to a justice.gov.uk domain. Enter your justice.gov.uk address instead.')
  }

  def "A user is not allowed to change email address to an invalid email address"() {
    given: 'I try to change my Email address'
    to LoginPage
    loginAs AUTH_CHANGE_EMAIL_INVALID, 'password123456'

    and: 'I am redirected to the existing password page'
    to ExistingPasswordChangeEmailPage

    when: "I enter password with correct credentials"
    existingPasswordAs AUTH_CHANGE_EMAIL_INVALID, 'password123456'

    then: 'My credentials are accepted and I am on the Change Email page'
    at ChangeEmailPage

    when: "I enter new email"
    changeEmailAs 'dm_user@digital.justice', 'auth_email@digital.justice.gov.uk'

    then:
    at ChangeEmailErrorPage
    errorText.startsWith('Enter your work email address')
  }

  def "Change email flow with failing email"() {
    given: 'I try to change my Email address'
    to LoginPage
    loginAs AUTH_CHANGE_EMAIL_INCOMPLETE, 'password123456'

    and: 'I am redirected to the existing password page'
    to ExistingPasswordChangeEmailPage

    when: "I enter password with correct credentials"
    existingPasswordAs AUTH_CHANGE_EMAIL_INCOMPLETE, 'password123456'

    then: 'My credentials are accepted and I am on the Change Email page'
    at ChangeEmailPage

    when: "I enter incomplete new email"
    changeEmailAs 'dm_user2@digital.justice', 'auth_email@digital.justice.gov.uk'

    then:
    at ChangeEmailErrorPage
    errorText == 'Enter your work email address'

    when: "I enter new email and the incomplete email is retained"
    changeEmailAs 'dm_user2@digital.justice.gov.uk', 'dm_user2@digital.justice'

    and: 'The Verify Email sent page is displayed'
    at VerifyEmailSentPage
    String verifyLink = $('#verifyLink').@href
    continueProcess()

    and: 'The Home page is displayed'
    at HomePage

    and: 'I can then verify my email address'
    browser.go verifyLink

    then: 'I am shown the success page'
    at VerifyEmailConfirmPage

    and: 'I can then verify my email address again'
    browser.go verifyLink

    then: 'I am shown the success page'
    at VerifyEmailConfirmPage
  }

  def "Delius user change email address flow"() {
    given: 'I try to change my Email address'
    to LoginPage
    loginAs DELIUS_USER_EMAIL, 'password'

    and: 'I am redirected to the existing password page'
    to ExistingPasswordChangeEmailPage

    when: "I enter password with correct credentials"
    existingPasswordAs DELIUS_USER_EMAIL, 'password'

    and: 'My credentials are accepted and I am on the Delius Change Email page'
    at ChangeDeliusEmailPage
    deliusUserText == 'Please update your email address within Delius as you are unable to do it here'
    continueToAccountDetailsPage()

    then: 'The Account Details page is displayed'
    at AccountDetailsPage
    principalName == 'Delius Smith'
  }

  def "Change email mfa user"() {
    given: 'I try to change my email'
    browser.go("/auth/existing-email")

    when: 'I login as an MFA user'
    at LoginPage
    loginAs AUTH_MFA_CHANGE_EMAIL, 'password123456'

    then: 'I am redirected to the mfa page'
    at MfaEmailPage

    when: "I enter my MFA credentials"
    submitCode mfaCode

    and: 'I am redirected to the existing password page'
    to ExistingPasswordChangeEmailPage

    and: "I enter password with correct credentials"
    existingPasswordAs AUTH_MFA_CHANGE_EMAIL, 'password123456'

    then: 'My credentials are accepted and I am on the Change Email page'
    at ChangeEmailPage

    when: "I enter new email"
    changeEmailAs 'dm_user@digital.justice.gov.uk', 'auth_email@digital.justice.gov.uk'

    and: 'The Verify Email sent page is displayed'
    at VerifyEmailSentPage
    String verifyLink = $('#verifyLink').@href
    continueProcess()

    and: 'The Home page is displayed'
    at HomePage

    and: 'I can then verify my email address'
    browser.go verifyLink

    then: 'I am shown the success page'
    at VerifyEmailConfirmPage

    and: 'I can then verify my email address again'
    browser.go verifyLink

    then: 'I am shown the success page'
    at VerifyEmailConfirmPage
  }

  def "Change email flow current verified email re-entered"() {
    given: 'I try to change my Email address'
    to LoginPage
    loginAs AUTH_CHANGE_EMAIL_VERIFIED, 'password123456'

    and: 'I am redirected to the existing password page'
    to ExistingPasswordChangeEmailPage

    when: "I enter password with correct credentials"
    existingPasswordAs AUTH_CHANGE_EMAIL_VERIFIED, 'password123456'

    then: 'My credentials are accepted and I am on the Change Email page'
    at ChangeEmailPage

    when: "I enter new email"
    changeEmailAs 'auth_email@digital.justice.gov.uk', 'auth_email@digital.justice.gov.uk'

    and: 'I am redirected to the Mobile already Verified page'
    at VerifyEmailAlreadyPage
    continueProcess()

    then: 'Your account details page is displayed'
    at AccountDetailsPage
  }
}
