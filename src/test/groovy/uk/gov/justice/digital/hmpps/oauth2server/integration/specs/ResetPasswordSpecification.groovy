package uk.gov.justice.digital.hmpps.oauth2server.integration.specs


import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.*

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.*

class ResetPasswordSpecification extends DeliusIntegrationSpec {

    def "A user can cancel reset password"() {
        given: 'I would like to reset my password'
        to LoginPage
        resetPassword()

        when: 'The Reset Password page is displayed'
        at ResetPasswordPage

        and: 'I cancel the reset password process'
        back()

        then: 'The Login page is displayed'
        at LoginPage
    }

    def "A user must enter a valid email address"() {
        given: 'I would like to reset my password'
        to ResetPasswordPage

        when:
        resetPasswordAs "joe@bloggs.com"

        then: 'My email address is rejected and I am still on the Reset Password page'
        at ResetPasswordErrorPage
        errorDetail == 'Enter your work email address'

        and: 'My email address is shown for me to correct'
        usernameInput == 'joe@bloggs.com'
    }

    def "A user can enter their gsi email address to reset their password"() {
        given: 'I would like to reset my password'
        to LoginPage
        resetPassword()

        when: 'The Reset Password page is displayed'
        at ResetPasswordPage
        resetPasswordAs 'reset_test@hmps.gsi.gov.uk'

        then: 'The Reset Password sent page is displayed'
        at ResetPasswordSentPage
    }

    def "A user can enter an email address to reset their password"() {
        given: 'I would like to reset my password'
        to LoginPage
        resetPassword()

        when: 'The Reset Password page is displayed'
        at ResetPasswordPage
        resetPasswordAs RESET_TEST_USER

        then: 'The Reset Password sent page is displayed'
        at ResetPasswordSentPage
        $('#resetLink').@href != null
    }

    def "A user can reset their password by email address"() {
        given: 'I would like to reset my password'
        to LoginPage
        resetPassword()

        when: 'The Reset Password page is displayed'
        at ResetPasswordPage
        resetPasswordAs 'reset_test@digital.justice.gov.uk'

        and: 'The Reset Password sent page is displayed'
        at ResetPasswordSentPage
        String resetLink = $('#resetLink').@href
        back()

        and: 'The Login page is displayed'
        at LoginPage

        and: 'I can then click the reset link in the email'
        browser.go resetLink

        then: 'I am shown the reset password select page'
        at SetPasswordSelectPage

        when: 'I enter my username'
        enterUsernameAs EXPIRED_TEST2_USER

        then: 'I am shown the set password select error page'
        at SetPasswordSelectErrorPage
        errorText == 'The username entered is not linked to your email address'

        when: 'I enter my username'
        enterUsernameAs CA_USER_TEST

        then: 'I am shown the reset password page'
        at SetPasswordPage

        when: "I change password using valid credentials"
        setPasswordAs 'helloworld2', 'helloworld2'

        and: 'My credentials are accepted and I am shown the confirmation page'
        at ResetPasswordSuccessPage

        and: 'I can try to login using new password'
        to LoginPage
        loginAs CA_USER_TEST, 'helloworld2'

        then: 'I am logged in with new password'
        at HomePage
    }

    def "A user can reset their password"() {
        given: 'I would like to reset my password'
        to LoginPage
        resetPassword()

        when: 'The Reset Password page is displayed'
        at ResetPasswordPage
        resetPasswordAs RESET_TEST_USER

        and: 'The Reset Password sent page is displayed'
        at ResetPasswordSentPage
        String resetLink = $('#resetLink').@href
        back()

        and: 'The Login page is displayed'
        at LoginPage

        and: 'I can then click the reset link in the email'
        browser.go resetLink

        then: 'I am shown the set password page'
        at SetPasswordPage

        when: "I change password using valid credentials"
        setPasswordAs 'helloworld2', 'helloworld2'

        and: 'My credentials are accepted and I am shown the confirmation page'
        at ResetPasswordSuccessPage

        and: 'I can try to login using new password'
        to LoginPage
        loginAs RESET_TEST_USER, 'helloworld2'

        then: 'I am logged in with new password'
        at HomePage
    }

