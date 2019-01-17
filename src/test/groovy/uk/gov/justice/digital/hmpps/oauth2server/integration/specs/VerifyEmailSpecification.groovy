package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.spock.GebReportingSpec
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.*

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.DM_USER
import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.RO_USER

class VerifyEmailSpecification extends GebReportingSpec {

    def "A user can cancel email verification"() {
        given: 'I login with a non verified email user'
        to LoginPage
        loginAs DM_USER, 'password123456'

        when: 'The Verify Email page is displayed'
        at VerifyEmailPage

        and: 'I cancel the verification process'
        cancel()

        then: 'The Home page is displayed'
        at HomePage
    }

    def "A user can verify a previously chosen email"() {
        given: 'I login with a non verified email user'
        to LoginPage
        loginAs DM_USER, 'password123456'

        when: 'The Verify Email page is displayed'
        at VerifyEmailPage
        verifyExistingEmailAs 'dm_user@digital.justice.gov.uk'

        and: 'The Verify Email sent page is displayed'
        at VerifyEmailSentPage
        String verifyLink = $('#verifyLink').@href
        continueProcess()

        and: 'The Home page is displayed'
        at HomePage

        and: 'I can then verify my email address'
        browser.go verifyLink

        then:
        at VerifyEmailConfirmPage
    }

    def "A user can verify an email that exists in pnomis"() {
        given: 'I login with a non verified email user'
        to LoginPage
        loginAs RO_USER, 'password123456'

        when: 'The Verify Email page is displayed'
        at VerifyEmailPage
        selectExistingEmailAs 'phillips@bobjustice.gov.uk'

        and: 'The Verify Email sent page is displayed'
        at VerifyEmailSentPage
        String verifyLink = $('#verifyLink').@href
        continueProcess()

        and: 'The Home page is displayed'
        at HomePage

        and: 'I can then verify my email address'
        browser.go verifyLink

        then:
        at VerifyEmailConfirmPage
    }

    def "A user is asked to sign in again if the verification link is invalid"() {
        given: 'I have a verify link'
        String verifyLink = "/auth/verify-email-confirm/someinvalidtoken"

        when: 'I browse to the link'
        browser.go verifyLink

        then:
        at VerifyEmailErrorPage
        errorDetail.startsWith('This link is invalid')
    }
}