    def "An auth user can reset their password"() {
        given: 'I would like to reset my password'
        to LoginPage
        resetPassword()

        when: 'The Reset Password page is displayed'
        at ResetPasswordPage
        resetPasswordAs AUTH_LOCKED2

        and: 'The Reset Password sent page is displayed'
        at ResetPasswordSentPage
        String resetLink = $('#resetLink').@href
        back()

        and: 'The Login page is displayed'
        at LoginPage

        and: 'I can then click the reset link in the email'
        browser.go resetLink

        then: 'I am shown the set password page'
        at SetPasswordPage

        when: "I change password using valid credentials"
        setPasswordAs 'helloworld2', 'helloworld2'

        and: 'My credentials are accepted and I am shown the confirmation page'
        at ResetPasswordSuccessPage

        and: 'I can try to login using new password'
        to LoginPage
        loginAs AUTH_LOCKED2, 'helloworld2'

        then: 'I am logged in with new password'
        at HomePage
    }

    def "A DELIUS user can reset their password"() {
        given: 'I would like to reset my password'
        to LoginPage
        resetPassword()

        when: 'The Reset Password page is displayed'
        at ResetPasswordPage
        resetPasswordAs DELIUS_PASSWORD_RESET

        and: 'The Reset Password sent page is displayed'
        at ResetPasswordSentPage
        String resetLink = $('#resetLink').@href
        back()

        and: 'The Login page is displayed'
        at LoginPage

        and: 'I can then click the reset link in the email'
        browser.go resetLink

        then: 'I am shown the set password page'
        at SetPasswordPage

        when: "I change password using valid credentials"
        setPasswordAs 'helloworld2', 'helloworld2'

        and: 'My credentials are accepted and I am shown the confirmation page'
        at ResetPasswordSuccessPage

        and: 'I can try to login using new password'
        to LoginPage
        loginAs DELIUS_PASSWORD_RESET, 'helloworld2'

        then: 'I am logged in with new password'
        at HomePage
    }

    def "A user can reset their password back with lowercase username"() {
        given: 'I would like to reset my password'
        to ResetPasswordPage
        resetPasswordAs "reset_test_user"

        and: 'The Reset Password sent page is displayed'
        at ResetPasswordSentPage
        String resetLink = $('#resetLink').@href

        and: 'I can then click the reset link in the email'
        browser.go resetLink

        when: "I change password using valid credentials"
        at SetPasswordPage
        setPasswordAs 'reset123456', 'reset123456'

        then: 'My credentials are accepted and I am shown the confirmation page'
        at ResetPasswordSuccessPage
    }

    def "Attempt reset password without credentials"() {
        given: 'I am on the Set Password page'
        to ResetPasswordPage
        resetPasswordAs RESET_TEST_USER

        and: 'The Reset Password sent page is displayed'
        at ResetPasswordSentPage
        String resetLink = $('#resetLink').@href

        and: 'I can then click the reset link in the email'
        browser.go resetLink

        when: "I change password without credentials"
        at SetPasswordPage
        setPasswordAs '', ''

        then: 'My credentials are rejected and I am still on the Reset Password page'
        at SetPasswordErrorPage
        errorText == 'Enter your new password\nEnter your new password again'
        errorNewText == 'Enter your new password'
        errorConfirmText == 'Enter your new password again'

        when: "I change password without credentials"
        at SetPasswordErrorPage
        setPasswordAs 'somepass', 'd'

        then: 'My credentials are rejected and I am still on the Set Password page'
        at SetPasswordErrorPage
        errorText == 'Your password must have both letters and numbers\n' +
                'Your password must have at least 9 characters\n' +
                'Your passwords do not match. Enter matching passwords.'
        errorNewText == 'Your password must have both letters and numbers\n' +
                'Your password must have at least 9 characters'
        errorConfirmText == 'Your passwords do not match. Enter matching passwords.'
    }

    def "A user is asked to reset password again if the reset link is invalid"() {
        given: 'I have a reset link'
        String resetLink = "/auth/reset-password-confirm?token=someinvalidtoken"

        when: 'I browse to the link'
        browser.go resetLink

        then:
        at ResetPasswordErrorPage
        errorDetail.startsWith('This link is invalid')
    }
}
